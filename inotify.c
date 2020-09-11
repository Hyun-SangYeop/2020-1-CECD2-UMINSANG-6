#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <limits.h>
#include <unistd.h>
#include <pwd.h>
  
#define MAX_EVENTS 1024 /*Max. number of events to process at one go*/
#define LEN_NAME 1024 /*Assuming length of the filename won't exceed 16 bytes*/
#define EVENT_SIZE  ( sizeof (struct inotify_event) ) /*size of one event*/
#define BUF_LEN     ( MAX_EVENTS * ( EVENT_SIZE + LEN_NAME )) /*buffer to store the data of events*/


const char* get_Monitored_Path(char* path){
        uid_t uid= geteuid();
        struct passwd* pw= getpwuid(uid);
        if(!pw){
                printf("pw's not right\n");
                return "";
        }
        sprintf(path,"/media/%s",pw->pw_name);
        return path;
}

void get_event (int fd) {
    char buffer[BUF_LEN];
    int length, i = 0;
 
    length = read( fd, buffer, BUF_LEN );  
    if ( length < 0 ) {
        perror( "read" );
    }  
  
    while ( i < length ) {
        struct inotify_event *event = ( struct inotify_event * ) &buffer[ i ];
        if ( event->len ) {
            if ( event->mask & IN_CREATE) {
                if (event->mask & IN_ISDIR)
                    printf( "The directory %s was Created.\n", event->name );       
                else
                    printf( "The file %s was Created with WD %d\n", event->name, event->wd );       
            }
            
            if ( event->mask & IN_MODIFY) {
                if (event->mask & IN_ISDIR)
                    printf( "The directory %s was modified.\n", event->name );       
                else
                    printf( "The file %s was modified with WD %d\n", event->name, event->wd );       
            }
            
            if ( event->mask & IN_DELETE) {
                if (event->mask & IN_ISDIR)
                    printf( "The directory %s was deleted.\n", event->name );       
                else
                    printf( "The file %s was deleted with WD %d\n", event->name, event->wd );       
            }  
            i += EVENT_SIZE + event->len;
        }
    }
}
 
int main( int argc, char **argv ) {
    int wd, fd;
    char m_path[100];
    get_Monitored_Path(m_path);
   
    fd = inotify_init();
    if ( fd < 0 ) {
        perror( "Couldn't initialize inotify");
    }
  
    wd = inotify_add_watch(fd, m_path, IN_CREATE | IN_MODIFY | IN_DELETE); 
    if (wd == -1) {
        printf("Couldn't add watch to %s\n",m_path);
    } else {
        printf("Watching::%s\n",m_path);
    }
  
    /* do it forever*/
    while(1) {
        get_event(fd); 
    } 
 
    /* Clean up*/
    inotify_rm_watch( fd, wd );
    close( fd );
    
    return 0;
}
