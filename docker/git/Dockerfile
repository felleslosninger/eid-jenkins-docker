FROM alpine:3.7

RUN apk add --no-cache bash openssh git
COPY ssh/* /etc/ssh/
RUN chmod 0400 /etc/ssh/ssh_host_*_key
RUN adduser -D -s /usr/bin/git-shell git \
  && echo git:12345 | chpasswd \
  && mkdir /home/git/.ssh
COPY git-shell-commands /home/git/git-shell-commands
EXPOSE 22
COPY entrypoint.sh /
ENTRYPOINT /entrypoint.sh
