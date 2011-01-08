#include <io.h>

// PINS  ===========================================
#define     LED0                  BIT0
#define     LED1                  BIT6
#define     LED_DIR               P1DIR
#define     LED_OUT               P1OUT

#define			PWM					  				BIT6
#define			PWM_SEL				  			P1SEL
#define			PWM_DIR				  			P1DIR
#define			PWM_OUT								P1OUT

#ifdef BUTTON
#define     P_BUTTON                BIT3
#define     P_BUTTON_OUT            P1OUT
#define     P_BUTTON_DIR            P1DIR
#define     P_BUTTON_IN             P1IN
#define     P_BUTTON_IE             P1IE
#define     P_BUTTON_IES            P1IES
#define     P_BUTTON_IFG            P1IFG
#define     P_BUTTON_REN            P1REN
#else
#define			RXD											BIT3
#endif

#define     C_BUTTON                BIT4
#define     C_BUTTON_OUT            P1OUT
#define     C_BUTTON_DIR            P1DIR
#define     C_BUTTON_IN             P1IN
#define     C_BUTTON_IE             P1IE
#define     C_BUTTON_IES            P1IES
#define     C_BUTTON_IFG            P1IFG
#define     C_BUTTON_REN            P1REN
