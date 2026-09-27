/********************************** (C) COPYRIGHT *******************************
 * File Name          : cc_power.c
 * Description        : Type-C CC management (J1 3A detect, J2 source attach).
 *********************************************************************************/
#include "ch32x035.h"
#include <ch32x035_usbpd.h>
#include "cc_power.h"
#include "delay.h"
#include "board.h"

/******************************************************************************/
/* J1 charger input: 3A Rp detection (ADC on PA2/PA4) */

/* Rp=10k charger with our Rd=5.1k gives vRd ~= 1.69V (spec 1.68-2.04V).
 * Rp=22k (1.5A) gives ~0.94V, Rp=56k (default USB) gives ~0.41V. */
#define CC_3A_MIN_MV 1450
#define CC_3A_MAX_MV 2250

/* J2 phone port: Rp=180uA with phone Rd=5.1k gives vCC ~= 0.92V, which sits
 * between the 0.66V and 1.23V PHY comparator thresholds (Rd window).
 * Ra (passive cable, ~1k) gives ~0.18V, open line gives VDD: both outside. */

static void adc_init(void)
{
    ADC_InitTypeDef adc = {0};

    RCC_APB2PeriphClockCmd(RCC_APB2Periph_ADC1, ENABLE);

    ADC_DeInit(ADC1);
    ADC_CLKConfig(ADC1, ADC_CLK_Div6);

    adc.ADC_Mode               = ADC_Mode_Independent;
    adc.ADC_ScanConvMode       = DISABLE;
    adc.ADC_ContinuousConvMode = DISABLE;
    adc.ADC_ExternalTrigConv   = ADC_ExternalTrigConv_None;
    adc.ADC_DataAlign          = ADC_DataAlign_Right;
    adc.ADC_NbrOfChannel       = 1;
    ADC_Init(ADC1, &adc);

    ADC_Cmd(ADC1, ENABLE);
    Delay_Ms(1); /* ADC stabilisation time */
}

static uint16_t adc_read_mv(uint8_t channel)
{
    uint16_t i;
    uint32_t acc = 0;

    ADC_RegularChannelConfig(ADC1, channel, 1, ADC_SampleTime_11Cycles);

    for (i = 0; i < 4; i++)
    {
        ADC_ClearFlag(ADC1, ADC_FLAG_EOC);
        ADC_SoftwareStartConvCmd(ADC1, ENABLE);
        while (ADC_GetFlagStatus(ADC1, ADC_FLAG_EOC) == RESET)
        {
        }
        acc += ADC_GetConversionValue(ADC1);
    }

    /* 12-bit ADC, VREF = VDD = 5V */
    return (uint16_t)((acc * 5000UL) / (4UL * 4096UL));
}

static uint8_t input_3a_raw(void)
{
    uint16_t mv;

    mv = adc_read_mv(ADC_Channel_2); /* PA2 = J1 CC1 */
    if ((mv >= CC_3A_MIN_MV) && (mv <= CC_3A_MAX_MV))
    {
        return 1;
    }

    mv = adc_read_mv(ADC_Channel_4); /* PA4 = J1 CC2 */
    if ((mv >= CC_3A_MIN_MV) && (mv <= CC_3A_MAX_MV))
    {
        return 1;
    }

    return 0;
}

/******************************************************************************/
/* J2 phone port: USBPD PHY based source Rp / sink attach detection */

static void cc_gpio_mode(uint8_t floating)
{
    GPIO_InitTypeDef gpio = {0};

    RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOC, ENABLE);

    gpio.GPIO_Pin   = GPIO_Pin_14 | GPIO_Pin_15;
    gpio.GPIO_Speed = GPIO_Speed_50MHz;
    gpio.GPIO_Mode  = floating ? GPIO_Mode_IN_FLOATING : GPIO_Mode_IPD;
    GPIO_Init(GPIOC, &gpio);
}

static uint8_t cc_cmp_above(uint8_t line, uint16_t cmp_bits)
{
    volatile uint16_t *port;

    port = (line == 1) ? &USBPD->PORT_CC1 : &USBPD->PORT_CC2;
    *port &= (uint16_t)~(CC_CMP_Mask | PA_CC_AI);
    *port |= cmp_bits;
    Delay_Us(2);
    return ((*port & PA_CC_AI) != 0) ? 1 : 0;
}

/* 1 when the CC line voltage sits in the Rd window (0.66V..1.23V) */
static uint8_t cc_line_in_rd_window(uint8_t line)
{
    uint8_t above_066 = cc_cmp_above(line, CC_CMP_66);
    uint8_t above_123 = cc_cmp_above(line, CC_CMP_123);

    return (above_066 && !above_123) ? 1 : 0;
}

static uint8_t sink_attached_raw(void)
{
    uint8_t cc1 = cc_line_in_rd_window(1);
    uint8_t cc2 = cc_line_in_rd_window(2);

    return (cc1 || cc2) ? 1 : 0;
}

/******************************************************************************/
/* Public API */

void cc_Init(void)
{
    adc_init();
    cc_gpio_mode(0); /* Rp off: CC lines weakly pulled down */
}

uint8_t cc_Input3A_Present(void)
{
    static uint8_t stable = 0;
    static uint8_t cnt = 0;
    uint8_t now = input_3a_raw();

    if (now != stable)
    {
        if (++cnt >= 3)
        {
            stable = now;
            cnt = 0;
        }
    }
    else
    {
        cnt = 0;
    }
    return stable;
}

void cc_SourceRp_Enable(uint8_t enable)
{
    if (enable)
    {
        RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO, ENABLE);
        RCC_AHBPeriphClockCmd(RCC_AHBPeriph_USBPD, ENABLE);

        cc_gpio_mode(1); /* CC lines to the USBPD PHY */

        AFIO->CTLR |= USBPD_IN_HVT | USBPD_PHY_V33;
        USBPD->CONFIG = 0;
        USBPD->PORT_CC1 = CC_PU_180; /* 180uA Rp: 1.5A source advertisement */
        USBPD->PORT_CC2 = CC_PU_180;
    }
    else
    {
        USBPD->PORT_CC1 = 0;
        USBPD->PORT_CC2 = 0;
        cc_gpio_mode(0); /* back to weak pull-down, no advertisement */
    }
}

uint8_t cc_SinkAttached(void)
{
    static uint8_t stable = 0;
    static uint8_t cnt = 0;
    uint8_t now = sink_attached_raw();

    if (now != stable)
    {
        if (++cnt >= 5)
        {
            stable = now;
            cnt = 0;
            board_Log(stable ? "CC: sink attached" : "CC: sink detached");
        }
    }
    else
    {
        cnt = 0;
    }
    return stable;
}
