/********************************** (C) COPYRIGHT *******************************
 * File Name          : usbh_aoa.h
 * Description        : USB host stack + Android Open Accessory (AOA) handshake
 *                      + FocusPod bulk application protocol.
 *
 *  The MCU is the USB host on J2. After the phone is enumerated, the AOA
 *  handshake (GET_PROTOCOL / SEND_STRING / START) is performed and the phone
 *  re-enumerates as an accessory (VID 0x18D1, PID 0x2D00/0x2D01) with two
 *  bulk endpoints.
 *
 *  Bulk application protocol (ASCII, '\n' terminated, request/response):
 *    phone -> MCU : "SET_OUT 0" | "SET_OUT 1" | "READ_IN" | "PING"
 *    MCU -> phone : "OK" | "ERR ..." | "IN 0" | "IN 1" | "PONG"
 *    SET_OUT 1 = output ON (Q1 gate driven LOW), SET_OUT 0 = output OFF.
 *    Any valid command refreshes the link activity watchdog.
 *********************************************************************************/
#ifndef __USBH_AOA_H
#define __USBH_AOA_H

#include <stdint.h>

/* Events reported by aoa_Poll() (bitmask, may be combined). */
#define AOA_EVT_NONE         0x00
#define AOA_EVT_CONNECTED    0x01 /* accessory link established */
#define AOA_EVT_DISCONNECTED 0x02 /* phone / accessory lost     */

void aoa_Init(void);      /* bring up the USB host controller        */
void aoa_Shutdown(void);  /* stop the host controller (session end) */

/* Advance the state machine, must be called frequently from the main loop. */
uint8_t aoa_Poll(void);

/* 1 while the accessory bulk link is up. */
uint8_t aoa_Running(void);

/* Milliseconds since the last valid command from the phone (wrap-safe). */
uint16_t aoa_MsSinceLastCommand(void);

#endif
