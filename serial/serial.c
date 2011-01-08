#include <io.h>
#include "serial.h"
#include "pins.h"

void init_serial()
{
	P1DIR &= ~(RXD);	// change to input
	P1IES |= RXD;			// interrupt on falling edge
	P1IFG &= ~(RXD);	// clear interrupt flag
	P1IE |= RXD;			// enable interrupt
}

// Delay Routine from mspgcc help file
// Takes 3 clock cycles to execute 1 iteration
void __inline__ delay(register unsigned int n)
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
