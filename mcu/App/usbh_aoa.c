/********************************** (C) COPYRIGHT *******************************
 * File Name          : usbh_aoa.c
 * Description        : USB host + AOA handshake + FocusPod bulk protocol.
 *********************************************************************************/
#include "ch32x035.h"
#include <ch32x035_usb.h>
#include <ch32x035_flash.h>
#include "ch32x035_usbfs_host.h"
#include "usb_host_config.h"
#include "usbh_aoa.h"
#include "board.h"
#include "cc_power.h"
#include "time.h"
#include "delay.h"

/******************************************************************************/
/* AOA definitions */

#define AOA_ACCESSORY_GET_PROTOCOL 51
#define AOA_ACCESSORY_SEND_STRING  52
#define AOA_ACCESSORY_START        53

#define AOA_VID_GOOGLE   0x18D1
#define AOA_PID_ACCESSORY      0x2D00
#define AOA_PID_ACCESSORY_ADB  0x2D01

#define AOA_STRING_MANUFACTURER 0
#define AOA_STRING_MODEL        1
#define AOA_STRING_DESCRIPTION  2
#define AOA_STRING_VERSION      3
#define AOA_STRING_URI          4
#define AOA_STRING_SERIAL       5

/* Must match the Android app accessory filter (manufacturer + model). */
static const char *const aoa_strings[6] = {
    "FocusPod",                  /* manufacturer */
    "FocusPod",                  /* model        */
    "FocusPod study pod",        /* description  */
    "1.0",                       /* version      */
    "",                          /* uri          */
    "FocusPod-0001",             /* serial       */
};

#define AOA_USB_DEVICE_ADDR   0x02

#define AOA_ENUM_RETRY_MAX    5
#define AOA_SWITCH_DETACH_MS  1000
#define AOA_SWITCH_ATTACH_MS  10000

/******************************************************************************/
/* Buffers (the USBFS host engine DMA transfers through its own 64 byte
 * endpoint buffers, these hold enumeration descriptors and line data) */

__attribute__((aligned(4))) static uint8_t dev_buf[64];
__attribute__((aligned(4))) static uint8_t cfg_buf[512];

#define RX_LINE_MAX 64
static char     rx_line[RX_LINE_MAX];
static uint8_t  rx_len;

static char     tx_pending[RX_LINE_MAX];
static uint8_t  tx_pending_len;

/******************************************************************************/
/* Session state */

typedef enum
{
    ST_WAIT_ATTACH = 0, /* phone not enumerated yet          */
    ST_ENUM,            /* enumerating / running AOA handshake */
    ST_SWITCH_DETACH,   /* AOA START sent, waiting bus detach */
    ST_SWITCH_ATTACH,   /* waiting for accessory re-attach    */
    ST_RUN,             /* accessory bulk link up             */
} aoa_state_t;

static aoa_state_t state = ST_WAIT_ATTACH;
static uint16_t    state_timer;

static uint8_t  enum_retries;
static uint8_t  ep0_size;
static uint8_t  in_ep;          /* accessory bulk IN  endpoint number  */
static uint8_t  out_ep;         /* accessory bulk OUT endpoint number  */
static uint8_t  in_ep_size;
static uint8_t  in_tog;
static uint8_t  out_tog;

static uint16_t last_cmd_tick;
static uint8_t  link_up;

/******************************************************************************/
/* Low level helpers */

static uint8_t usb_dev_attached(void)
{
    return (USBFSH->MIS_ST & USBFS_UMS_DEV_ATTACH) ? 1 : 0;
}

static void clear_detect_flag(void)
{
    USBFSH->INT_FG = USBFS_UIF_DETECT;
}

static uint8_t ctrl_request(uint8_t bRequestType, uint8_t bRequest,
                            uint16_t wValue, uint16_t wIndex, uint16_t wLength,
                            uint8_t *data, uint16_t *rx_len)
{
    pUSBFS_SetupRequest->bRequestType = bRequestType;
    pUSBFS_SetupRequest->bRequest     = bRequest;
    pUSBFS_SetupRequest->wValue       = wValue;
    pUSBFS_SetupRequest->wIndex       = wIndex;
    pUSBFS_SetupRequest->wLength      = wLength;
    return USBFSH_CtrlTransfer(ep0_size, data, rx_len);
}

/******************************************************************************/
/* Endpoint discovery in the configuration descriptor */

