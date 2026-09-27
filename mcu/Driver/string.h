/********************************** (C) COPYRIGHT *******************************
 * File Name          : string.h
 * Description        : Freestanding string shim for the no-newlib toolchain.
 *                      Declares only what this firmware actually uses.
 *********************************************************************************/
#ifndef __FOCUSPOD_STRING_SHIM_H
#define __FOCUSPOD_STRING_SHIM_H

#include <stddef.h>

void  *memcpy(void *dest, const void *src, size_t n);
void  *memset(void *s, int c, size_t n);
size_t strlen(const char *s);
int    memcmp(const void *a, const void *b, size_t n);

#endif
