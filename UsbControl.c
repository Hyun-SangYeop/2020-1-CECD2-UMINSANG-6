#define _GNU_SOURCE /* Needed to get O_LARGEFILE definition */
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <string.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/fanotify.h>
#include <unistd.h>
#include <sys/inotify.h>
/* Read all available fanotify events from the file descriptor 'fd' */

#include <time.h>

//----socket
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <netdb.h>

//-----

#define MAX_EVENTS 1024                                /*Max. number of events to process at one go*/
#define LEN_NAME 1024                                  /*Assuming length of the filename won't exceed 16 bytes*/
#define EVENT_SIZE (sizeof(struct inotify_event))      /*size of one event*/
#define BUF_LEN (MAX_EVENTS * (EVENT_SIZE + LEN_NAME)) /*buffer to store the data of events*/

typedef struct userNames
{
    char name[50];
    uint32_t wd;
} userNames;

//감지할 이벤트 항목
static uint64_t event_mask =
    (FAN_CLOSE_WRITE);

//-----------------------------socket
void checkHostName(int hostname)
{
    if (hostname == -1)
    {
        perror("gethostname");
        exit(1);
    }
}

void checkHostEntry(struct hostent *hostentry)
{
    if (hostentry == NULL)
    {
        perror("gethostbyname");
        exit(1);
    }
}

void checkIPbuffer(char *IPbuffer)
{
    if (IPbuffer == NULL)
    {
        perror("inet_ntoa");
        exit(1);
    }
}

char *getIPaddress()
{
    char hostbuffer[256];
    char *IPbuffer;
    struct hostent *host_entry;
    int hostname;

    hostname = gethostname(hostbuffer, sizeof(hostbuffer));
    checkHostName(hostname);

    host_entry = gethostbyname(hostbuffer);
    checkHostEntry(host_entry);

    host_entry->h_addr_list[0];

    IPbuffer = inet_ntoa(*((struct in_addr *)host_entry->h_addr_list[0]));

    return IPbuffer;
}

//------------------------------------------------socket

int cnt = 0;

void cutString(char *event_path, char *usb_path, char *moveto)
{
    int len1, len2;
    char *tmp;
    tmp = strrchr(event_path, '/');

    strcpy(moveto, tmp);

    len1 = strlen(event_path);
    len2 = strlen(moveto);

    strncpy(usb_path, event_path, len1 - len2);
    sprintf(moveto, "/usb%s", tmp);

    usb_path[len1 - len2] = '\0';
    return;
}

// //스캐너 예제
// int scanner(char *path)
// {
    // int f_fd = open(path, O_RDONLY);
    // char content[1000];
    // int len;
    // if (f_fd < 0)
    // {
        // printf("open failure\n");
        // return 0;
    // }

    // if ((len = read(f_fd, content, 1000)) <= 0)
    // {
        // close(f_fd);
    // }
    // content[len] = '\0';
    // close(f_fd);

    // //if content contaions the word "evil" -1 will be returned
    // if (strstr(content, "evil") != NULL)
    // {
        // return -1;
    // }
    // else
    // {
        // return 0;
    // }
// }

//경로 출력 함수 (발생한 이벤트의 fd를 이용하여 이벤트가 발생한 경로 찾기용도)
static char *
get_file_path_from_fd(int fd,
                      char *buffer,
                      size_t buffer_size)
{
    ssize_t len;

    if (fd <= 0)
        return NULL;

    sprintf(buffer, "/proc/self/fd/%d", fd);
    if ((len = readlink(buffer, buffer, buffer_size - 1)) < 0)
        return NULL;

    buffer[len] = '\0';
    return buffer;
}

// 이진 탐색으로 개선 필
char *findUserID(userNames *users, int cnt, struct inotify_event *event)
{
    for (int i = 0; i < cnt; ++i)
    {
        if (event->wd == users[i].wd)
        {
            return users[i].name;
        }
    }
    printf("couldn't find userID\n");
    return NULL;
}

