/********************************** (C) COPYRIGHT *******************************
 * File Name          : time.h
 * Description        : 1 kHz free running tick (TIM2, polled, no interrupt).
 *********************************************************************************/
#ifndef __FOCUSPOD_TIME_H
#define __FOCUSPOD_TIME_H

#include <stdint.h>

/* Start TIM2 as a free running 1 kHz counter (wraps at 65536 s ~= 65.5 s). */
void time_Init(void);

/* Current tick, 1 unit == 1 ms. Use unsigned subtraction for wrap-safe math. */
uint16_t time_MsTick_Get(void);

/* Elapsed ms since `since` (wrap-safe). */
uint16_t time_ElapsedMs(uint16_t since);

/* Non-blocking periodic scheduler helper: returns 1 once every period_ms.
   Keep the returned cookie between calls. */
uint8_t time_EveryMs(uint16_t *cookie, uint16_t period_ms);

#endif
