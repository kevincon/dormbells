/*
 * Writes song data to MSP430 information memory (0x10FF - 0x1000)
 * Since Segment A cannot be overwritten, there are 192B available
 * tempo (int), pause & length (chars) are also stored in info mem
 * that leaves room for 94 notes to be stored.
 */

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

#define			RXD										BIT2
// DEFINES  ========================================
#define SEGMENT_A (0x10FF)
#define SEGMENT_B (0x10FF - 64)
#define SEGMENT_C (0x10FF - 128)
#define SEGMENT_D (0x10FF - 192)
#define INFOMEM		(0x1000)

#define BUF_SIZE	96
#define LENGTH		(*(unsigned char *)(INFOMEM))	// where length is written

#define BAUD_RATE 2400
#define BIT_PER		417		// 1,000,000 / BAUD_RATE = us delay
#define BIT_PER3	139		// BIT_PER / 3
#define BIT_DEL		400 	// _bitPeriod - 17 nee clockCyclesToMicroseconds(50);
#define BIT_DEL3	133 	// BIT_DEL / 3
#define MAGIC			60		// delayMicroseconds(bitDelay / 2 - 20 nee clockCyclesToMicroseconds(50)) / 3

// GLOBALS  ========================================
volatile unsigned char buffer[BUF_SIZE];
volatile unsigned char i;

// FUNCTION PROTOTYPES =============================

void init_leds(void);
void init_clocks(void);
void init_serial(void);

void erase_seg(char *);
void write_byte(unsigned char, unsigned char *);
void write_word(unsigned int, unsigned int *);

static void __inline__ delay(register unsigned int n);
unsigned char read(void);

// CODE  ===========================================

int main(void)
{
	unsigned char *ptr;

	WDTCTL = WDTPW + WDTHOLD;               // Stop WDT
	init_clocks();
	init_leds();
	init_serial();

	LED_OUT |= LED0;

	// erase information memory segments
	erase_seg((char *)SEGMENT_B);
	erase_seg((char *)SEGMENT_C);
	erase_seg((char *)SEGMENT_D);

	/*
	for (i = 0; i < BUF_SIZE; i++)
		buffer[i] = read();
		*/
	i = 0;
	__enable_interrupt();
	while (!buffer[0]);
	while (i < buffer[0]+2);
	__disable_interrupt();

	// write constants to information memory (bottom of Segment D)
	ptr = (unsigned char *)INFOMEM;
	write_byte(buffer[0], ptr++);
	write_byte(buffer[1], ptr++);

	// write melody 
	for (i = 0; i < LENGTH; i++)
		write_byte(buffer[i+2], ptr++);

	/*
	for (i = 0; i < BUF_SIZE; i++)
		buffer[i] = read();
		*/

	i = 0;
	__enable_interrupt();
	while (i < LENGTH + 2);
	__disable_interrupt();

	// write constant to middle of available info mem
	ptr = (unsigned char *)(INFOMEM + 192 / 2);
	write_word((unsigned int)buffer[0], (unsigned int *)ptr);
	ptr += 2;

	// write beats
	for (i = 0; i < LENGTH; i++)
		write_byte(buffer[i+2], ptr++);

	LED_OUT &= ~(LED0);
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

void init_serial()
{
	P1DIR &= ~(RXD);	// change to input
	P1IES |= RXD;			// interrupt on falling edge
	P1IFG &= ~(RXD);	// clear interrupt flag
	P1IE |= RXD;			// enable interrupt
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
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + WRT;			// enable write
	*ptr = data;							// write data
	FCTL1 = FWKEY;						// clear WRITE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
}

void write_word(unsigned int data, unsigned int *ptr)
{
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + WRT;			// enable write
	*ptr = data;							// write data
	FCTL1 = FWKEY;						// clear WRITE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
}

// Delay Routine from mspgcc help file
// I think this delays n/3 us -Varun
static void __inline__ delay(register unsigned int n)
{
	__asm__ __volatile__ (
			"1: \n"
			" dec %[n] \n"
			" jne 1b \n"
			: [n] "+r"(n));
}

// code ported/borrowed from David Mellis's SoftwareSerial for Arduino
unsigned char read()
{
	unsigned char val = 0;

	//while (P1IN & RXD);

	// confirm that this is a real start bit, not line noise
	if (!(P1IN & RXD)) {
		unsigned char offset;
		// frame start indicated by a falling edge and low start bit
		// jump to the middle of the low start bit
		delay(MAGIC);

		// offset of the bit in the byte: from 0 (LSB) to 7 (MSB)
		for (offset = 0; offset < 8; offset++) {
			// jump to middle of next bit
			delay(BIT_DEL3);

			// read bit
			val |= ((P1IN & RXD) > 0) << offset;
		}

		delay(BIT_PER3);

		return val;
	}

	return 0xFF;
}

#ifdef GCC
interrupt(PORT1_VECTOR) PORT1_ISR(void)
#else
#pragma vector=PORT1_VECTOR
__interrupt void PORT1_ISR(void)
#endif
{
	LED_OUT |= LED1;
	P1IE &= ~(RXD);				// disable interrupt
	P1IFG &= ~(RXD);			// clear interrupt flag
	buffer[i++] = read();	// read data
	P1IE |= RXD;					// enable interrupt
	LED_OUT &= ~(LED1);
}
