## RSVP in Click

Implementation of the Resource ReSerVation Protocol (RFCs [2205](telecommunications-systems/rfc2205.pdf) and [2210](telecommunications-systems/rfc2210.pdf)) in userlevel [Click](http://read.cs.ucla.edu/click/click). Admission control was not implemented.  

## Requirements
* Click version 2.0.1  
* Telnet
    
## Install
extract click-2.0.1.tar.gz and cd into click-2.0.1
``` 
./configure --disable-linuxmodule --enable-local --enable-etherswitch
``` 
make sure that custom elements are in click-2.0.1/elements/local/
``` 
make elemlist 
make
```
    
## RSVP test
Run the["telecommunications-systems/scripts/ipnetwork-local.click](./scripts/ipnetwork-local.click) file with:
``` 
click telecommunications-systems/scripts/custom/ipnetwork-local.click -p 10000
``` 
Then, in another window
```  
telnet localhost 10000  
``` 
Copy the "Local" handler calls from [telecommunications-systems/scripts/custom/test-ipnetwork-local.txt](./scripts/custom/test-ipnetwork.txt) into the telnet window. Copy the blocks one by one, not simultaneously.

Confirm that data is being sent with Quality of Service.
``` 
telecommunications-systems/scripts/read_statistics.sh  
```  
![rsvp](./rsvp.png)

## Handlers
You can call handlers yourself in telnet, the following ones are available:  

    RSVP:
        To setup messages:
                 write session          DEST               PROTOCOL POLICE            PORT
                 write hop              NEIGHBOR           LIH
                 write errorspec        ERROR_NODE_ADDRESS INPLACE  NOTGUILTY         ERROR_CODE        ERROR_VALUE
                 write timevalues       REFRESH
                 write resvconfobj      RECEIVER_ADDRESS
                 write scope            SRC_ADDRESS
                 write senderdescriptor SRC_ADDRESS        SRC_PORT TOKEN_BUCKET_RATE TOKEN_BUCKET_SIZE PEAK_DATA_RATE MINIMUM_POLICED_UNIT MAXIMUM_PACKET_SIZE
        
        To send messages:
                 write path     TTL
                 write resv     TTL
                 write patherr  TTL
                 write resverr  TTL
                 write pathtear TTL
                 write resvtear TTL
                 write resvconf TTL
        
    MyIPEncap:
        To change the tos byte:
            write tos int
            
    
            
#### Types of variables

    IPAddress: DEST, NEIGHBOR, ERROR_NODE_ADDRESS, RECEIVER_ADDRESS, SRC_ADDRESS, 
    Unsigned:  PROTOCOL, PORT, LIH, ERROR_CODE, ERROR_VALUE, REFRESH, SRC_PORT, MINIMUM_POLICED_UNIT, MAXIMUM_PACKET_SIZE
    Bool:      POLICE, INPLACE, NOTGUILTY
    Double:    TOKEN_BUCKET_RATE, TOKEN_BUCKET_SIZE, PEAK_DATA_RATE


#### Example
Run the [test.click](./scripts/custom/test.click) file with:
``` 
click telecommunications-systems/scripts/custom/test.click -p 12345
``` 

Then 
``` 
telnet localhost 12345
``` 
Copy the handler calls from [telecommunications-systems/scripts/custom/test.txt](./scripts/custom/test.txt) into the terminal where telnet is running.  
This will make Click generate RSVP packets which will be printed in the terminal where click is running.  
They can also be found in the "test.dump" file, for reading with wireshark.
