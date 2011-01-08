#define BAUD_RATE 2400

#if BAUD_RATE == 2400
#define DELAY_CENTER 190	// 54 start bit center + 136 to jump to LSB center
#define DELAY_INTRA	134
#define DELAY_STOP	56	// nee 58
#endif

#if BAUD_RATE == 1200
#define DELAY_CENTER 398 	// nee 124 start bit center+275 to jump to LSB center
#define DELAY_INTRA	267	// nee 272
#define DELAY_STOP	132		// nee 127
#endif

void init_serial(void);
void __inline__ delay(register unsigned int n);
unsigned char read(void);
