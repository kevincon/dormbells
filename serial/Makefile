CC=msp430-gcc
CFLAGS=-Os -Wall -g -mmcu=msp430x2012

OBJS=main.o serial.o flash.o


all: $(OBJS)
	$(CC) $(CFLAGS) -o main.elf $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) -c $<

clean:
	rm -fr main.elf $(OBJS)