//usb장착/탈착이 될때 생성되는 디렉토리 경로를 찾기 위해 아이노티파이 활용
void get_inotify_event(int i_fd, int f_fd, userNames *users, int cnt)
{
    char buffer[1000];
    char path[100];
    int length, i = 0;

    length = read(i_fd, buffer, 1000);
    if (length < 0)
    {
        perror("read");
    }

    while (i < length)
    {
        struct inotify_event *event = (struct inotify_event *)&buffer[i];
        if (event->len)
        {
            char *userID = findUserID(users, cnt, event);
            sprintf(path, "/media/%s/%s", userID, event->name);

            if (event->mask & IN_CREATE)
            {
                if (event->mask & IN_ISDIR)
                {
                    printf("directory : %s was created\n", path);

                    sleep(1);

                    if (fanotify_mark(f_fd, FAN_MARK_ADD | FAN_MARK_MOUNT, event_mask, AT_FDCWD, path) < 0)
                    {
                        printf("mark error\n");
                        return;
                    }
                    else
                        printf("%s is marked\n", path);
                }
                else
                    printf("The file %s was Created with WD %d\n", path, event->wd);
            }

            if (event->mask & IN_DELETE)
            {
                if (event->mask & IN_ISDIR)
                {
                    printf("The directory %s was deleted.\n", path);

                    if (fanotify_mark(f_fd, FAN_MARK_REMOVE, event_mask, AT_FDCWD, path) >= 0)
                    {
                        printf("removed mark\n");
                    }
                    //		      			else{	printf("remove mark failure\n");}
                }
                else
                    printf("The file %s was deleted with WD %d\n", path, event->wd);
            }

            i += EVENT_SIZE + event->len;
        }
    }
}

