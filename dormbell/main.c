#define DEBUG
#ifdef GCC
#include <io.h>
#include <signal.h>
#else
#include "msp430g2231.h"
#endif

// PINS  ===========================================
#define     LED0                  BIT0
#define     LED1                  BIT6
#define     LED_DIR               P1DIR
#define     LED_OUT               P1OUT

#define			PWM					  				BIT6
#define			PWM_SEL				  			P1SEL
#define			PWM_DIR				  			P1DIR
#define			PWM_OUT								P1OUT

#define     BUTTON                BIT3
#define     BUTTON_OUT            P1OUT
#define     BUTTON_DIR            P1DIR
#define     BUTTON_IN             P1IN
#define     BUTTON_IE             P1IE
#define     BUTTON_IES            P1IES
#define     BUTTON_IFG            P1IFG
#define     BUTTON_REN            P1REN

// FLASH SEGMENTS  ===========================================
#define SEGMENT_A (0x10FF)
#define SEGMENT_B (0x10FF - 64)
#define SEGMENT_C (0x10FF - 128)
#define SEGMENT_D (0x10FF - 192)
#define INFOMEM		(0x1000)

// TONES  ==========================================
// Start by defining the relationship between 
//       note, period, and frequency.
// This is calculated by finding the number of SMCLK
// 		 ticks (1,048,576 Hz) needed to make up twice the
//       frequency of each tone. (Ex: for A, which is
//       440 Hz, find the # of SMCLK ticks needed to
//       create a frequency of 880 Hz = ~1192. 
// D major scale: R,D,E,F#,G,A,B,C#,D (294 Hz - 587 Hz)
unsigned const int notes[] = {
	0, 56, 50, 44, 42, 37, 33, 30, 28
};


// MELODY and TIMING  =======================================
//  melody[] is an array of notes, accompanied by beats[], 
//  which sets each note's relative length (higher #, longer note) 
unsigned char *melody;
unsigned char *beats;
unsigned char length;  // Melody length, for looping.

//tempo in SMCLK ticks per beat
unsigned int tempo; 
// Set length of pause between notes
unsigned char pause; // ~1 ms

// excuse the strong language, but fuck you GCC optimizations
// the missing volatile messed with my head for an hour
volatile unsigned int tone = 0; 			// current tone
volatile unsigned int duration = 0;		// current duration

// OTHER DECLARATIONS  =======================================

void init_leds(void);
void init_button(void);
void init_clocks(void);
void init_pwm(void);
void init_consts(void);

void play_song(void);
void play_tone(void);

// CODE  =====================================================

int main(void)
{
	WDTCTL = WDTPW + WDTHOLD;               // Stop WDT

	init_clocks();
	init_button();
#ifdef DEBUG
	init_leds();
#endif
	init_pwm();
	init_consts();

	//Checksum...sorta
	if (tempo != 11633 || pause != 33 || length != 50) {
		PWM_SEL &= ~(PWM);
		LED_OUT |= LED0 + LED1;
	}
	__bis_SR_register(LPM1_bits + GIE);     // LPM1 with interrupts enabled

	while(1);
	return 0;
}

void init_clocks(void)
{
	BCSCTL1 = CALBC1_1MHZ;	// Set range for DCO
	DCOCTL = CALDCO_1MHZ;		// Calibrate DCO
	BCSCTL2 |= DIVS_3;			// SMCLK = MCLK/8 = 131,072 Hz
}

void init_button(void)		// Configure Push Button 
{
	BUTTON_DIR &= ~BUTTON;	// change to input
	BUTTON_OUT |= BUTTON;		// output is HIGH
	BUTTON_REN |= BUTTON;		// enable pullup resistor
	BUTTON_IES |= BUTTON;		// interrupt on falling edge
	BUTTON_IFG &= ~BUTTON;	// clear interrupt flag
	BUTTON_IE |= BUTTON;		// enable interrupt
}

void init_leds(void)
{
	LED_DIR |= LED0;                          
	LED_OUT &= ~(LED0);  
}

void init_pwm(void)
{
	TACTL |= TASSEL_2 + ID_2;		// SMCLK/4 source, continuous mode
	TACCTL1 = OUTMOD_2 + CCIE;	// toggle/reset for TACCR1 and interrupt enable
	TACCTL0 |= CCIE;						// enable interrupt for TACCR0
								
	PWM_SEL |= PWM;				// set P1.6 to PWM output (primary pin function)
	PWM_DIR |= PWM;
}

void init_consts(void)
{
	//	Example: Happy Birthday!
	// { g, g, a, g, c, b, R, g, g, a, g, d, c, R }
	melody = (unsigned char *)(INFOMEM + 2);
	// { 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 1 } 
	beats = (unsigned char *)(INFOMEM + 192/2 + 2);
	length = *(unsigned char *)(INFOMEM);  
	pause = *(unsigned char *)(INFOMEM + 1); // 33, ~1 ms
	tempo = *(unsigned int *)(INFOMEM + 192/2); // ~169 BPM, which sounded about right for Happy Birthday
}
	
void play_song(void)
{
	// Set up a counter to pull from melody[] and beats[]
	unsigned char i;

	__enable_interrupt();			// enable nested interrupts (since called from one)
#ifdef DEBUG
	LED_OUT |= LED0;		// turn on red LED
#endif

	for (i = 0; i < length; i++) {
		tone = notes[melody[i]];
		duration = beats[i] * tempo;
		
		play_tone(); 
		// A pause between notes...
		tone = 0;
		duration = pause;
		play_tone();
	}
#ifdef DEBUG
	LED_OUT &= ~(LED0);			// turn off red LED
#endif
}

void play_tone(void)
{
	if (tone > 0) {				// if this isn't a Rest beat
		PWM_SEL |= PWM;
		PWM_DIR |= PWM;
		TACCR1 = tone;			
	}
	else {								// switch to GPIO input mode, no voltage for you!
		PWM_SEL &= ~(PWM);
		PWM_DIR &= ~(PWM);
	}
	TACCR0 = duration;
	TACTL |= MC_2 + ID_2;	// SMCLK/4, continuous mode, should start timer
	while(TACTL & MC_2);	// wait for timer to stop (tone is done playing)
}

#ifdef GCC
interrupt(PORT1_VECTOR) PORT1_ISR(void)
#else
#pragma vector=PORT1_VECTOR
__interrupt void PORT1_ISR(void)
#endif
{   
	BUTTON_IFG = 0;  				// clear interrupt flag
	BUTTON_IE &= ~BUTTON; 	//  Debounce (no multiple presses)
	play_song();
	BUTTON_IE |= BUTTON;		// reenable interrupt
}

#ifdef GCC
interrupt(TIMERA0_VECTOR) TACCR0_ISR (void)
#else
#pragma vector=TIMERA0_VECTOR
interrupt void TACCR0_ISR(void)
#endif
{
	TACTL &= ~(MC_2);			// stop timer
	TACTL |= TACLR;				// clear timer
}

#ifdef GCC
interrupt(TIMERA1_VECTOR) TACCR1_ISR (void)
#else
#pragma vector=TIMERA1_VECTOR
interrupt void TACCR1_ISR(void)
#endif
{
  TACCTL1 &= ~CCIFG;	// clear interrupt flag
  TACCR1 += tone;			// update for next compare match
}
