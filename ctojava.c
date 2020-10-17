#include <stdio.h>       
#include <stdlib.h>       
#include <string.h>      
#include <unistd.h>
#include <arpa/inet.h>   
#include <sys/types.h>    
#include <sys/socket.h>

#define PORT 55000

int main()  {
	int clientSocket;
	struct sockaddr_in  server_address;
	char toServer[100]="Hello Server!!! I'm Client!!";
	char fromServer[100];
	int str_len;

	/*서버에 접속해서 데이터를 입출력 하는 클라이언트 소켓생성*/
    clientSocket=socket(PF_INET, SOCK_STREAM, 0);
	printf("Create Client Socket!!\n");

	/*서버의 주소 정보가 저장될 server_adress 를 0으로 초기화*/
	memset(&server_address, 0, sizeof(server_address));

	/*server_address 에 서버의 주소 정보 대입*/
	server_address.sin_family=AF_INET;
	server_address.sin_addr.s_addr=inet_addr("127.0.0.1");
	server_address.sin_port=htons(PORT);

	/*서버에 접속*/
	connect(clientSocket, (struct sockaddr*)&server_address, sizeof(server_address));
	printf("Connect Server!!\n");
	
	strcpy(toServer, "chat start!");
	while(1){
		printf("To Server Message: ");
		gets(toServer);

		/*toServer 에 저장된 메시지를 서버로 전송*/
		write(clientSocket,toServer,sizeof(toServer));
		if(strcmp(toServer, "Bye") == 0)
			break;

		/*서버에서 보내온 메시지를 fromServer 에 저장*/
		read(clientSocket, fromServer, sizeof(fromServer));
		printf("From Server Message: %s\n",fromServer);
	}


	close(clientSocket);	

	return 0;
}

