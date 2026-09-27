#include <stdint.h>
#include <string.h>

#include "ch552.h"
#include "ch554_usb.h"
#include "debug.h"
#include "bootloader.h"

#include "time.h"

#define ENABLE_IAP_PIN 6
SBIT(EnableIAP, 0xB0, ENABLE_IAP_PIN);

#define LED_PIN 0
SBIT(LED, 0xB0, LED_PIN);

#define PULSE_PIN 6
SBIT(PULSE_OUT, 0x90, PULSE_PIN);

#define PULSE_MODE_RUN        0
#define PULSE_MODE_FORCED_LOW 1
#define PULSE_PERIOD_MS       5000
#define PULSE_HIGH_MS         4800

#define EP0_SIZE       DEFAULT_ENDP0_SIZE
#define FOCUSPOD_VENDOR_SET_OUTPUT 0x01

__xdata __at(0x0000) uint8_t Ep0Buffer[EP0_SIZE + 2];

static uint8_t __data usb_config;
static volatile uint8_t __data pulse_mode;
static volatile uint8_t __data pulse_restart;

static uint8_t __data setup_req;
static uint8_t __data setup_len;
static const uint8_t __code * __data setup_descr;

#define UsbSetupBuf ((PXUSB_SETUP_REQ)Ep0Buffer)

static const uint8_t __code DevDesc[] = {
    0x12, 0x01, 0x10, 0x01,
    0xFF, 0x00, 0x00, EP0_SIZE,
    0x31, 0x51, 0x08, 0x20,
    0x00, 0x01, 0x01, 0x02,
    0x00, 0x01
};

static const uint8_t __code CfgDesc[] = {
    0x09, 0x02, 0x12, 0x00, 0x01, 0x01, 0x00, 0x80, 0x32,
    0x09, 0x04, 0x00, 0x00, 0x00, 0xFF, 0x00, 0x00, 0x00
};

static const uint8_t __code LangDesc[] = {
    0x04, 0x03, 0x09, 0x04
};

static const uint8_t __code ManuDesc[] = {
    0x0E, 0x03, 'F', 0, 'o', 0, 'c', 0, 'u', 0, 's', 0, 'P', 0
};

static const uint8_t __code ProdDesc[] = {
    0x12, 0x03, 'C', 0, 'H', 0, '5', 0, '5', 0, '2', 0, ' ', 0, 'I', 0, 'O', 0
};

static void Pulse_SetOutput(uint8_t high)
{
    if (high)
    {
        PULSE_OUT = 1;
        LED = 1;
    }
    else
    {
        PULSE_OUT = 0;
        LED = 0;
    }
}

static void Pulse_SetCommand(uint8_t command)
{
    if (command == 0x01)
    {
        pulse_mode = PULSE_MODE_FORCED_LOW;
        pulse_restart = 0;
        Pulse_SetOutput(0);
    }
    else if (command == 0x00)
    {
        pulse_mode = PULSE_MODE_RUN;
        pulse_restart = 1;
    }
}

void IAP_Init(void)
{
    USB_CTRL = 0x00;
    USB_CTRL = bUC_RESET_SIE | bUC_CLR_ALL;
    USB_CTRL &= ~bUC_RESET_SIE;
    UDEV_CTRL &= ~bUD_PD_DIS;
    P3_MOD_OC &= ~(1 << ENABLE_IAP_PIN);
    P3_DIR_PU &= ~(1 << ENABLE_IAP_PIN);

    mDelaymS(10);

    if (EnableIAP == 1)
    {
        EA = 0;
        TMOD = 1;
        mDelaymS(100);
        bootloader();
    }
}

static void IO_Init(void)
{
    P1_MOD_OC &= ~(1 << PULSE_PIN);
    P1_DIR_PU |= (1 << PULSE_PIN);

    P3_MOD_OC &= ~(1 << LED_PIN);
    P3_DIR_PU |= (1 << LED_PIN);

    pulse_mode = PULSE_MODE_RUN;
    pulse_restart = 1;
    Pulse_SetOutput(0);
}

