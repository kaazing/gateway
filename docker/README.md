# Docker

This project builds a [Docker image](https://docs.docker.com/userguide/dockerimages/).  This project does not build by default.  Activate 
it with a "docker" profile. This project releases to [docker hub kaazing/unstable-gateway](https://registry.hub.docker.com/u/kaazing/unstable-gateway/)

### Prerequisites to builds

Docker must be [installed](https://docs.docker.com/installation/) and the `docker` command must be working from the terminal where you run `mvn`.

### Prerequisites to releases

To release you must have push rights to the [kaazing docker hub organization](https://registry.hub.docker.com/repos/kaazing/).  You must also be logged in on docker. To log in run the command `docker login`.

### Usage

You can launch a container using the docker image produced by this build by running 

```bash
docker run --rm -p 8080:8080 -h <hostname> kaazing/unstable-gateway:develop-SNAPSHOT
```

where hostname points to your [docker host ip address](https://docs.docker.com/articles/basics/#bind-docker-to-another-hostport-or-a-unix-socket).  You will then want to add the <hostname> and docker host ip address to your etc/hosts.
