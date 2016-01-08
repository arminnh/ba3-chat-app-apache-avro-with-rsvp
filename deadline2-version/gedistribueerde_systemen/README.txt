=====================================
| Chat application with Apache Avro |
=====================================

chat-app is a CLI java application which allows users to communicate with each other using a server-clients architecture (with Apache Avro).
Using the application, users can chat with one another in public or private chat mode. Video streaming is also supported.
Features which are implemented are listed in features.txt. More information about the implementation can be found in architecture.txt.


Requirements
-------------------------------------
The following files (which are deliverd with this package) need to be included in the build path of the program:
    avro-1.7.7.jar:
    avro-ipc-1.7.7.jar
    jackson-core-asl-1.9.13.jar
    jackson-mapper-asl-1.9.13.jar
    slf4j-api-1.7.7.jar
    slf4j-simple-1.7.7.jar
     
        
Install
-------------------------------------
You can compile the program yourself or just use eclipse to get the job done.
   
    With Eclipse
    ---------------------------------
    Go to File->Import.
    Select "File System" as import source.
    Choose the chat-app-eclipse-export/ directory.
    Click "Select All"
    Choose a folder to import into.
    Click "Finish".
  
    
Run
-------------------------------------
The program can be run from 1 or multiple computers as follows:
    1) Run AppServer.
    2) Run AppClient.
        Appclient will prompt you to input a username and 2 IP addresses.
        The first IP address is the IP address of the machine that AppServer is running on (0.0.0.0 will work if you run both executables on 1 machine). 
        The second IP address is the IP address of the  machine that AppClient is running on (again, 0.0.0.0 if on 1 machine).
    3) Repeat step 2 to have as many users as you wish.
    4) You can now use the program on each running AppClient.
        To get a list of available commands, type "?list".
        To quit, type "exit".
