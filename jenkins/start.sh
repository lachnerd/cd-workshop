#!/bin/bash
#um Docker ohne sudo im Container zugänglich zu machen
sudo chmod 777 /var/run/docker.sock
# Jenkins_Master Container starten
docker-compose up -d