/********************************** (C) COPYRIGHT *******************************
 * File Name          : board.c
 * Description        : FocusPod board level IO (see board.h pin map).
 *********************************************************************************/
#include "ch32x035.h"
#include "board.h"
#include "time.h"

/* Debug UART on PB10. Keep enabled by default: the pad is unpopulated on the
 * production PCB, the code is tiny and helps bring-up on the bench. */
#define BOARD_DEBUG_UART 1

static void debug_uart_init(void)
{
#if BOARD_DEBUG_UART
    GPIO_InitTypeDef  gpio = {0};
    USART_InitTypeDef usart = {0};

    RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB | RCC_APB2Periph_USART1, ENABLE);

    gpio.GPIO_Pin   = GPIO_Pin_10;
    gpio.GPIO_Speed = GPIO_Speed_50MHz;
    gpio.GPIO_Mode  = GPIO_Mode_AF_PP;
    GPIO_Init(GPIOB, &gpio);

    usart.USART_BaudRate            = 115200;
    usart.USART_WordLength          = USART_WordLength_8b;
    usart.USART_StopBits            = USART_StopBits_1;
    usart.USART_Parity              = USART_Parity_No;
    usart.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
    usart.USART_Mode                = USART_Mode_Tx;
    USART_Init(USART1, &usart);
    USART_Cmd(USART1, ENABLE);
#endif
}

static void debug_uart_putc(char c)
{
#if BOARD_DEBUG_UART
    while (USART_GetFlagStatus(USART1, USART_FLAG_TC) == RESET)
    {
    }
    USART_SendData(USART1, (uint16_t)(uint8_t)c);
#else
    (void)c;
#endif
}

void board_Log(const char *s)
{
#if BOARD_DEBUG_UART
    while (*s)
    {
        debug_uart_putc(*s++);
    }
    debug_uart_putc('\r');
    debug_uart_putc('\n');
#else
    (void)s;
#endif
}

void board_LogHex(const char *tag, uint32_t value)
{
#if BOARD_DEBUG_UART
    static const char hex[] = "0123456789ABCDEF";
    int i;

    board_Log(tag);
    debug_uart_putc('0');
    debug_uart_putc('x');
    for (i = 28; i >= 0; i -= 4)
    {
        debug_uart_putc(hex[(value >> i) & 0xF]);
    }
    debug_uart_putc('\r');
    debug_uart_putc('\n');
#else
    (void)tag;
    (void)value;
#endif
}

void board_Init(void)
{
    GPIO_InitTypeDef gpio = {0};

    RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA | RCC_APB2Periph_GPIOB |
                           RCC_APB2Periph_GPIOC | RCC_APB2Periph_AFIO, ENABLE);

    /* PA0: dry-contact input with internal pull-down */
    gpio.GPIO_Pin  = GPIO_Pin_0;
    gpio.GPIO_Mode = GPIO_Mode_IPD;
    GPIO_Init(GPIOA, &gpio);

    /* PA1: LED1, push-pull output, default LOW = off. */
    gpio.GPIO_Pin   = GPIO_Pin_1;
    gpio.GPIO_Speed = GPIO_Speed_50MHz;
    gpio.GPIO_Mode  = GPIO_Mode_Out_PP;
    GPIO_Init(GPIOA, &gpio);
    GPIO_ResetBits(GPIOA, GPIO_Pin_1);

    /* PA5: U2 EN, push-pull output, default LOW = phone power off.
     * R4 (5.1k pull-down) also keeps it off during reset. */
    gpio.GPIO_Pin   = GPIO_Pin_5;
    gpio.GPIO_Speed = GPIO_Speed_50MHz;
    gpio.GPIO_Mode  = GPIO_Mode_Out_PP;
    GPIO_Init(GPIOA, &gpio);
    GPIO_ResetBits(GPIOA, GPIO_Pin_5);

    /* PA2 / PA4: J1 CC1 / CC2, analog inputs (ADC channels 2 / 4) */
    gpio.GPIO_Pin  = GPIO_Pin_2 | GPIO_Pin_4;
    gpio.GPIO_Mode = GPIO_Mode_AIN;
    GPIO_Init(GPIOA, &gpio);

    /* PC14 / PC15: J2 CC lines, weak pull-down while the source Rp is off */
    gpio.GPIO_Pin  = GPIO_Pin_14 | GPIO_Pin_15;
    gpio.GPIO_Mode = GPIO_Mode_IPD;
    GPIO_Init(GPIOC, &gpio);

    /* PC10 / PC11 stay floating inputs (USB PHY 5V configuration requirement) */
    gpio.GPIO_Pin  = GPIO_Pin_10 | GPIO_Pin_11;
    gpio.GPIO_Mode = GPIO_Mode_IN_FLOATING;
    GPIO_Init(GPIOC, &gpio);

    debug_uart_init();

    board_Log("FocusPod CH32X035 init");
}

uint8_t board_In_Read(void)
{
    return (GPIO_ReadInputDataBit(GPIOA, GPIO_Pin_0) == Bit_SET) ? 1 : 0;
}

void board_Led_Set(uint8_t on)
{
    if (on)
    {
        GPIO_SetBits(GPIOA, GPIO_Pin_1); /* LED1 lit   */
    }
    else
    {
        GPIO_ResetBits(GPIOA, GPIO_Pin_1); /* LED1 off */
    }
}

void board_Led_Toggle(void)
{
    GPIO_WriteBit(GPIOA, GPIO_Pin_1,
                  (GPIO_ReadOutputDataBit(GPIOA, GPIO_Pin_1) == Bit_SET) ? Bit_RESET : Bit_SET);
}

void board_Power_Enable(uint8_t on)
{
    if (on)
    {
        GPIO_SetBits(GPIOA, GPIO_Pin_5);
    }
    else
    {
        GPIO_ResetBits(GPIOA, GPIO_Pin_5);
    }
}

uint8_t board_Power_Get(void)
{
    return (GPIO_ReadOutputDataBit(GPIOA, GPIO_Pin_5) == Bit_SET) ? 1 : 0;
}
