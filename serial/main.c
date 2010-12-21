/* DormBell Serial Flasher
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

#include <io.h>
#include <signal.h>

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
	unsigned char total_bytes;

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
	while (!buffer[0]);				// wait to receive count of total bytes
	i = 0; total_bytes = buffer[0];
	while (i < total_bytes && i < BUF_SIZE);	// wait to get all (or a buffer's worth) of data
	__disable_interrupt();		// no interrupts during flash write

	// write as much as possible within the buffer
	ptr = (unsigned char *)INFOMEM;
	for (i = 0; i < total_bytes && i < BUF_SIZE; i++)
		write_byte(buffer[i], ptr++);

	i = 0;
	// get and write the rest of it
	if (total_bytes-BUF_SIZE > 0) {
		__enable_interrupt();
		while (i < (total_bytes-BUF_SIZE));
		__disable_interrupt();

		// write
		for (i = 0; i < (total_bytes-BUF_SIZE); i++)
		write_byte(buffer[i], ptr++);
	}

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

interrupt(PORT1_VECTOR) PORT1_ISR(void)
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
