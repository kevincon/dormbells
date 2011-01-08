CC=msp430-gcc
CFLAGS=-Os -Wall -g -mmcu=msp430x2012

OBJS=main.o serial.o


all: $(OBJS)
	$(CC) $(CFLAGS) -o main.elf $(OBJS)

main.o:
	$(CC) $(CFLAGS) -c main.c

serial.o:
	$(CC) $(CFLAGS) -c ../serial/serial.c

button: serial.o
	$(CC) $(CFLAGS) -c main.c -DBUTTON
	$(CC) $(CFLAGS) -o main.elf $(OBJS)

clean:
	rm -fr main.elf $(OBJS)
