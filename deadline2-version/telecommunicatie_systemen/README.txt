=====================================
|           RSVP in Click           |
=====================================


Requirements
-------------------------------------
    click version 2.0.1
    telnet
    
    
Install
-------------------------------------
    *) cd into your click installation
    *) make sure your click installation is configured with "--disable-linuxmodule --enable-local --enable-etherswitch"
    *) copy the contents from local/ to elements/local/
    *) execute "make elemlist"
    *) execute "make"


Handlers
-------------------------------------
You can call handlers yourself in telnet, the following ones are available:
    At the end of this file there is a list of the types of the following variables.
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
            
            
Types of variables
-------------------------------------
IPAddress: DEST, NEIGHBOR, ERROR_NODE_ADDRESS, RECEIVER_ADDRESS, SRC_ADDRESS, 
Unsigned:  PROTOCOL, PORT, LIH, ERROR_CODE, ERROR_VALUE, REFRESH, SRC_PORT, MINIMUM_POLICED_UNIT, MAXIMUM_PACKET_SIZE
Bool:      POLICE, INPLACE, NOTGUILTY
Double:    TOKEN_BUCKET_RATE, TOKEN_BUCKET_SIZE, PEAK_DATA_RATE
