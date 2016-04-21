# Docker

This project builds a [Docker image](https://docs.docker.com/userguide/dockerimages/).  It does not build by default, you must activate it with the `docker` profile (see below). This project releases to [docker hub kaazing/gateway](https://registry.hub.docker.com/u/kaazing/gateway/)

### Prerequisites to build

Docker must be [installed](https://docs.docker.com/installation/) and the `docker` command must be working from the terminal where you run `mvn`.

### Prerequisites to release

To release you must have push rights to the [kaazing docker hub organization](https://registry.hub.docker.com/repos/kaazing/).  You must also be logged in on docker. To log in run the command `docker login`.

If you don't have privileges, you can still build this project.

### Building

To build a Docker container locally, specify the `docker` profile:

`mvn clean install -Pdocker`

### Usage

You can launch a container using the docker image produced by this build by following these steps:

0. Add `gateway` to your `/etc/hosts`, pointing to the IP address of your Docker host:

    > 192.168.99.100 gateway

0. Run the Docker container:

    ```bash
docker run --rm -p 8000:8000 -h gateway kaazing/gateway:develop-SNAPSHOT
```

0. Using a browser, open `http://gateway:8000` to see the welcome page.