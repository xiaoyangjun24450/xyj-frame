/********************************** (C) COPYRIGHT *******************************
 * File Name          : delay.h
 * Description        : Blocking micro/millisecond delay, SysTick based
 *                      (same API as the WCH EVT debug.c so the vendor USB
 *                       host driver can be used unmodified).
 *********************************************************************************/
#ifndef __FOCUSPOD_DELAY_H
#define __FOCUSPOD_DELAY_H

#include <stdint.h>

void Delay_Init(void);
void Delay_Us(uint32_t n);
void Delay_Ms(uint32_t n);

#endif