//위 아이노티파이에서 장착된 유에스비 경로가 마크 되었기 때문에
//이제부터 감지대상의 이벤트가 유에스비에서 발생하면 아래의 함수가 호출됨.
int port = 50000;
void get_fanotify_event(struct fanotify_event_metadata *event, int fd)
{
    char buffer_filepath[100];
    get_file_path_from_fd(event->fd, buffer_filepath, 100);
    printf("Received event in path '%s'\n", buffer_filepath);
    //handler
    if (event->mask & FAN_CLOSE_WRITE)
    {
        sleep(1);
        int x;
        char command[100];
        char log_path[100];
        char usb_path[100];
        //printf("close event\n");

        sprintf(command, "mv \"%s\" /usb", buffer_filepath);
        system(command);

        cutString(buffer_filepath, usb_path, log_path);

        // // mail 실험 현재 중지
        // 		x=scanner(log_path);

        // 		//통제 해야한다면 메일 보내고 block 메세지 출력하기
        // 		if(x==-1){
        // 		      	char mail_command[300];
        // 	    		char mail_content[100]={"strange trial to copy confidential file was detected"};
        // 			sprintf(mail_command, "echo \"%s\" | mail -s 'alert' whitesky118@gmail.com",mail_content);  //whitesky118@gmail.com 으로 보내기
        // 		    	system(mail_command);
        // 			printf("blocked\n");
        // 		}
        // 		//통제 필요없으면 다시 로그디렉토리에서 가져옴
        // 		else{
        // 			cnt++;
        // 			sprintf(command,"mv \"%s\" \"%s\"",log_path,usb_path);
        // 			system(command);
        // 		}
        // //!**	이 부분까지를 삭제하고 아래 소켓 주석을 풀 것 **!

        //----------------------------socket
        int client_socket;
        int client_socket_option;
        //int port = 50000;
        struct sockaddr_in server_addr;
        //char message[PATH_MAX]=path;
        //char *sendmessage = path;
        //char inputMessage[100] = "";
        char *toServer = log_path;
        char fromServer[1024];

        //TCP ipv4 socket
        client_socket = socket(PF_INET, SOCK_STREAM, 0);
        while (client_socket == -1)
        {
            client_socket = socket(PF_INET, SOCK_STREAM, 0);
        }
        //     if (client_socket == -1)
        //     {
        //       printf("create socket fail");
        //       exit(1);
        //     }

        //get local IP address
        //KU: 127.0.0.1이 아닌 경우도 있었다
        char *IPaddress = getIPaddress();
        //printf("%s \n", IPaddress);

        //store server address
        memset(&server_addr, 0, sizeof(server_addr));

        //IPv4
        server_addr.sin_family = AF_INET;

        //127.0.0.1
        //store server address ip
        server_addr.sin_addr.s_addr = inet_addr(IPaddress);

        //store server address port number
        server_addr.sin_port = htons(port++);
		
		// bind 허가, 이건 서버에 넣어줘야... (개인정보 검출 모듈 JAVA)
        //client_socket_option = 1;
        //setsockopt(client_socket, SOL_SOCKET, SO_REUSEADDR, &client_socket_option, sizeof(client_socket_option));

        if (connect(client_socket, (struct sockaddr *)&server_addr, sizeof(server_addr)))
        {
            printf("connect fail");
            exit(1);
        }

        //printf("To Server Message: ");
        //puts(toServer);

        /*toServer 에 저장된 메시지를 서버로 전송*/
        write(client_socket, toServer, strlen(toServer));

        while (1)
        {

            if (read(client_socket, fromServer, sizeof(fromServer)) > 0)
            {
                //printf("From Server Message: %s\n", fromServer);
                break;
            }
        }

        //printf("Validator로 소켓통신 완료!");

        close(client_socket);

        //-------------------------------socket

        // 1. fromServer 쪼개 (-1or0, ssn, mph, phn, hin)
        // 2. 결과 화면 깔끔하게
        //fromServer[strlen(fromServer)-1] = '\0';
        //printf("fromServer: %s\n", fromServer);

        time_t t = time(NULL);
        struct tm tm = *localtime(&t);

        printf("%d-%d-%d %d:%d:%d\n", tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec);
		
		char ssnNum[10] = "\0";
		char mphNum[10] = "\0";
		char phnNum[10] = "\0";
		char hinNum[10] = "\0";
		
        //통제 해야한다면
        if (fromServer[0] == '1')
        {
			char *temp = strtok(fromServer, " ");
            int num = 0;
            while (temp != NULL)
            {
                    if (num == 1){
                            printf("ssn: %s\n", temp);
							strcpy(ssnNum, temp);
					}
                    else if (num == 2){
                            printf("mph: %s\n", temp);
							strcpy(mphNum, temp);
					}
                    else if (num == 3){
                            printf("phn: %s\n", temp);
							strcpy(phnNum, temp);
					}
                    else if (num == 4){
                            printf("hin: %s\n", temp);
							strcpy(hinNum, temp);
					}

                    num++;
                    if (num >= 5)
                            break;

                    temp = strtok(NULL, " ");
            }
			
			// 앱의 서버 IP 주소로 변경해주어야
			// curl 설치 필요 (USB 통제 모듈이 작동하는 곳에서)
            // test와 filepath는 앱 서버 코드에 따라 달라질 수 있음
            // --data-urlencode는 한글을 인식하기 위해 필요 (하나의 데이터)
            char httpmsg[500]="curl -G http://localhost:8080/test --data-urlencode \"filepath=";
            sprintf(httpmsg, "%s%s\" -d \"ssn=%s&mph=%s&phn=%s&hin=%s\"",
                                        httpmsg, buffer_filepath, ssnNum, mphNum, phnNum, hinNum);
            //printf("%s\n", httpmsg);
			system(httpmsg);
            printf("\n%s is blocked\n\n",strrchr(buffer_filepath,'/')+sizeof(char));

        }
        //통제 필요없으면 다시 로그디렉토리에서 가져옴
        else
        {
            cnt++;
            sprintf(command, "mv \"%s\" \"%s\"", log_path, usb_path);
            system(command);
            printf("%s is passed\n\n", strrchr(buffer_filepath, '/') + sizeof(char));
        }
    }
}

int countUsers(FILE *fp)
{
    int cnt = 0;
    while (1)
    {
        char tmp[50];
        if (fscanf(fp, "%s", tmp) == -1)
            break;
        cnt++;
    }
    return cnt;
}

void readUsers(FILE *fp, int count, userNames *users)
{
    fseek(fp, 0L, SEEK_SET);
    for (int i = 0; i < count; ++i)
    {
        fscanf(fp, "%s", users[i].name);
    }
}

