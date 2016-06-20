## Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

Startes på følgende måte:

```
$ docker run -d \
  -p 80:8080 \
  -v /home/jenkins/.docker/key.pem:/tmp/docker_key \
  -v /home/jenkins/.ssh/id_rsa:/tmp/git_key \
  -v /home/jenkins/.m2/repository:/maven-repo \
  --restart=unless-stopped \
  --name jenkins \
  docker-registry.dmz.local/eid-jenkins
```
