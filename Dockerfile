FROM jenkinsci/jenkins:2.42
MAINTAINER Robert Lachner - heinzepreller@gmail.com
# Quellen
# http://container-solutions.com/running-docker-in-jenkins-in-docker/
# https://renzedevries.wordpress.com/2016/06/30/building-containers-with-docker-in-docker-and-jenkins/

# get jenkins Plugins from script console
#Jenkins.instance.pluginManager.plugins.each{
#  plugin -> 
#    println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
#}

# run
# docker run -d --restart=unless-stopped --name=buildserver-jenkins -p 8080:8080 -p 50000:50000 -v /var/run/docker.sock:/var/run/docker.sock -v $(which docker):/usr/bin/docker:ro -v /var/docker-volumes/jenkins:/var/jenkins_home heinzepreller/jenkins:2.41

# Change Context
USER root


RUN apt-get update && apt-get install -y sudo supervisor && rm -rf /var/lib/apt/lists/*
RUN echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
 
USER jenkins

#Umgebungsvariablen ausgeben
RUN printenv

#ins root Folder wechseln
RUN cd /

#komplette Folderstruktur im Dockerfile Folder als chroot ins Image kopieren
COPY . /

# alle Plugins runterladen wie in plugins.txt angegeben
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt

# supervisord
USER root

# Create log folder for supervisor and jenkins
RUN mkdir -p /var/log/supervisor
RUN mkdir -p /var/log/jenkins

# Start supervisord when running the container
CMD /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf