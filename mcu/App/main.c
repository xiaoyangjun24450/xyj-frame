/********************************** (C) COPYRIGHT *******************************
 * File Name          : main.c
 * Description        : FocusPod power session state machine (CH32X035G8U6).
 *
 *  Firmware sequence (see hardware/电路设计.md):
 *   1. power on: U2 (phone VBUS) off, J2 Rp off, output off, wait 650ms for
 *      the J2 VBUS discharge path (R7/C3).
 *   2. check the J1 charger 3A advertisement through the CC ADC.
 *   3. only then enable the J2 180uA Rp and wait for the phone Rd attach
 *      (debounced) before switching U2 on.
 *   4. run the USB host + AOA handshake and the bulk command protocol.
 *   5. on input capability loss or phone detach: turn U2 and Rp off together,
 *      wait 650ms, restart detection from step 2.
 *   6. LED1 on PA1 blinks at 0.5s as a heartbeat (Q1 removed, PA1 = LED1).
 *********************************************************************************/
#include "ch32x035.h"
#include "board.h"
#include "cc_power.h"
#include "time.h"
#include "delay.h"
#include "usbh_aoa.h"

/* Safety watchdog: force the link down when the accessory link is up but
 * the app stopped issuing commands for this long. */
#define APP_CMD_TIMEOUT_MS 5000

/* LED1 heartbeat period. */
#define APP_LED_TOGGLE_MS 500

typedef enum
{
    APP_DISCHARGE = 0, /* everything off, VBUS discharging      */
    APP_WAIT_INPUT,    /* waiting for the J1 3A advertisement   */
    APP_WAIT_SINK,     /* Rp on, waiting for the phone (Rd)     */
    APP_SESSION,       /* U2 on, USB host + AOA running         */
} app_state_t;

static app_state_t app_state = APP_DISCHARGE;
static uint16_t    state_tick;

static void session_shutdown(const char *reason)
{
    board_Log(reason);
    board_Power_Enable(0);
    aoa_Shutdown();
    cc_SourceRp_Enable(0);

    app_state  = APP_DISCHARGE;
    state_tick = time_MsTick_Get();
}

int main(void)
{
    uint16_t cookie_input = 0;
    uint16_t cookie_sink  = 0;
    uint16_t cookie_mon   = 0;
    uint16_t cookie_led   = 0;

    SystemCoreClockUpdate();
    Delay_Init();
    board_Init();
    time_Init();
    cc_Init();

    board_Led_Set(0);
    board_Power_Enable(0);
    cc_SourceRp_Enable(0);

    app_state  = APP_DISCHARGE;
    state_tick = time_MsTick_Get();

    for (;;)
    {
        if (time_EveryMs(&cookie_led, APP_LED_TOGGLE_MS))
        {
            board_Led_Toggle();
        }

        switch (app_state)
        {
        case APP_DISCHARGE:
            if (time_ElapsedMs(state_tick) >= CC_DISCHARGE_MS)
            {
                app_state = APP_WAIT_INPUT;
                board_Log("PWR: discharged, waiting for input");
            }
            break;

        case APP_WAIT_INPUT:
            if (time_EveryMs(&cookie_input, 100))
            {
                if (cc_Input3A_Present())
                {
                    board_Log("PWR: 3A input confirmed, Rp on");
                    cc_SourceRp_Enable(1);
                    app_state = APP_WAIT_SINK;
                }
            }
            break;

        case APP_WAIT_SINK:
            if (time_EveryMs(&cookie_sink, 5))
            {
                if (!cc_Input3A_Present())
                {
                    session_shutdown("PWR: input lost (sink wait)");
                    break;
                }
                if (cc_SinkAttached())
                {
                    board_Log("PWR: phone attached, U2 on");
                    board_Power_Enable(1);
                    Delay_Ms(50); /* let VBUS_PHONE rise */
                    aoa_Init();
                    app_state = APP_SESSION;
                }
            }
            break;

        case APP_SESSION:
        {
            uint8_t events = aoa_Poll();

            if (events & AOA_EVT_CONNECTED)
            {
                board_Log("PWR: accessory connected");
            }
            if (events & AOA_EVT_DISCONNECTED)
            {
                /* Phone rebooted or cable glitch: the host stack keeps
                 * waiting for re-attach, the session itself survives. */
                board_Log("PWR: accessory disconnected");
            }

            if (time_EveryMs(&cookie_mon, 5))
            {
                if (!cc_Input3A_Present())
                {
                    session_shutdown("PWR: input lost (session)");
                    break;
                }
                if (!cc_SinkAttached())
                {
                    session_shutdown("PWR: phone detached");
                    break;
                }
            }

            if (aoa_Running() && (aoa_MsSinceLastCommand() > APP_CMD_TIMEOUT_MS))
            {
                session_shutdown("PWR: app command watchdog");
            }
            break;
        }

        default:
            session_shutdown("PWR: invalid state");
            break;
        }
    }
}
