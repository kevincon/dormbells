#define SEGMENT_A (0x10FF)
#define SEGMENT_B (0x10FF - 64)
#define SEGMENT_C (0x10FF - 128)
#define SEGMENT_D (0x10FF - 192)
#define INFOMEM		(0x1000)

void erase_seg(char *);
void write_byte(unsigned char, unsigned char *);
void write_word(unsigned int *, unsigned int *);