int main(int argc, char *argv[])
{

    /* erase
	if (argc != 2) {
		printf("현재 프로그램은 매개변수에 /media/userid 를 넣어주어야 합니다\n");
		exit(1);
	}

	else {
		strcpy(mediausername, argv[1]);
	}
	*/

    system("ls /media > /list");
    FILE *fp = fopen("/list", "r");
    if (fp == NULL)
    {
        printf("file open failure\n");
        return -1;
    }
    int numOfUsers = countUsers(fp);
    userNames *users = (userNames *)malloc(sizeof(userNames) * numOfUsers);
    readUsers(fp, numOfUsers, users);

    int fd, poll_num;
    nfds_t nfds; //number of  inotify, fanotify fds
    int i_fd, wd;
    struct pollfd fds[2];

    //아이노티파이 설치
    i_fd = inotify_init();
    if (i_fd < 0)
    {
        perror("couldn't initialize inotify\n");
    }

    // add users on inotify
    for (int i = 0; i < numOfUsers; ++i)
    {
        char user_path[100];
        sprintf(user_path, "/media/%s", users[i].name);
        users[i].wd = inotify_add_watch(i_fd, user_path, IN_CREATE | IN_DELETE);
        if (users[i].wd == -1)
        {
            printf("inotify couldn't add watch %s\n", user_path);
        }
    }

    /* erase 
	wd = inotify_add_watch(i_fd, argv[1], IN_CREATE | IN_DELETE);
	if(wd==-1){
		printf("inotify couldn't add watch\n");
	}
	else{
		printf("inotify watching...\n");
	}
	*/

    //패노티파이 설치
    fd = fanotify_init(FAN_CLOEXEC | FAN_CLASS_CONTENT | FAN_NONBLOCK,
                       O_RDONLY | O_LARGEFILE);
    if (fd == -1)
    {
        perror("fanotify_init");
        exit(EXIT_FAILURE);
    }

    //패노티파이가 감시할 목록은 아이노티파이 이벤트핸들러가 추가해줄 것임.
    nfds = 2;
    fds[0].fd = i_fd;
    fds[0].events = POLLIN;
    fds[1].fd = fd;
    fds[1].events = POLLIN;

    //printf("listening for events\n"); erase

    //감시 시작
    while (1)
    {
        poll_num = poll(fds, nfds, -1);
        if (poll_num == -1)
        {
            if (errno = EINTR)
                continue;
            perror("poll");
            exit(EXIT_FAILURE);
        }
        if (poll_num > 0)
        {

            //아이노티파이가 이벤트가  발생했을 경우
            if (fds[0].revents & POLLIN)
            {
                //printf("inofity event caught!\n");
                get_inotify_event(i_fd, fd, users, numOfUsers);
            }

            //패노티파이 이벤트가 발생했을 경우
            if (fds[1].revents & POLLIN)
            {
                if (cnt >= 1)
                {
                    cnt = 0;
                    char buffer[100];
                    ssize_t length;
                    if ((length = read(fds[1].fd, buffer, 8192)) > 0)
                    {
                        struct fanotify_event_metadata *metadata;
                        metadata = (struct fanotify_event_metadata *)buffer;
                        while (FAN_EVENT_OK(metadata, length))
                        {
                            if (metadata->fd > 0)
                            {
                                close(metadata->fd);
                            }
                            metadata = FAN_EVENT_NEXT(metadata, length);
                        }
                        continue;
                    }
                }
                //printf("fan event caught!\n");
                char buffer[100];
                ssize_t length;

                if ((length = read(fds[1].fd, buffer, 8192)) > 0)
                {
                    struct fanotify_event_metadata *metadata;
                    metadata = (struct fanotify_event_metadata *)buffer;
                    while (FAN_EVENT_OK(metadata, length))
                    {
                        get_fanotify_event(metadata, fd);
                        if (metadata->fd > 0)
                        {
                            close(metadata->fd);
                        }
                        metadata = FAN_EVENT_NEXT(metadata, length);
                    }
                }
                else
                {
                    printf("nothing read\n");
                }
            }
        }
    }

    free(users);
    close(i_fd);
    close(fd);
}
