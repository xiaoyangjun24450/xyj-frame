/********************************** (C) COPYRIGHT *******************************
 * File Name          : usb_host_config.h
 * Description        : Minimal USB host stack configuration for FocusPod
 *                      (adapted from WCH CH32X035 EVT HOST_KM example, HID/hub
 *                       layers removed, only the common definitions needed by
 *                       Driver/ch32x035_usbfs_host.c are kept)
*********************************************************************************/

#ifndef __USB_HOST_CONFIG_H__
#define __USB_HOST_CONFIG_H__

#include <string.h>
#include "ch32x035.h"
#include <ch32x035_usb.h>
#include <ch32x035_usbfs_host.h>
#include "ch32x035_pwr.h"
#include "delay.h"

/******************************************************************************/
/* USB Host Communication Related Macro Definition */

/* Root device status */
#define ROOT_DEV_DISCONNECT         0
#define ROOT_DEV_CONNECTED          1
#define ROOT_DEV_FAILED             2
#define ROOT_DEV_SUCCESS            3

/* USB speed */
#define USB_LOW_SPEED               0x00
#define USB_FULL_SPEED              0x01

/* USB communication status code */
#define ERR_SUCCESS                 0x00
#define ERR_USB_CONNECT             0x15
#define ERR_USB_DISCON              0x16
#define ERR_USB_BUF_OVER            0x17
#define ERR_USB_DISK_ERR            0x1F
#define ERR_USB_TRANSFER            0x20
#define ERR_USB_UNSUPPORT           0xFB
#define ERR_USB_UNAVAILABLE         0xFC
#define ERR_USB_UNKNOWN             0xFE

/* USB communication time */
#define DEF_BUS_RESET_TIME          11          /* USB bus reset time (ms) */
#define DEF_WAIT_USB_TRANSFER_CNT   1000        /* Wait for the USB transfer to complete */
#define DEF_CTRL_TRANS_TIMEOVER_CNT 200000 / 20 /* Control transmission timeout */

#endif /* __USB_HOST_CONFIG_H__ */
