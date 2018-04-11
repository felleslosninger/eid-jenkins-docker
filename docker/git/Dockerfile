FROM alpine:3.7

RUN apk add --no-cache bash openssh git
RUN ssh-keygen -A
RUN adduser -D -s /usr/bin/git-shell git \
  && echo git:12345 | chpasswd \
  && mkdir /home/git/.ssh
COPY git-shell-commands /home/git/git-shell-commands
COPY sshd_config /etc/ssh/sshd_config
EXPOSE 22
COPY entrypoint.sh /
ENTRYPOINT /entrypoint.sh
