
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <linux/ioctl.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <errno.h>
#include <unistd.h>
#include <jni.h> 
#include <sys/ioctl.h>

#include "org_idea_net_TunTap.h"

JNIEXPORT jint JNICALL Java_org_idea_net_TunTap_open(JNIEnv *env, jclass this, jstring dev) {
  int fd;
  struct ifreq ifr;
  const char *tap;

  if ((fd = open("/dev/net/tun", O_RDWR)) < 0) {
    perror("Failed to open /dev/net/tun");
    return fd;
  } 

  memset(&ifr, 0, sizeof(ifr));
  tap = (*env)->GetStringUTFChars(env, dev, 0);
  memcpy(ifr.ifr_name, tap, strlen(tap));
  (*env)->ReleaseStringUTFChars(env, dev, tap);

  ifr.ifr_flags = IFF_TAP | IFF_NO_PI;
  if (ioctl(fd, TUNSETIFF, (void*) &ifr) < 0) {    
    perror("Failed to open tap");
    close(fd);
    return -1;
  }

  return fd;
}

JNIEXPORT void JNICALL Java_org_idea_net_TunTap_close(JNIEnv *env, jclass this, jint fd) {
  close(fd);
}

JNIEXPORT jint JNICALL Java_org_idea_net_TunTap_write(JNIEnv *env, jclass this, jint fd, jobject buf, jint len) {
  char* b = (char *)(*env)->GetDirectBufferAddress(env, buf);
  
  return write(fd, b, len);
}

JNIEXPORT jboolean JNICALL Java_org_idea_net_TunTap_await(JNIEnv *env, jclass this, jint fd, jlong timeout) {
  fd_set set;
  struct timeval to;

  FD_ZERO(&set);
  FD_SET(fd, &set);

  to.tv_sec = timeout / 1000;
  to.tv_usec = (timeout % 1000) * 1000;

  return select(fd + 1, &set, NULL, NULL, &to);
}

JNIEXPORT jint JNICALL Java_org_idea_net_TunTap_read(JNIEnv *env, jclass this, jint fd, jobject buf) {
  char* b = (char *)(*env)->GetDirectBufferAddress(env, buf);
  jlong capacity = (*env)->GetDirectBufferCapacity(env, buf);

  return read(fd, b, capacity);
}
