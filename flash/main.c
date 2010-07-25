/*
 * Writes song data to MSP430 information memory (0x10FF - 0x1000)
 * Since Segment A cannot be overwritten, there are 192B available
 * tempo (int), pause & length (chars) are also stored in info mem
 * that leaves room for 94 notes to be stored.
 */
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

// TONES  ==========================================
// Start by defining the relationship between 
//       note, period, and frequency.
// This is calculated by finding the number of SMCLK
// 		 ticks (32,768 Hz) needed to make up twice the
//       frequency of each tone. (Ex: for A, which is
//       440 Hz, find the # of SMCLK ticks needed to
//       create a frequency of 880 Hz = ~37. 
#define  c     63		// 261 Hz 
#define  d     56		// 294 Hz 
#define  e     50   // 329 Hz 
#define  f     47   // 349 Hz 
#define  g     42   // 392 Hz 
#define  a     37   // 440 Hz 
#define  b     33   // 493 Hz 
// Define a special note, 'R', to represent a rest
#define  R     0

// MELODY and TIMING  =======================================
//  melody[] is an array of notes, accompanied by beats[], 
//  which sets each note's relative length (higher #, longer note) 
//	Example: Happy Birthday!
char melody[] = { g, g, a, g, c, b, R, g, g, a, g, d, c, R }; //, g, g, g, e, c, b, a, R, f, f, e, c, d, c };
char beats[]  = { 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 1 }; //, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2 }; 
char length = sizeof(melody); // Melody length, for looping.

//tempo in SMCLK ticks per beat
unsigned int tempo = 11633; // ~169 BPM, which sounded about right for Happy Birthday
// Set length of pause between notes
char pause = 33; // ~1 ms

// FLASH SEGMENTS  ===========================================
#define SEGMENT_A (0x10FF)
#define SEGMENT_B (0x10FF - 64)
#define SEGMENT_C (0x10FF - 128)
#define SEGMENT_D (0x10FF - 192)
#define INFOMEM		(0x1000)

// FUNCTION DECLARATIONS  ====================================

void init_leds(void);
void init_clocks(void);

void erase_seg(char *);
void write_byte(unsigned char, unsigned char *);
void write_word(unsigned int, unsigned int *);
// CODE  =====================================================

int main(void)
{
	unsigned char *ptr;
	unsigned char i;

	WDTCTL = WDTPW + WDTHOLD;               // Stop WDT
	init_clocks();
#ifdef DEBUG
	init_leds();
#endif

#ifdef DEBUG
	LED_OUT |= LED0;
#endif
	// erase information memory segments
	erase_seg((char *)SEGMENT_B);
	erase_seg((char *)SEGMENT_C);
	erase_seg((char *)SEGMENT_D);

	// write constants to information memory (bottom of Segment D)
	ptr = (unsigned char *)INFOMEM;
	write_byte(length, ptr++);
	write_byte(pause, ptr++);

	// write melody 
	for (i = 0; i < length; i++)
		write_byte(melody[i], ptr++);

	// write constant to middle of available info mem
	ptr = (unsigned char *)(INFOMEM + 192 / 2);
	write_word(tempo, (unsigned int *)ptr);
	ptr += 2;

	// write beats
	for (i = 0; i < length; i++)
		write_byte(beats[i], ptr++);

#ifdef DEBUG
	LED_OUT &= ~(LED0);
#endif
	while(1);
	return 0;
}

void init_clocks(void)
{
	BCSCTL1 = CALBC1_1MHZ;	// Set range for DCO
	DCOCTL = CALDCO_1MHZ;		// Calibrate DCO
	FCTL2 = FWKEY + FSSEL_1 + FN1 + FN0;	// use MCLK/4 for flash clock generator
}

void init_leds(void)
{
	LED_DIR |= LED0 + LED1;                          
	LED_OUT &= ~(LED0 + LED1);  
}

void erase_seg(char *ptr)
{
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + ERASE;		// enable segment erase
	*ptr = 0;									// dummy write, erase segment
	FCTL1 = FWKEY;						// clear ERASE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
}

void write_byte(unsigned char data, unsigned char *ptr)
{
#ifdef DEBUG
	LED_OUT |= LED1;
#endif
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + WRT;			// enable write
	*ptr = data;							// write data
	FCTL1 = FWKEY;						// clear WRITE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
#ifdef DEBUG
	LED_OUT &= ~(LED1);
#endif
}

void write_word(unsigned int data, unsigned int *ptr)
{
#ifdef DEBUG
	LED_OUT |= LED1;
#endif
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + WRT;			// enable write
	*ptr = data;							// write data
	FCTL1 = FWKEY;						// clear WRITE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
#ifdef DEBUG
	LED_OUT &= ~(LED1);
#endif
}
