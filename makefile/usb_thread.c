#define _GNU_SOURCE /* Needed to get O_LARGEFILE definition */
#include <sys/stat.h>
#include "notify.h"



long long num=0;
pthread_mutex_t mutex, mutex2;


int countUsers(FILE *fp);
void readUsers(FILE *fp, int count, userNames *users);

int main(int argc, char *argv[]){
    const char* folder;
    folder="/usb";
    struct stat sb;

    if(!(stat(folder,&sb) == 0 && S_ISDIR(sb.st_mode))){
	    system("mkdir /usb");
    }

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

    pthread_t thread_id;
    pthread_mutex_init(&mutex,NULL);
    pthread_mutex_init(&mutex2,NULL);

    //아이노티파이 설치
    i_fd = inotify_init();
    if (i_fd < 0){
        perror("couldn't initialize inotify\n");
    }

    // add users on inotify
    for (int i = 0; i < numOfUsers; ++i){
        char user_path[100];
        sprintf(user_path, "/media/%s", users[i].name);
        users[i].wd = inotify_add_watch(i_fd, user_path, IN_CREATE | IN_DELETE);
        if (users[i].wd == -1){
            printf("inotify couldn't add watch %s\n", user_path);
        }
    }

    //패노티파이 설치
    fd = fanotify_init(FAN_CLOEXEC | FAN_CLASS_CONTENT | FAN_NONBLOCK,
                       O_RDONLY | O_LARGEFILE);
    if (fd == -1){
        perror("fanotify_init");
        exit(EXIT_FAILURE);
    }

    //패노티파이가 감시할 목록은 아이노티파이 이벤트핸들러가 추가해줄 것임.
    nfds = 2;
    fds[0].fd = i_fd;
    fds[0].events = POLLIN;
    fds[1].fd = fd;
    fds[1].events = POLLIN;

    //감시 시작
    while (1){
        poll_num = poll(fds, nfds, -1);
        if (poll_num == -1){
            if (errno = EINTR)
                continue;
            perror("poll");
            exit(EXIT_FAILURE);
        }
        if (poll_num > 0){
            //아이노티파이가 이벤트가  발생했을 경우
            if (fds[0].revents & POLLIN){
                get_inotify_event(i_fd, fd, users, numOfUsers);
            }

            //패노티파이 이벤트가 발생했을 경우
            else if (fds[1].revents & POLLIN){
                if(pthread_create((&thread_id),NULL, thread_fan_event,(void*)fds)!=0){
                    puts("pthread_create() error");
                    return -1;
                }
                else{
                    pthread_detach(thread_id);
                    usleep(100);
                }

            }
            else{
                printf("nothing read\n");
            }
        }
    }
    pthread_mutex_destroy(&mutex);
    pthread_mutex_destroy(&mutex2);
    free(users);
    close(i_fd);
    close(fd);
}

int countUsers(FILE *fp){
    int cnt = 0;
    while (1){
        char tmp[50];
        if (fscanf(fp, "%s", tmp) == -1)
            break;
        cnt++;
    }
    return cnt;
}

void readUsers(FILE *fp, int count, userNames *users){
    fseek(fp, 0L, SEEK_SET);
    for (int i = 0; i < count; ++i)
    {
        fscanf(fp, "%s", users[i].name);
    }
}