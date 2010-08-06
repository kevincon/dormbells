/*
 * Writes song data to MSP430 information memory (0x10FF - 0x1000)
 * Since Segment A cannot be overwritten, there are 192B available
 * tempo (int), pause & length (chars) are also stored in info mem
 * that leaves room for 94 notes to be stored.
 * 
 * Serial data taken via software UART at 2400 baud. Right now it
 * seems to run just a tad too quickly, so error bytes 0xFF are
 * skipped over as a safety measure.  No problems when testing with
 * the Can-Can.
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
#define SAFETY

#define SEGMENT_A (0x10FF)
#define SEGMENT_B (0x10FF - 64)
#define SEGMENT_C (0x10FF - 128)
#define SEGMENT_D (0x10FF - 192)
#define INFOMEM		(0x1000)

#define BUF_SIZE	96
#define LENGTH		(*(unsigned char *)(INFOMEM))	// where length is written

#define BAUD_RATE 2400
#define DELAY_CENTER 190	// 54 start bit center + 136 to jump to LSB center
#define DELAY_INTRA	134
#define DELAY_STOP	56	// nee 58

/*
#define BAUD_RATE 1200
#define DELAY_CENTER 398 	// nee 124 start bit center+275 to jump to LSB center
#define DELAY_INTRA	267	// nee 272
#define DELAY_STOP	132		// nee 127
*/

// GLOBALS  ========================================
volatile unsigned char buffer[BUF_SIZE];	// software UART buffer
volatile unsigned char i;		// buffer position

// FUNCTION PROTOTYPES =============================

void init_leds(void);
void init_clocks(void);
void init_serial(void);

void erase_seg(char *);
void write_byte(unsigned char, unsigned char *);
void write_word(unsigned int *, unsigned int *);

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

	i = 0;		
	__enable_interrupt();			// interrupt for software UART
	while (!buffer[0]);				// wait to receive length data
	while (i < buffer[0]+2);	// wait to get all song data
	__disable_interrupt();		// no interrupts during flash write

	// write constants to information memory (bottom of Segment D)
	ptr = (unsigned char *)INFOMEM;
	write_byte(buffer[0], ptr++);
	write_byte(buffer[1], ptr++);

	// write melody 
	for (i = 0; i < LENGTH; i++)
		write_byte(buffer[i+2], ptr++);

	i = 0;
	__enable_interrupt();		// read the last half of data
	while (i < LENGTH + 2);	// wait for tempo & beats
	__disable_interrupt();

	// write tempo to middle of available info mem
	ptr = (unsigned char *)(INFOMEM + 192 / 2);
	write_word((unsigned int *)buffer, (unsigned int *)ptr);
	ptr += 2;

	// write beats
	for (i = 0; i < LENGTH; i++)
		write_byte(buffer[i+2], ptr++);

	// we're done!
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

void write_word(unsigned int *data, unsigned int *ptr)
{
	FCTL3 = FWKEY;						// clear LOCK
	FCTL1 = FWKEY + WRT;			// enable write
	*ptr = *data;							// write data
	FCTL1 = FWKEY;						// clear WRITE bit
	FCTL3 = FWKEY + LOCK;			// set LOCK
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

// ideas borrowed from David Mellis's SoftwareSerial 
// and Mikal Hart's NewSoftSerial for Arduino
unsigned char read()
{
	unsigned char val = 0;

	// make sure line has gone low (we're on a start bit)
	if (!(P1IN & RXD)) {
		unsigned char bit;
		// jump to middle of first data bit
		delay(DELAY_CENTER);

		// bit to potentially write: goes from 0 (LSB) to 7 (MSB)
		for (bit = 1; bit; bit <<= 1) {

			// read bit
			if (P1IN & RXD)
				val |= bit;

			// jump to middle of next bit
			delay(DELAY_INTRA);
		}
		// delay for stop bit
		delay(DELAY_STOP);

		return val;
	}
	// didn't get data
	return 0xFF;
}

#ifdef GCC
interrupt(PORT1_VECTOR) PORT1_ISR(void)
#else
#pragma vector=PORT1_VECTOR
__interrupt void PORT1_ISR(void)
#endif
{
#ifdef SAFETY
	unsigned char temp;
#endif
	P1IE &= ~(RXD);				// disable interrupt
	P1IFG &= ~(RXD);			// clear interrupt flag
#ifdef SAFETY
	temp = read();
	if (temp != 0xFF) {		// don't store in buffer if error
		buffer[i++] = temp;
	}
#else
	buffer[i++] = read();	// read data
#endif
	P1IE |= RXD;					// enable interrupt
}
