/********************************** (C) COPYRIGHT *******************************
 * File Name          : stdint.h
 * Description        : Freestanding stdint shim for the no-newlib toolchain
 *                      (ILP32 / RV32).
 *********************************************************************************/
#ifndef __FOCUSPOD_STDINT_SHIM_H
#define __FOCUSPOD_STDINT_SHIM_H

typedef signed char        int8_t;
typedef unsigned char      uint8_t;
typedef short              int16_t;
typedef unsigned short     uint16_t;
typedef int                int32_t;
typedef unsigned int       uint32_t;
typedef long long          int64_t;
typedef unsigned long long uint64_t;

typedef int8_t   int_least8_t;
typedef uint8_t  uint_least8_t;
typedef int16_t  int_least16_t;
typedef uint16_t uint_least16_t;
typedef int32_t  int_least32_t;
typedef uint32_t uint_least32_t;
typedef int64_t  int_least64_t;
typedef uint64_t uint_least64_t;

typedef int      intptr_t;
typedef unsigned int uintptr_t;

typedef int8_t   int_fast8_t;
typedef uint8_t  uint_fast8_t;
typedef int32_t  int_fast16_t;
typedef uint32_t uint_fast16_t;
typedef int32_t  int_fast32_t;
typedef uint32_t uint_fast32_t;
typedef int64_t  int_fast64_t;
typedef uint64_t uint_fast64_t;

typedef int64_t  intmax_t;
typedef uint64_t uintmax_t;

#ifndef __INT64_C
#define __INT64_C(c)  c ## LL
#define __UINT64_C(c) c ## ULL
#endif

#define INT8_MIN    (-128)
#define INT16_MIN   (-32768)
#define INT32_MIN   (-2147483647 - 1)
#define INT8_MAX    127
#define INT16_MAX   32767
#define INT32_MAX   2147483647
#define UINT8_MAX   255
#define UINT16_MAX  65535
#define UINT32_MAX  4294967295U

#define INT_LEAST8_MIN   INT8_MIN
#define INT_LEAST16_MIN  INT16_MIN
#define INT_LEAST32_MIN  INT32_MIN
#define INT_LEAST64_MIN  (-__INT64_C(9223372036854775807) - 1)
#define INT_LEAST8_MAX   INT8_MAX
#define INT_LEAST16_MAX  INT16_MAX
#define INT_LEAST32_MAX  INT32_MAX
#define INT_LEAST64_MAX  __INT64_C(9223372036854775807)
#define UINT_LEAST8_MAX  UINT8_MAX
#define UINT_LEAST16_MAX UINT16_MAX
#define UINT_LEAST32_MAX UINT32_MAX
#define UINT_LEAST64_MAX __UINT64_C(18446744073709551615)

#define INTPTR_MIN      INT32_MIN
#define INTPTR_MAX      INT32_MAX
#define UINTPTR_MAX     UINT32_MAX

#define INTMAX_MIN      (-__INT64_C(9223372036854775807) - 1)
#define INTMAX_MAX      __INT64_C(9223372036854775807)
#define UINTMAX_MAX     __UINT64_C(18446744073709551615)

#define PTRDIFF_MAX     INT32_MAX
#define PTRDIFF_MIN     INT32_MIN
#define SIZE_MAX        UINT32_MAX

#define SIG_ATOMIC_MIN  INT32_MIN
#define SIG_ATOMIC_MAX  INT32_MAX
#define WCHAR_MIN       INT32_MIN
#define WCHAR_MAX       INT32_MAX
#define WINT_MIN        INT32_MIN
#define WINT_MAX        INT32_MAX

#define INT8_C(c)   c
#define INT16_C(c)  c
#define INT32_C(c)  c
#define UINT8_C(c)  c
#define UINT16_C(c) c
#define UINT32_C(c) c ## U

#endif
