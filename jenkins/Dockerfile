# Jenkins with Java 17 (to build BookNest) and the Docker command line (to build and push its images).
FROM jenkins/jenkins:lts-jdk17

# the Docker command line and its compose plugin, copied from Docker's official image
# (single programs, so no package repository is needed)
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

# the plugins the BookNest pipeline uses, installed when the image is built
RUN jenkins-plugin-cli --plugins git workflow-aggregator docker-workflow junit credentials-binding ssh-agent blueocean
