#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <netdb.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


void checkHostName(int hostname);

void checkHostEntry(struct hostent *hostentry);

void checkIPbuffer(char *IPbuffer);

char *getIPaddress();