#!/bin/bash -xe
 
//generiert einen beliebigen Port zw. 50k und 60k
def randomPort() {
    Random random = new Random() ;
    random.nextInt(60000 - 50000) + 50000
}
 
//filtert allerhand unerwünschten Output damit das yslow.xml Testergebnis ein valides XML ergibt
def filterYSlowXML(stdout) {
    stdout = stdout - "--: not found"
        stdout = stdout - "-i: not found"
        stdout = stdout - "phantomjs is /usr/local/bin/phantomjs"
        stdout = stdout - "FAIL to load undefined"
        stdout = stdout - "FAIL to load undefined"
         
        stdout = stdout.replaceAll('(?m)^[ \t]*\r?\n', '');
        return stdout;
}
 
node {
    //DOCKERHOST_IP muss mit der IP Adresse des des Hosts angepasst werden
    def dockerHOST = "127.0.0.1"
     
    //URL zu GIT
    def gitUrl = "http://192.168.99.100:10080/bob/bobbuilderme.git"
     
    //diverse Docker Images
    def maven = docker.image("maven:3.3.9-jdk-8"); // https://registry.hub.docker.com/_/maven/
    def nginx = docker.image("nginx:alpine");
    def yslow = docker.image("tmaier/yslow:latest");
 
    stage("SCM") {
        git gitUrl
    }
 
    stage("Build") {
        //Maven Image holen
        maven.pull()
 
        //das Bauen läuft mit Hilfe eines Maven Docker Containers der lediglich den Maven Prozess zur Verfügung stellt
        //ohne root läuft kein npm ! deswegen mit "-u root"
        maven.inside("-u root") {
            //das lokal Maven Repo liegt im Workspace unter /var/jenkins_home/workspace/<JOB>@tmp/m2repo
            //solange der Workspace nicht gelöscht wird bleibt auch das Repo erhalten
            sh "mvn -Dmaven.repo.local=${pwd tmp: true}/m2repo -B clean package"
        }
    }
 
    def bobbuildermeImg
    stage("Docker") {
        nginx.pull()
        bobbuildermeImg = docker.build("cd-workshop/bobbuilderme:${env.BUILD_TAG}", ".")
    }
     
    stage("Test") {
         
        dockerHOST = input(
         id: "userInput", message: "Docker Host IP ?", parameters: [
         [$class: "TextParameterDefinition", defaultValue: "127.0.0.1", description: "Docker Host IP", name: "ip"]
        ])
        echo ('Dockerhost: '+dockerHOST)
 
        def port = randomPort().toString();
         
        //YSlow Test Image holen
        yslow.pull()
         
         
        //bobbuilder Image starten
        //"bob" als Referenz auf den Containers
        //ausserhalb des Containers aber in dessen Kontext YSlow Container starten und Test durchführen
        bobbuildermeImg.withRun("--name bobbuilder -p "+port+":80") {bob ->;        
             
            def stdout = sh(script: "docker run --rm tmaier/yslow:latest phantomjs /tmp/yslow.js -i basic  -f junit --threshold '{'overall': 'B', 'ycdn': 0 }' "+dockerHOST+":"+port, returnStdout: true)
             
            //output filtern
            stdout = filterYSlowXML(stdout)
            //in Datei schreiben
            writeFile file: 'yslow.xml', text: stdout
            //Testergebnisse veröffentlichen
            junit allowEmptyResults: true, testResults: "yslow.xml"
             
            //Warten auf Input
            //hier wäre manuelles Testen möglich
            input 'Anwendung in Ordnung - http://' + dockerHOST + ':' + port + ' ?'
        }
    }
     
}