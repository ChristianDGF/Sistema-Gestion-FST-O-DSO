FROM jenkins/jenkins:lts-jdk21

USER root

# Docker CLI (talks to the host's Docker daemon via the mounted socket - no
# Docker-in-Docker needed) + Node.js 22 (matches ci.yml's node-version) for
# the frontend build/lint stages.
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && chmod a+r /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get update && apt-get install -y --no-install-recommends \
      docker-ce-cli nodejs \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

# ws-cleanup provides the cleanWs() step used in the pipeline's post actions.
RUN jenkins-plugin-cli --plugins \
      git \
      workflow-aggregator \
      docker-workflow \
      nodejs \
      sonar \
      credentials-binding \
      pipeline-stage-view \
      ws-cleanup \
      blueocean