static void find_accessory_endpoints(uint16_t cfg_len)
{
    uint8_t  *p = cfg_buf;
    uint16_t  rem = cfg_len;
    uint8_t   itf_class = 0xFF;

    in_ep  = 0xFF;
    out_ep = 0xFF;

    while ((rem >= 2) && (p[0] >= 2) && (rem >= p[0]))
    {
        if ((p[1] == USB_DESCR_TYP_INTERF) && (p[0] >= 9))
        {
            itf_class = p[5];
        }
        else if ((p[1] == USB_DESCR_TYP_ENDP) && (p[0] >= 7) && (itf_class == USB_DEV_CLASS_VEN_SPEC))
        {
            uint8_t attr = p[3] & 0x03;

            if (attr == 0x02) /* bulk */
            {
                if ((p[2] & 0x80) && (in_ep == 0xFF))
                {
                    in_ep      = p[2] & 0x0F;
                    in_ep_size = p[4];
                }
                else if (!(p[2] & 0x80) && (out_ep == 0xFF))
                {
                    out_ep = p[2] & 0x0F;
                }
            }
        }
        rem -= p[0];
        p += p[0];
    }
}

/******************************************************************************/
/* Enumeration + AOA handshake (blocking, bounded by the host driver timeouts) */

#define ENUM_RES_FAIL      0 /* enumeration or handshake error   */
#define ENUM_RES_ACCESSORY 1 /* accessory endpoints are ready    */
#define ENUM_RES_SWITCHED  2 /* AOA START sent, device detached  */

static uint8_t enumerate_and_handshake(void)
{
    uint8_t  s;
    uint8_t  speed;
    uint16_t len;
    uint16_t vid, pid;
    uint8_t  i;
    uint16_t cfg_len = 0;
    uint8_t  cfg_val;
    uint16_t wait;
    uint8_t  stable;

    /* Reset the device and wait until the port stays enabled (WCH flow) */
    USBFSH_ResetRootHubPort(0);
    for (wait = 0, stable = 0, s = 0; wait < 100; wait++)
    {
        if (USBFSH_EnableRootHubPort(&speed) == ERR_SUCCESS)
        {
            wait = 0;
            stable++;
            if (stable > 6)
            {
                break;
            }
        }
        else
        {
            stable = 0;
        }
        Delay_Ms(1);
    }
    if (wait >= 100)
    {
        return ENUM_RES_FAIL;
    }

    s = USBFSH_GetDeviceDescr(&ep0_size, dev_buf);
    if (s != ERR_SUCCESS)
    {
        return ENUM_RES_FAIL;
    }
    vid = (uint16_t)dev_buf[8] | ((uint16_t)dev_buf[9] << 8);
    pid = (uint16_t)dev_buf[10] | ((uint16_t)dev_buf[11] << 8);

    s = USBFSH_SetUsbAddress(ep0_size, AOA_USB_DEVICE_ADDR);
    if (s != ERR_SUCCESS)
    {
        return ENUM_RES_FAIL;
    }

    s = USBFSH_GetConfigDescr(ep0_size, cfg_buf, sizeof(cfg_buf), &cfg_len);
    if (s != ERR_SUCCESS)
    {
        return ENUM_RES_FAIL;
    }

    cfg_val = ((PUSB_CFG_DESCR)cfg_buf)->bConfigurationValue;
    s = USBFSH_SetUsbConfig(ep0_size, cfg_val);
    if (s != ERR_SUCCESS)
    {
        return ENUM_RES_FAIL;
    }

    if ((vid == AOA_VID_GOOGLE) && ((pid == AOA_PID_ACCESSORY) || (pid == AOA_PID_ACCESSORY_ADB)))
    {
        /* Already in accessory mode */
        find_accessory_endpoints(cfg_len);
        if ((in_ep == 0xFF) || (out_ep == 0xFF))
        {
            return ENUM_RES_FAIL;
        }
        board_Log("AOA: accessory mode");
        return ENUM_RES_ACCESSORY;
    }

    /* AOA handshake */
    {
        uint16_t ver = 0;

        s = ctrl_request(USB_REQ_TYP_IN | USB_REQ_TYP_VENDOR | USB_REQ_RECIP_DEVICE,
                         AOA_ACCESSORY_GET_PROTOCOL, 0, 0, 2, dev_buf, &len);
        if ((s != ERR_SUCCESS) || (len < 2))
        {
            board_Log("AOA: get protocol failed");
            return ENUM_RES_FAIL;
        }
        ver = (uint16_t)dev_buf[0] | ((uint16_t)dev_buf[1] << 8);
        if (ver < 1)
        {
            board_Log("AOA: protocol unsupported");
            return ENUM_RES_FAIL;
        }
    }

    for (i = 0; i < 6; i++)
    {
        uint16_t slen = (uint16_t)strlen(aoa_strings[i]);

        s = ctrl_request(USB_REQ_TYP_OUT | USB_REQ_TYP_VENDOR | USB_REQ_RECIP_DEVICE,
                         AOA_ACCESSORY_SEND_STRING, 0, i, slen,
                         (uint8_t *)aoa_strings[i], NULL);
        if (s != ERR_SUCCESS)
        {
            board_Log("AOA: send string failed");
            return ENUM_RES_FAIL;
        }
    }

    s = ctrl_request(USB_REQ_TYP_OUT | USB_REQ_TYP_VENDOR | USB_REQ_RECIP_DEVICE,
                     AOA_ACCESSORY_START, 0, 0, 0, NULL, NULL);
    if (s != ERR_SUCCESS)
    {
        board_Log("AOA: start failed");
        return ENUM_RES_FAIL;
    }

    board_Log("AOA: start sent");
    return ENUM_RES_SWITCHED;
}

