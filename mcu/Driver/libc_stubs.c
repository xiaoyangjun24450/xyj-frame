/********************************** (C) COPYRIGHT *******************************
 * File Name          : libc_stubs.c
 * Description        : Minimal libc implementations for the freestanding build.
 *********************************************************************************/
#include <stddef.h>

void *memcpy(void *dest, const void *src, size_t n)
{
    unsigned char *d = (unsigned char *)dest;
    const unsigned char *s = (const unsigned char *)src;
    while (n--)
    {
        *d++ = *s++;
    }
    return dest;
}

void *memset(void *s, int c, size_t n)
{
    unsigned char *p = (unsigned char *)s;
    while (n--)
    {
        *p++ = (unsigned char)c;
    }
    return s;
}

size_t strlen(const char *s)
{
    const char *p = s;
    while (*p)
    {
        p++;
    }
    return (size_t)(p - s);
}

int memcmp(const void *a, const void *b, size_t n)
{
    const unsigned char *pa = (const unsigned char *)a;
    const unsigned char *pb = (const unsigned char *)b;
    while (n--)
    {
        if (*pa != *pb)
        {
            return (int)*pa - (int)*pb;
        }
        pa++;
        pb++;
    }
    return 0;
}
