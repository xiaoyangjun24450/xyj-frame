/********************************** (C) COPYRIGHT *******************************
 * File Name          : cc_power.h
 * Description        : Type-C CC management for FocusPod
 *
 *  - J1 (charger input): detect the 3A Rp advertisement on CC1/CC2 through
 *    the on-board 5.1k Rd resistors, using the ADC on PA2/PA4.
 *  - J2 (phone port): present a 180uA Rp (1.5A source role) on CC1/CC2 via
 *    the USBPD PHY and detect the phone Rd attach/detach with debounce.
 *********************************************************************************/
#ifndef __FOCUSPOD_CC_POWER_H
#define __FOCUSPOD_CC_POWER_H

#include <stdint.h>

/* Time the J2 VBUS discharge path (R7 1k + C3 100uF, tau ~= 120ms) needs. */
#define CC_DISCHARGE_MS 650

void cc_Init(void);

/* J1 input capability, must be polled periodically.
 * Returns 1 when either CC advertises 3A (Rp = 10k, vRd ~= 1.7V).
 * Internal debounce: 3 consecutive samples. */
uint8_t cc_Input3A_Present(void);

/* J2 source role: enable / disable the 180uA Rp on both CC lines. */
void cc_SourceRp_Enable(uint8_t enable);

/* J2 phone attach status, must be polled periodically (~5ms).
 * Returns 1 when exactly one CC line is pulled into the Rd window
 * (0.66V < vCC < 1.23V), debounced 5 consecutive samples. */
uint8_t cc_SinkAttached(void);

#endif