/******************************************************************************/
/* Bulk link processing */

static void queue_tx(const char *s)
{
    uint16_t len = strlen(s);

    if (len > (RX_LINE_MAX - 1))
    {
        len = RX_LINE_MAX - 1;
    }
    memcpy(tx_pending, s, len);
    tx_pending_len = (uint8_t)len;
}

static void process_command(char *cmd)
{
    last_cmd_tick = time_MsTick_Get();

    if ((cmd[0] == 'S') && (cmd[1] == 'E') && (cmd[2] == 'T') &&
        (cmd[3] == '_') && (cmd[4] == 'O') && (cmd[5] == 'U') && (cmd[6] == 'T') &&
        (cmd[7] == ' ') && ((cmd[8] == '0') || (cmd[8] == '1')) && (cmd[9] == '\0'))
    {
        /* Q1 output switch removed from the board (PA1 is now LED1 with a
         * firmware heartbeat): keep accepting SET_OUT for host compatibility. */
        board_Log("AOA: SET_OUT ignored (no output switch)");
        queue_tx("OK");
    }
    else if (memcmp(cmd, "READ_IN", 8) == 0)
    {
        queue_tx(board_In_Read() ? "IN 1" : "IN 0");
    }
    else if (memcmp(cmd, "PING", 5) == 0)
    {
        queue_tx("PONG");
    }
    else if (memcmp(cmd, "RESET", 6) == 0)
    {
        /* Safe reboot: shut the session down first, then force USER boot
         * mode so the high level on PC17 (phone pull-up) cannot route us
         * into the ROM bootloader at reset. */
        queue_tx("OK");
        board_Power_Enable(0);
        cc_SourceRp_Enable(0);
        Delay_Ms(CC_DISCHARGE_MS);
        SystemReset_StartMode(Start_Mode_USER);
        NVIC_SystemReset();
    }
    else
    {
        board_Log("AOA: unknown command");
        queue_tx("ERR CMD");
    }
}

static void feed_rx(uint8_t *data, uint16_t len)
{
    while (len--)
    {
        char c = (char)*data++;

        if ((c == '\r') || (c == '\0'))
        {
            continue;
        }
        if (c == '\n')
        {
            if (rx_len > 0)
            {
                rx_line[rx_len] = '\0';
                process_command(rx_line);
                rx_len = 0;
            }
            continue;
        }
        if (rx_len < (RX_LINE_MAX - 1))
        {
            rx_line[rx_len++] = c;
        }
        else
        {
            /* line overflow: drop it */
            rx_len = 0;
        }
    }
}

static uint8_t bulk_poll(void)
{
    uint8_t  s;
    uint16_t len = 0;

    /* Try to flush a pending response first */
    if (tx_pending_len)
    {
        s = USBFSH_SendEndpData(out_ep, &out_tog, (uint8_t *)tx_pending, tx_pending_len);
        if (s == ERR_SUCCESS)
        {
            tx_pending_len = 0;
        }
        else if (s == (USB_PID_STALL | ERR_USB_TRANSFER))
        {
            (void)USBFSH_ClearEndpStall(ep0_size, out_ep);
        }
        else if (s == ERR_USB_DISCON)
        {
            return 0;
        }
    }

    s = USBFSH_GetEndpData(in_ep, &in_tog, dev_buf, &len);
    if (s == ERR_SUCCESS)
    {
        feed_rx(dev_buf, len);
    }
    else if (s == (USB_PID_NAK | ERR_USB_TRANSFER))
    {
        /* no data pending, normal */
    }
    else if (s == (USB_PID_STALL | ERR_USB_TRANSFER))
    {
        (void)USBFSH_ClearEndpStall(ep0_size, in_ep);
    }
    else if (s == ERR_USB_DISCON)
    {
        return 0;
    }

    if (!usb_dev_attached())
    {
        return 0;
    }
    return 1;
}

