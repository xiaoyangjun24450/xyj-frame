/********************************** (C) COPYRIGHT *******************************
 * File Name          : time.c
 * Description        : 1 kHz free running tick based on TIM2 (polled).
 *********************************************************************************/
#include "ch32x035.h"
#include "time.h"

void time_Init(void)
{
    TIM_TimeBaseInitTypeDef tim = {0};

    RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM2, ENABLE);

    /* 48 MHz / (48000-1) = 1 kHz, counter wraps every ~65.5 s: 1 tick == 1 ms */
    tim.TIM_Period            = 0xFFFF;
    tim.TIM_Prescaler         = 48000 - 1;
    tim.TIM_ClockDivision     = TIM_CKD_DIV1;
    tim.TIM_CounterMode       = TIM_CounterMode_Up;
    tim.TIM_RepetitionCounter = 0;
    TIM_TimeBaseInit(TIM2, &tim);

    TIM_Cmd(TIM2, ENABLE);
}

uint16_t time_MsTick_Get(void)
{
    return (uint16_t)TIM_GetCounter(TIM2);
}

uint16_t time_ElapsedMs(uint16_t since)
{
    return (uint16_t)(time_MsTick_Get() - since);
}

uint8_t time_EveryMs(uint16_t *cookie, uint16_t period_ms)
{
    uint16_t now = time_MsTick_Get();

    if ((uint16_t)(now - *cookie) >= period_ms)
    {
        *cookie = now;
        return 1;
    }
    return 0;
}
