/********************************** (C) COPYRIGHT *******************************
 * File Name          : delay.c
 * Description        : SysTick based busy-wait delay (logic taken from the
 *                      WCH EVT debug.c, printf/UART parts removed).
 *********************************************************************************/
#include "ch32x035.h"
#include "delay.h"

static uint8_t  s_p_us = 0;
static uint16_t s_p_ms = 0;

void Delay_Init(void)
{
    s_p_us = SystemCoreClock / 8000000;
    s_p_ms = (uint16_t)s_p_us * 1000;
}

void Delay_Us(uint32_t n)
{
    uint32_t i;

    SysTick->SR &= ~(1 << 0);
    i = (uint32_t)n * s_p_us;

    SysTick->CMP = i;
    SysTick->CTLR |= (1 << 4);
    SysTick->CTLR |= (1 << 5) | (1 << 0);

    while ((SysTick->SR & (1 << 0)) != (1 << 0))
    {
    }
    SysTick->CTLR &= ~(1 << 0);
}

void Delay_Ms(uint32_t n)
{
    uint32_t i;

    SysTick->SR &= ~(1 << 0);
    i = (uint32_t)n * s_p_ms;

    SysTick->CMP = i;
    SysTick->CTLR |= (1 << 4);
    SysTick->CTLR |= (1 << 5) | (1 << 0);

    while ((SysTick->SR & (1 << 0)) != (1 << 0))
    {
    }
    SysTick->CTLR &= ~(1 << 0);
}
