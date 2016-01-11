## Telecommunications + Distributed Systems Project
Integrated project for [Telecommunications systems](https://www.uantwerpen.be/popup/opleidingsonderdeel.aspx?catalognr=1001WETTEL&taal=en&aj=2015) and [Distributed Systems courses](https://www.uantwerpen.be/popup/opleidingsonderdeel.aspx?catalognr=1001WETIGS&taal=en&aj=2015) at the University of Antwerp.   
For more specific information, see READMEs in subdirectories.


## Distributed Systems: Chat app with Apache Avro
chat-app is a CLI java application which allows users to communicate with each other using a server-clients architecture (with [Apache Avro](https://avro.apache.org/)).   
Using the application, clients can chat with one another in public or private chat mode. 
Video streaming is also supported. Features which are implemented are listed in features.txt. 
More information about the implementation can be found in architecture.txt.
  
![video-stream-gif](distributed-systems/video-streaming.gif)


## Telecommunications systems: RSVP in Click

Implementation of the Resource ReSerVation Protocol (RFCs [2205](telecommunications-systems/rfc2205.pdf) and [2210](telecommunications-systems/rfc2210.pdf)) in userlevel [Click](http://read.cs.ucla.edu/click/click). Admission control was not implemented.  
![rsvp](telecommunications-systems/rsvp.png)


## Integration

A video session in the chat-app will, using custom components in Click, set up a path reservation with RSVP. The reservation can also be set up outside of the chat-app.  
Videos can be streamed with Quality of Service. Video streaming is possible in one or in both ways between two clients.  
To make the QoS reservation work, certains scripts need to be running before the Chat App Clients (see list of steps).
The application will still work without QoS if these scripts are not running.


#### Steps to test integration (using 2 PCs)
We use Click scripts (in telecommunications-systems/scripts) which create a virtual network that is limited to 1Mbps. Videos can be streamed with Quality of Service over this virtual network.

1. Run setup_ds_server.sh on PC 1. Then, run host2.click -p 10000.
2. Run AppServer on PC 1.
3. Run AppClient on PC 1.
  * Server IP = 192.168.11.1, Client IP = 192.168.11.1
4. Run setup_click.sh on PC 2. Then, run ipnetwork.click -p 10000.
5. Run AppClient on PC 2. 
  * Server IP = 192.168.11.1, Client IP = 192.168.10.1
6. Start a private chat session between the two AppClient instances.
7. Request a reservation (= send a path message), write "?videoRequest" or "?vr".
8. Accept a reservation (= send a resv message and construct reservation path), write "?acceptVideo" or "?av".
9. Select a video to stream.
10. Generate background traffic from PC 2 to PC 1
   * iperf -c 192.168.11.1 -l 9999MB -u -b 500K
11. Confirm that the video is streamed with Quality of Service by executing read_statistics.sh on PC 2.
   * On PC 2 because read_statistics.sh looks for Click elements in ipnetwork.click


### Authors
* [Josse Coen](https://github.com/jsscn)
* [Armin Halilovic](https://github.com/arminnh)
