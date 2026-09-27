/********************************** (C) COPYRIGHT *******************************
 * File Name          : board.h
 * Description        : FocusPod board pin map (CH32X035G8U6, QFN28)
 *
 *  See hardware/电路设计.md:
 *    PA0  (5)  IN   : dry-contact input (to 5V_SYS), internal pull-down
 *    PA1  (6)  OUT  : LED1 (Q1 removed), HIGH = lit, 0.5s heartbeat
 *    PA2  (7)  J1CC1: charger CC1 via 5.1k Rd, ADC channel 2
 *    PA4  (9)  J1CC2: charger CC2 via 5.1k Rd, ADC channel 4
 *    PA5  (10) U2EN : RT9742 EN, active high (R4 5.1k pull-down at reset)
 *    PC14 (28) J2CC1: phone port CC1 (USBPD PHY)
 *    PC15 (1)  J2CC2: phone port CC2 (USBPD PHY)
 *    PC16 (26) D-    : USB D- to phone
 *    PC17 (27) D+    : USB D+ to phone (also ROM BOOT pin)
 *    PB10 (23) DBG   : optional debug UART1 TX (unpopulated)
 *********************************************************************************/
#ifndef __FOCUSPOD_BOARD_H
#define __FOCUSPOD_BOARD_H

#include <stdint.h>

void board_Init(void);

/* Digital input, 1 = closed (shorted to 5V_SYS), 0 = open. */
uint8_t board_In_Read(void);

/* LED1 on PA1, 1 = lit. */
void board_Led_Set(uint8_t on);
void board_Led_Toggle(void);

/* Phone VBUS (U2) enable, 1 = on. */
void board_Power_Enable(uint8_t on);
uint8_t board_Power_Get(void);

/* Optional debug log on USART1/PB10, compiled out unless enabled. */
void board_Log(const char *s);
void board_LogHex(const char *tag, uint32_t value);

#endif
