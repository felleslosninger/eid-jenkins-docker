## Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

Startes på følgende måte:

```
$ docker run -d \
  -p 80:8080 \
  -v ~jenkins/.docker/key.pem:/tmp/docker_key \
  -v ~jenkins/.ssh/id_rsa:/tmp/git_key \
  -v ~jenkins/.m2/repository:/maven-repo \
  -e uid=`id -u jenkins` \
  -e gid=`id -g jenkins` \
  --restart=unless-stopped \
  --name jenkins \
  docker-registry.dmz.local/eid-jenkins
```
