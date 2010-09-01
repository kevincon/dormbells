#!/bin/bash

svn="$HOME/dormbells/svn/"
serial="$svn/serial/"
dormbell="$svn/dormbell/"

if [[ $UID -ne 0 ]]; then
	echo "$0 must be run as root"
	exit 1
fi

if [ -e "/dev/ttyACM0" ]; then
	mv /dev/ttyACM0 /dev/ttyUSB0
fi

cd $serial
mspdebug rf2500 "prog main.elf"

read -p "Press ENTER to continue "

cd $dormbell
mspdebug rf2500 "prog main.elf"
