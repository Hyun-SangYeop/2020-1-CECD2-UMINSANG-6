//아이노티파이와 패노티파이 연동 시도 코드


#define _GNU_SOURCE     /* Needed to get O_LARGEFILE definition */
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/fanotify.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <unistd.h>
/* Read all available fanotify events from the file descriptor 'fd' */
#include <stdio.h>

#define MAX_EVENTS 1024 /*Max. number of events to process at one go*/
#define LEN_NAME 1024 /*Assuming length of the filename won't exceed 16 bytes*/
#define EVENT_SIZE  ( sizeof (struct inotify_event) ) /*size of one event*/
#define BUF_LEN     ( MAX_EVENTS * ( EVENT_SIZE + LEN_NAME )) /*buffer to store the data of events*/

//감지할 이벤트 항목
static uint64_t event_mask =
(
	FAN_CLOSE_WRITE
//	FAN_ONDIR	|
//	FAN_OPEN	
//	FAN_ACCESS	|
//	FAN_CLOSE_NOWRITE 
//	FAN_OPEN_PERM
);


int count =0;
char usb_path[100];

//경로 출력 함수 (발생한 이벤트의 fd를 이용하여 이벤트가 발생한 경로 찾기용도)
static char *
get_file_path_from_fd (int     fd,
                         char   *buffer,
                         size_t  buffer_size){
  ssize_t len;

  if (fd <= 0)
    return NULL;

  sprintf (buffer, "/proc/self/fd/%d", fd);
  if ((len = readlink (buffer, buffer, buffer_size - 1)) < 0)
    return NULL;

  buffer[len] = '\0';
  return buffer;
}

//usb장착/탈착이 될때 생성되는 디렉토리 경로를 찾기 위해 아이노티파이 활용
void get_inotify_event(int i_fd, int f_fd){
	char buffer[1000];
	char path[1000];
	int length, i=0;
	
	length = read(i_fd, buffer, 1000);
	if(length<0){	perror("read");	}

	while(i<length){
		struct inotify_event* event= (struct inotify_event*) & buffer[ i ];
		if(event->len){
			if(event->mask & IN_CREATE){
				if(event->mask & IN_ISDIR){
					printf("directory : %s was created\n",event->name);
					sprintf(usb_path,"/media/yu/%s",event->name);
					sleep(1);
					count++;
					}
				else	printf( "The file %s was Created with WD %d\n", event->name, event->wd );
			}
			if(event->mask & IN_DELETE){
				if (event->mask & IN_ISDIR){
		     			printf( "The directory %s was deleted.\n", event->name );
					sprintf(path,"/media/yu/%s",event->name);
		    			if(fanotify_mark(f_fd, FAN_MARK_REMOVE, event_mask, AT_FDCWD, path)>=0){
						printf("removed mark\n");
				       	}
		      			else{	printf("remove mark failure\n");}
				}
				else	printf( "The file %s was deleted with WD %d\n", event->name, event->wd );
			}

			 i += EVENT_SIZE + event->len;
			}
		}
}

//위 아이노티파이에서 장착된 유에스비 경로가 마크 되었기 때문에
//이제부터 감지대상의 이벤트가 유에스비에서 발생하면 아래의 함수가 호출됨.
void get_fanotify_event(struct fanotify_event_metadata* event, int fd){
	char buffer[100];
	get_file_path_from_fd(event->fd, buffer, 100);
	printf ("Received event in path '%s'\n",buffer);
	//handler
        if(event->mask & FAN_OPEN)
		printf("fan open!\n");
	if(event->mask & FAN_ACCESS)
		printf("fan access\n");
	if(event->mask & FAN_CLOSE_WRITE)
		printf("fan close wirte!\n");
	if(event->mask & FAN_CLOSE_NOWRITE)
		printf("fan close no write!\n");
	if(event->mask & FAN_OPEN_PERM){
		struct fanotify_response access;
		access.fd = event->fd;

		printf("open permission!\n");

		access.response=FAN_ALLOW;
		write(fd, &access, sizeof(access));

	}
}

//argv[1] 은 /media/사용자명
int main(int argc, char* argv[]){
        int fd, poll_num;
        nfds_t nfds;

	int i_fd, wd;
	struct pollfd fds[2];

	//아이노티파이 설치
	i_fd = inotify_init();
	if(i_fd<0){
		perror("couldn't initialize inotify\n");
	}
	//argv[1]를 아이노티파이 감시 목록에 추가
	wd = inotify_add_watch(i_fd, argv[1], IN_CREATE | IN_DELETE);
	if(wd==-1){
		printf("inotify couldn't add watch\n");
	}
	else{
		printf("inotify watching...\n");
	}

	//패노티파이 설치
        fd = fanotify_init(FAN_CLOEXEC | FAN_CLASS_CONTENT | FAN_NONBLOCK,
                              O_RDONLY | O_LARGEFILE);
        if (fd == -1) {
               perror("fanotify_init");
               exit(EXIT_FAILURE);
           }
	//패노티파이가 감시할 목록은 아이노티파이 이벤트핸들러가 추가해줄 것임.

        nfds=2;
        fds[0].fd=i_fd;
        fds[0].events=POLLIN;
	fds[1].fd=fd;
	fds[1].events=POLLIN;

        printf("listening for events\n");

	//감시 시작
        while(1){
		if(count==1){
			if(fanotify_mark(fd, FAN_MARK_ADD | FAN_MARK_MOUNT, event_mask, AT_FDCWD,usb_path)<0){
                                                printf("mark error\n");
                                                return -1;
                                        }
			else printf("%s is marked\n", usb_path);        
			count--;
		}

                poll_num= poll(fds, nfds, -1);
                if(poll_num==-1){
                        if(errno=EINTR)
                                continue;
                        perror("poll");
                        exit(EXIT_FAILURE);
                }
                if(poll_num>0){

			//아이노티파이가 이벤트가  발생했을 경우
			if(fds[0].revents& POLLIN){
				printf("inofity event caught!\n");
				get_inotify_event(i_fd,fd);

			}
			if(count==1){
				continue;
			}


			//패노티파이 이벤트가 발생했을 경우
                        if(fds[1].revents & POLLIN){
				printf("fan event caught!\n");
                                char buffer[100];
                                ssize_t length;

                                if((length=read(fds[1].fd, buffer, 8192))>0){
                                        struct fanotify_event_metadata* metadata;
                                        metadata= (struct fanotify_event_metadata*) buffer;
                                        while(FAN_EVENT_OK(metadata, length)){
                                                get_fanotify_event(metadata,fd);                                      
                                                if(metadata->fd>0) {close(metadata->fd);}
                                                metadata= FAN_EVENT_NEXT(metadata,length);
                                        }

                                }
				else{
					printf("nothing read\n");
				}

                        }
                }
        }
}


