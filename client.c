#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <netdb.h>

void checkHostName(int hostname){
    if(hostname==-1){
        perror("gethostname");
        exit(1);
    }
}

void checkHostEntry(struct hostent * hostentry)
{
    if(hostentry==NULL){
        perror("gethostbyname");
        exit(1);
    }
}

void checkIPbuffer(char *IPbuffer){
    if(IPbuffer==NULL){
        perror("inet_ntoa");
        exit(1);
    }
}

char* getIPaddress(){
    char hostbuffer[256];
    char *IPbuffer;
    struct hostent *host_entry;
    int hostname;

    hostname=gethostname(hostbuffer,sizeof(hostbuffer));
    checkHostName(hostname);

    host_entry=gethostbyname(hostbuffer);
    checkHostEntry(host_entry);

    host_entry->h_addr_list[0];

    IPbuffer = inet_ntoa(*((struct in_addr *)host_entry->h_addr_list[0]));

    return IPbuffer;
}

int main(){
    int client_socket;
    struct sockaddr_in server_addr;
    char message[1024] = "this is message";

    //TCP ipv4 socket 
    client_socket=socket(PF_INET,SOCK_STREAM,0);

    if(client_socket==-1){
        printf("create socket fail");
        exit(1);
    }

    //get local IP address
    char *IPaddress=getIPaddress();
    printf("%s \n",IPaddress);

    //store server address
    memset(&server_addr,0,sizeof(server_addr));

    //IPv4
    server_addr.sin_family=AF_INET;

    //127.0.0.1
    //store server address ip
    server_addr.sin_addr.s_addr=inet_addr(IPaddress);
    
    //store server address port number
    server_addr.sin_port=htons(1218);

    if(connect(client_socket,(struct sockaddr*)&server_addr,sizeof(server_addr))){
        printf("connect fail");
        exit(1);
    }

    write(client_socket,message,sizeof(message)+1);

    close(client_socket);
}



