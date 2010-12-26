/* DormBell
 * Copyright (C) 2010 DormBells
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#define DEBUG


#include <io.h>
#include <signal.h>

// PINS  ===========================================
#define     LED0                  BIT0
#define     LED1                  BIT6
#define     LED_DIR               P1DIR
#define     LED_OUT               P1OUT

#define			PWM					  				BIT6
#define			PWM_SEL				  			P1SEL
#define			PWM_DIR				  			P1DIR
#define			PWM_OUT								P1OUT

#define     P_BUTTON                BIT3
#define     P_BUTTON_OUT            P1OUT
#define     P_BUTTON_DIR            P1DIR
#define     P_BUTTON_IN             P1IN
#define     P_BUTTON_IE             P1IE
#define     P_BUTTON_IES            P1IES
#define     P_BUTTON_IFG            P1IFG
#define     P_BUTTON_REN            P1REN

#define     C_BUTTON                BIT4
#define     C_BUTTON_OUT            P1OUT
#define     C_BUTTON_DIR            P1DIR
#define     C_BUTTON_IN             P1IN
#define     C_BUTTON_IE             P1IE
#define     C_BUTTON_IES            P1IES
#define     C_BUTTON_IFG            P1IFG
#define     C_BUTTON_REN            P1REN

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
// 		 ticks (32,768 Hz) needed to make up twice the
//       frequency of each tone. (Ex: for A, which is
//       440 Hz, find the # of SMCLK ticks needed to
//       create a frequency of 880 Hz = ~37. 
// D major scale: R,D,E,F#,G,A,B,C#,D (294 Hz - 587 Hz)

// MELODY and TIMING  =======================================
//  melody[] is an array of notes, accompanied by beats[], 
//  which sets each note's relative length (higher #, longer note) 
volatile unsigned char *song;
volatile unsigned char length;

//tempo in SMCLK ticks per beat
volatile unsigned int tempo; 
// Set length of pause between notes
volatile unsigned char pause; // ~1 ms

// excuse the strong language, but fuck you GCC optimizations
// the missing volatile messed with my head for an hour
volatile unsigned int tone = 0; 			// current tone
volatile unsigned int duration = 0;		// current duration

// FUNCTION PROTOTYPES  ======================================

void init_leds(void);
void init_buttons(void);
void init_clocks(void);
void init_pwm(void);
void init_consts(void);

static void __inline__ delay(register unsigned int n);
void change_consts(void);
void play_song(void);
void play_tone(void);

// CODE  =====================================================

int main(void)
{
	WDTCTL = WDTPW + WDTHOLD;               // Stop WDT

	init_clocks();
	init_buttons();
#ifdef DEBUG
	init_leds();
#endif
	init_pwm();
	init_consts();

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

void init_buttons(void)		// configure push buttons
{
	P_BUTTON_DIR &= ~P_BUTTON;	// change to input
	P_BUTTON_OUT |= P_BUTTON;		// output is HIGH
	P_BUTTON_REN |= P_BUTTON;		// enable pullup resistor
	P_BUTTON_IES |= P_BUTTON;		// interrupt on falling edge
	P_BUTTON_IFG &= ~P_BUTTON;	// clear interrupt flag
	P_BUTTON_IE |= P_BUTTON;		// enable interrupt

	C_BUTTON_DIR &= ~C_BUTTON;	// change to input
	C_BUTTON_OUT |= C_BUTTON;		// output is HIGH
	C_BUTTON_REN |= C_BUTTON;		// enable pullup resistor
	C_BUTTON_IES |= C_BUTTON;		// interrupt on falling edge
	C_BUTTON_IFG &= ~C_BUTTON;	// clear interrupt flag
	C_BUTTON_IE |= C_BUTTON;		// enable interrupt
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
	song = (unsigned char *)(INFOMEM + 4);
	length = *(unsigned char *)(INFOMEM);  
	pause = *(unsigned char *)(INFOMEM + 1);
	tempo = *(unsigned int *)(INFOMEM + 2);
}

void change_consts(void)
{
	if ((*(song + (length << 1)) == 0xFF) || 
			(*(song + (length << 1)) >= (INFOMEM+192))) {
		// reset if we've cycled through all
		init_consts();
	}
	else {
		// update consts for next song
		song += (length << 1) + 4;	// 4 for length, pause, tempo
		length = *(song - 4);
		pause = *(song - 3);
		tempo = *(unsigned int *)(song - 2);
	}
}
	
void play_song(void)
{
	unsigned char i;

	__enable_interrupt();			// enable nested interrupts (since called from one)
#ifdef DEBUG
	LED_OUT |= LED0;		// turn on red LED
#endif

	// loop through all notes in the song
	for (i = 0; i < (length << 1); i += 2) {
		tone = song[i];	// tone is first byte
		duration = song[i+1] * tempo; // beats is second byte
		
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

// Delay Routine from mspgcc help file
// Takes 3 clock cycles to execute 1 iteration
static void __inline__ delay(register unsigned int n)
{
	__asm__ __volatile__ (
			"1: \n"
			" dec %[n] \n"
			" jne 1b \n"
			: [n] "+r"(n));
}

interrupt(PORT1_VECTOR) PORT1_ISR(void)
{   
	// 1MHz clock with 3 clock cycles per iteration
	// means 4000 = 12ms
	// this is a terrible debounce idea, but 
	// hopefully it works
	delay(4000);
	P_BUTTON_IE &= ~P_BUTTON; 	// no multiple presses
	C_BUTTON_IE &= ~C_BUTTON;		// no interleaving

	if (!(P_BUTTON_IN & P_BUTTON)) {	// indicates playback
		P_BUTTON_IFG = 0;  				// clear interrupt flag
		play_song();
	}
	else {
		C_BUTTON_IFG = 0;
		change_consts();
	}

	P_BUTTON_IE |= P_BUTTON;		// reenable interrupt
	C_BUTTON_IE |= C_BUTTON;		// reenable interrupt
}

interrupt(TIMERA0_VECTOR) TACCR0_ISR (void)
{
	TACTL &= ~(MC_2);			// stop timer
	TACTL |= TACLR;				// clear timer
}

interrupt(TIMERA1_VECTOR) TACCR1_ISR (void)
{
  TACCTL1 &= ~CCIFG;	// clear interrupt flag
  TACCR1 += tone;			// update for next compare match
}