/******************************************************************************/
/* Public API */

void aoa_Init(void)
{
    USBFS_RCC_Init();
    USBFS_Host_Init(ENABLE, PWR_VDD_5V);

    state       = ST_WAIT_ATTACH;
    link_up     = 0;
    rx_len      = 0;
    tx_pending_len = 0;
    in_tog      = 0;
    out_tog     = 0;
    enum_retries = 0;
    clear_detect_flag();
}

void aoa_Shutdown(void)
{
    USBFS_Host_Init(DISABLE, PWR_VDD_5V);
    link_up = 0;
    state = ST_WAIT_ATTACH;
}

uint8_t aoa_Running(void)
{
    return link_up;
}

uint16_t aoa_MsSinceLastCommand(void)
{
    return time_ElapsedMs(last_cmd_tick);
}

uint8_t aoa_Poll(void)
{
    uint8_t events = AOA_EVT_NONE;

    switch (state)
    {
    case ST_WAIT_ATTACH:
        if (USBFSH->INT_FG & USBFS_UIF_DETECT)
        {
            clear_detect_flag();
        }
        if (usb_dev_attached())
        {
            Delay_Ms(100); /* contact debounce */
            if (usb_dev_attached())
            {
                state = ST_ENUM;
                enum_retries = 0;
                board_Log("USB: device attached");
            }
        }
        break;

    case ST_ENUM:
        if (!usb_dev_attached())
        {
            state = ST_WAIT_ATTACH;
            break;
        }

        switch (enumerate_and_handshake())
        {
        case ENUM_RES_ACCESSORY:
            state   = ST_RUN;
            link_up = 1;
            events |= AOA_EVT_CONNECTED;
            rx_len     = 0;
            tx_pending_len = 0;
            in_tog     = 0;
            out_tog    = 0;
            last_cmd_tick = time_MsTick_Get();
            break;

        case ENUM_RES_SWITCHED:
            state       = ST_SWITCH_DETACH;
            state_timer = time_MsTick_Get();
            break;

        default: /* ENUM_RES_FAIL */
            if (++enum_retries >= AOA_ENUM_RETRY_MAX)
            {
                board_Log("USB: enum failed, reset port");
                enum_retries = 0;
                USBFS_Host_Init(ENABLE, PWR_VDD_5V);
                clear_detect_flag();
                state = ST_WAIT_ATTACH;
            }
            else
            {
                Delay_Ms(200);
            }
            break;
        }
        break;

    case ST_SWITCH_DETACH:
        if (!usb_dev_attached())
        {
            state       = ST_SWITCH_ATTACH;
            state_timer = time_MsTick_Get();
            board_Log("AOA: bus detached, waiting accessory");
            break;
        }
        if (time_ElapsedMs(state_timer) > AOA_SWITCH_DETACH_MS)
        {
            state = ST_WAIT_ATTACH;
        }
        break;

    case ST_SWITCH_ATTACH:
        if (USBFSH->INT_FG & USBFS_UIF_DETECT)
        {
            clear_detect_flag();
        }
        if (usb_dev_attached())
        {
            Delay_Ms(100);
            if (usb_dev_attached())
            {
                state       = ST_ENUM;
                enum_retries = 0;
            }
            break;
        }
        if (time_ElapsedMs(state_timer) > AOA_SWITCH_ATTACH_MS)
        {
            board_Log("AOA: re-attach timeout");
            USBFS_Host_Init(ENABLE, PWR_VDD_5V);
            clear_detect_flag();
            state = ST_WAIT_ATTACH;
        }
        break;

    case ST_RUN:
        if (!bulk_poll())
        {
            board_Log("USB: accessory lost");
            link_up = 0;
            events |= AOA_EVT_DISCONNECTED;
            USBFS_Host_Init(ENABLE, PWR_VDD_5V);
            clear_detect_flag();
            state = ST_WAIT_ATTACH;
        }
        break;

    default:
        state = ST_WAIT_ATTACH;
        break;
    }

    return events;
}