static void USBDeviceInit(void)
{
    IE_USB = 0;
    USB_CTRL = 0x00;

    UEP4_1_MOD &= ~(bUEP4_RX_EN | bUEP4_TX_EN);
    UEP2_3_MOD &= ~(bUEP2_RX_EN | bUEP2_TX_EN | bUEP2_BUF_MOD);

    UEP0_DMA = (uint16_t)Ep0Buffer;

    UEP0_CTRL = UEP_R_RES_ACK | UEP_T_RES_NAK;

    USB_DEV_AD = 0x00;
    UDEV_CTRL = bUD_PD_DIS;
    UDEV_CTRL &= ~bUD_LOW_SPEED;
    USB_CTRL = bUC_DEV_PU_EN | bUC_INT_BUSY | bUC_DMA_EN;
    UDEV_CTRL |= bUD_PORT_EN;

    usb_config = 0;
    USB_INT_FG = 0xFF;
    USB_INT_EN = bUIE_SUSPEND | bUIE_TRANSFER | bUIE_BUS_RST;
    IE_USB = 1;
}

void USBInterrupt(void) __interrupt(INT_NO_USB) __using(1)
{
    uint8_t len;

    if (UIF_TRANSFER)
    {
        switch (USB_INT_ST & (MASK_UIS_TOKEN | MASK_UIS_ENDP))
        {
        case UIS_TOKEN_SETUP | 0:
            UEP0_CTRL = bUEP_R_TOG | bUEP_T_TOG | UEP_R_RES_ACK | UEP_T_RES_ACK;
            len = USB_RX_LEN;

            if (len == sizeof(USB_SETUP_REQ))
            {
                setup_len = UsbSetupBuf->wLengthL;
                if (UsbSetupBuf->wLengthH || setup_len > 0x7F)
                {
                    setup_len = 0x7F;
                }

                setup_req = UsbSetupBuf->bRequest;
                len = 0;

                if ((UsbSetupBuf->bRequestType & USB_REQ_TYP_MASK) == USB_REQ_TYP_VENDOR)
                {
                    if ((UsbSetupBuf->bRequestType & USB_REQ_TYP_IN) == USB_REQ_TYP_OUT &&
                        (UsbSetupBuf->bRequestType & USB_REQ_RECIP_MASK) == USB_REQ_RECIP_DEVICE &&
                        setup_req == FOCUSPOD_VENDOR_SET_OUTPUT)
                    {
                        Pulse_SetCommand(UsbSetupBuf->wValueL);
                        setup_len = 0;
                    }
                    else
                    {
                        len = 0xFF;
                    }
                }
                else if ((UsbSetupBuf->bRequestType & USB_REQ_TYP_MASK) != USB_REQ_TYP_STANDARD)
                {
                    len = 0xFF;
                }
                else
                {
                    switch (setup_req)
                    {
                    case USB_GET_DESCRIPTOR:
                        switch (UsbSetupBuf->wValueH)
                        {
                        case USB_DESCR_TYP_DEVICE:
                            setup_descr = DevDesc;
                            len = sizeof(DevDesc);
                            break;
                        case USB_DESCR_TYP_CONFIG:
                            setup_descr = CfgDesc;
                            len = sizeof(CfgDesc);
                            break;
                        case USB_DESCR_TYP_STRING:
                            switch (UsbSetupBuf->wValueL)
                            {
                            case 0:
                                setup_descr = LangDesc;
                                len = sizeof(LangDesc);
                                break;
                            case 1:
                                setup_descr = ManuDesc;
                                len = sizeof(ManuDesc);
                                break;
                            case 2:
                                setup_descr = ProdDesc;
                                len = sizeof(ProdDesc);
                                break;
                            default:
                                len = 0xFF;
                                break;
                            }
                            break;
                        default:
                            len = 0xFF;
                            break;
                        }

                        if (len != 0xFF)
                        {
                            if (setup_len > len)
                            {
                                setup_len = len;
                            }
                            len = (setup_len >= EP0_SIZE) ? EP0_SIZE : setup_len;
                            memcpy(Ep0Buffer, setup_descr, len);
                            setup_len -= len;
                            setup_descr += len;
                        }
                        break;

                    case USB_SET_ADDRESS:
                        setup_len = UsbSetupBuf->wValueL;
                        break;

                    case USB_GET_CONFIGURATION:
                        Ep0Buffer[0] = usb_config;
                        if (setup_len >= 1)
                        {
                            len = 1;
                        }
                        break;

                    case USB_SET_CONFIGURATION:
                        usb_config = UsbSetupBuf->wValueL;
                        break;

                    case USB_GET_INTERFACE:
                        Ep0Buffer[0] = 0;
                        if (setup_len >= 1)
                        {
                            len = 1;
                        }
                        break;

                    case USB_GET_STATUS:
                        Ep0Buffer[0] = 0;
                        Ep0Buffer[1] = 0;
                        len = (setup_len >= 2) ? 2 : setup_len;
                        break;

                    default:
                        len = 0xFF;
                        break;
                    }
                }
            }
            else
            {
                len = 0xFF;
            }

            if (len == 0xFF)
            {
                setup_req = 0xFF;
                UEP0_CTRL = bUEP_R_TOG | bUEP_T_TOG | UEP_R_RES_STALL | UEP_T_RES_STALL;
            }
            else
            {
                UEP0_T_LEN = len;
                UEP0_CTRL = bUEP_R_TOG | bUEP_T_TOG | UEP_R_RES_ACK | UEP_T_RES_ACK;
            }
            break;

        case UIS_TOKEN_IN | 0:
            switch (setup_req)
            {
            case USB_GET_DESCRIPTOR:
                len = (setup_len >= EP0_SIZE) ? EP0_SIZE : setup_len;
                memcpy(Ep0Buffer, setup_descr, len);
                setup_len -= len;
                setup_descr += len;
                UEP0_T_LEN = len;
                UEP0_CTRL ^= bUEP_T_TOG;
                break;

            case USB_SET_ADDRESS:
                USB_DEV_AD = (USB_DEV_AD & bUDA_GP_BIT) | setup_len;
                UEP0_CTRL = UEP_R_RES_ACK | UEP_T_RES_NAK;
                break;

            default:
                UEP0_T_LEN = 0;
                UEP0_CTRL = UEP_R_RES_ACK | UEP_T_RES_NAK;
                break;
            }
            break;

        case UIS_TOKEN_OUT | 0:
            UEP0_CTRL = UEP_R_RES_ACK | UEP_T_RES_NAK;
            break;

        default:
            break;
        }

        UIF_TRANSFER = 0;
    }

    if (UIF_BUS_RST)
    {
        UEP0_CTRL = UEP_R_RES_ACK | UEP_T_RES_NAK;
        USB_DEV_AD = 0x00;
        usb_config = 0;
        UIF_SUSPEND = 0;
        UIF_TRANSFER = 0;
        UIF_BUS_RST = 0;
    }

    if (UIF_SUSPEND)
    {
        UIF_SUSPEND = 0;
    }
}

void main(void)
{
    uint16_t pulse_start = 0;
    uint16_t now;
    uint16_t elapsed;
    uint8_t pulse_high;

    CfgFsys();
    mDelaymS(10);
    IAP_Init();
    IO_Init();
    time_MsTick_init();
    USBDeviceInit();

    EA = 1;
    pulse_start = time_MsTick_Get();

    while (1)
    {
        now = time_MsTick_Get();

        if (pulse_mode == PULSE_MODE_FORCED_LOW)
        {
            pulse_start = now;
            Pulse_SetOutput(0);
        }
        else
        {
            if (pulse_restart)
            {
                pulse_restart = 0;
                pulse_start = now;
            }

            elapsed = now - pulse_start;
            if (elapsed >= PULSE_PERIOD_MS)
            {
                pulse_start = now;
                elapsed = 0;
            }

            pulse_high = elapsed < PULSE_HIGH_MS;
            Pulse_SetOutput(pulse_high);
        }

        time_MsTick_Delay(0);
    }
}

void Timer2_Interrupt(void) __interrupt(INT_NO_TMR2)
{
    time_MsTickInterrupt();
}
