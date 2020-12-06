all: usbController

usbController: usb_thread.c notify.c socket.c
	gcc -o usbController usb_thread.c notify.c socket.c -D_REENTRANT -lpthread

clean:
	rm usbController
