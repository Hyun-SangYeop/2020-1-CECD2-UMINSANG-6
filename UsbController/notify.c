
#include "notify.h"
#include "socket.h"

extern pthread_mutex_t mutex, mutex2;

//감지할 이벤트 항목
static uint64_t event_mask =
    (FAN_CLOSE_WRITE);


void cutString(char *event_path, char *usb_path, char *moveto){
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


//경로 출력 함수 (발생한 이벤트의 fd를 이용하여 이벤트가 발생한 경로 찾기용도)
static char *
get_file_path_from_fd(int fd,char *buffer,size_t buffer_size){
    ssize_t len;
    if (fd <= 0)
        return NULL;

    sprintf(buffer, "/proc/self/fd/%d", fd);
    if ((len = readlink(buffer, buffer, buffer_size - 1)) < 0)
        return NULL;

    buffer[len] = '\0';
    return buffer;
}


char *findUserID(userNames *users, int cnt, struct inotify_event *event){
    for (int i = 0; i < cnt; ++i){
        if (event->wd == users[i].wd){
            return users[i].name;
        }
    }
    printf("couldn't find userID\n");
    return NULL;
}

//usb장착/탈착이 될때 생성되는 디렉토리 경로를 찾기 위해 아이노티파이 활용
void get_inotify_event(int i_fd, int f_fd, userNames *users, int cnt){
    char buffer[1000];
    char path[100];
    int length, i = 0;

    length = read(i_fd, buffer, 1000);
    if (length < 0){
        perror("read");
    }

    while (i < length){
        struct inotify_event *event = (struct inotify_event *)&buffer[i];
        if (event->len){
            char *userID = findUserID(users, cnt, event);
            sprintf(path, "/media/%s/%s", userID, event->name);

            if (event->mask & IN_CREATE){
                if (event->mask & IN_ISDIR){
                    printf("directory : %s was created\n", path);

                    sleep(1);
                    if (fanotify_mark(f_fd, FAN_MARK_ADD | FAN_MARK_MOUNT, event_mask, AT_FDCWD, path) < 0){
                        printf("mark error\n");
                        return;
                    }
                    else
                        printf("%s is marked\n", path);
                }
                else
                    printf("The file %s was Created with WD %d\n", path, event->wd);
            }

            if (event->mask & IN_DELETE){
                if (event->mask & IN_ISDIR){
                    printf("The directory %s was deleted.\n", path);
                    if (fanotify_mark(f_fd, FAN_MARK_REMOVE, event_mask, AT_FDCWD, path) >= 0){
                        printf("removed mark\n");
                    }
 
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
int port = 65500;
char temp[100];
void get_fanotify_event(struct fanotify_event_metadata *event, int fd, ssize_t length){
    char buffer_filepath[100];

    while(FAN_EVENT_OK(event,length)){

        get_file_path_from_fd(event->fd, buffer_filepath, 100);

        if (strstr (buffer_filepath, "/.Trash") != NULL) {
            return;
        }

        pthread_mutex_lock(&mutex2);
        if(strcmp(temp,buffer_filepath)==0){
            temp[0]=0;
            pthread_mutex_unlock(&mutex2);
            return;
        }
        strcpy(temp, buffer_filepath);
        pthread_mutex_unlock(&mutex2);

        printf("Received event in path '%s'\n", buffer_filepath);

        //handler
        if (event->mask & FAN_CLOSE_WRITE){
            sleep(1);
            int x;
            char command[100];
            char log_path[100];
            char usb_path[100];
            //printf("close event\n");

            sprintf(command, "mv \"%s\" /usb", buffer_filepath);
            system(command);

            cutString(buffer_filepath, usb_path, log_path);

            //----------------------------socket
            int client_socket;
            int client_socket_option;
            struct sockaddr_in server_addr;
            char *toServer = log_path;
            char fromServer[1024];

            client_socket = socket(PF_INET, SOCK_STREAM, 0);
            while (client_socket == -1){
                client_socket = socket(PF_INET, SOCK_STREAM, 0);
            }

            //char *IPaddress = getIPaddress();
 	    char IPaddress[10] = "10.0.2.15";
            memset(&server_addr, 0, sizeof(server_addr));
            server_addr.sin_family = AF_INET;
            server_addr.sin_addr.s_addr = inet_addr(IPaddress);
            server_addr.sin_port = htons(port);

            if (connect(client_socket, (struct sockaddr *)&server_addr, sizeof(server_addr))){
                printf("connect fail");
                exit(1);
            }
            write(client_socket, toServer, strlen(toServer));

            while (1){
                if (read(client_socket, fromServer, sizeof(fromServer)) > 0){
                    //printf("From Server Message: %s\n", fromServer);
                    break;
                }
            }
            close(client_socket);

            time_t t = time(NULL);
            struct tm tm = *localtime(&t);

            printf("%d-%d-%d %d:%d:%d\n", tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec);
            
            char ssnNum[10] = "\0";
            char mphNum[10] = "\0";
            char phnNum[10] = "\0";
            char hinNum[10] = "\0";
            
            //통제 해야한다면
            if (fromServer[0] == '1'){
                char *temp = strtok(fromServer, " ");
                int num = 0;
                while (temp != NULL){
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
                system(httpmsg);
                printf("\n%s is blocked\n\n",strrchr(buffer_filepath,'/')+sizeof(char));
            }
            //통제 필요없으면 다시 로그디렉토리에서 가져옴
            else{
                pthread_mutex_lock(&mutex);
                fanotify_mark(fd, FAN_MARK_REMOVE | FAN_MARK_MOUNT, event_mask, AT_FDCWD,usb_path);
                sprintf(command, "mv \"%s\" \"%s\"", log_path, usb_path);
                system(command);
                fanotify_mark(fd, FAN_MARK_ADD | FAN_MARK_MOUNT, event_mask, AT_FDCWD,usb_path);
                pthread_mutex_unlock(&mutex);
		sleep(1); // 아래 passed printf가 blocked사이에 섞여서 뜰 때가 있음 (기능은 문제X)
                printf("%s is passed\n\n", strrchr(buffer_filepath, '/') + sizeof(char));
            }
            
        }

        if(event->fd>0) close(event->fd);
        event=FAN_EVENT_NEXT(event, length);
    }
}

void* thread_fan_event(void* arg){
	struct pollfd* fds = arg;
	
    char buffer[100];
    ssize_t length;
    if((length=read(fds[1].fd, buffer, 8192))>0){
        struct fanotify_event_metadata* metadata;
        metadata= (struct fanotify_event_metadata*) buffer;
        get_fanotify_event(metadata,fds[1].fd, length);
        }
        else{
                printf("nothing read\n");
        }                                
}
