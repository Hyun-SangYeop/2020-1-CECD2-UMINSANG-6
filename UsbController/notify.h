#define _GNU_SOURCE /* Needed to get O_LARGEFILE definition */
#define MAX_EVENTS 1024                                /*Max. number of events to process at one go*/
#define LEN_NAME 1024                                  /*Assuming length of the filename won't exceed 16 bytes*/
#define EVENT_SIZE (sizeof(struct inotify_event))      /*size of one event*/
#define BUF_LEN (MAX_EVENTS * (EVENT_SIZE + LEN_NAME)) /*buffer to store the data of events*/

#include <poll.h>
#include <stdio.h>
#include <sys/fanotify.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <string.h>
#include <pthread.h>
#include <fcntl.h>
#include <stdlib.h>
#include <time.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>


typedef struct userNames{
    char name[50];
    uint32_t wd;
} userNames;




void cutString(char *event_path, char *usb_path, char *moveto);

//경로 출력 함수 (발생한 이벤트의 fd를 이용하여 이벤트가 발생한 경로 찾기용도)
static char *get_file_path_from_fd(int fd,char *buffer,size_t buffer_size);

char *findUserID(userNames *users, int cnt, struct inotify_event *event);

//usb장착/탈착이 될때 생성되는 디렉토리 경로를 찾기 위해 아이노티파이 활용
void get_inotify_event(int i_fd, int f_fd, userNames *users, int cnt);

//위 아이노티파이에서 장착된 유에스비 경로가 마크 되었기 때문에
//이제부터 감지대상의 이벤트가 유에스비에서 발생하면 아래의 함수가 호출됨.
void get_fanotify_event(struct fanotify_event_metadata *event, int fd, ssize_t length);

void* thread_fan_event(void* arg);