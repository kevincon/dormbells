#include <io.h>

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
