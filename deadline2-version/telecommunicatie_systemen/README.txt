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
You can call handlers yourself in telnet, the following ones are available in RSVPElement, the RSVP element used in hosts:
    At the end of this file there is a list of the types of the following variables.
    RSVP:
    	These handlers are used to set the parameters:
                 write session          DEST               PROTOCOL POLICE            PORT
                 write hop              NEIGHBOR           LIH
                 write errorspec        ERROR_NODE_ADDRESS INPLACE  NOTGUILTY         ERROR_CODE        ERROR_VALUE
                 write senderdescriptor SRC_ADDRESS        SRC_PORT TOKEN_BUCKET_RATE TOKEN_BUCKET_SIZE PEAK_DATA_RATE MINIMUM_POLICED_UNIT MAXIMUM_PACKET_SIZE
                 write flowdescriptor SRC_ADDRESS        SRC_PORT TOKEN_BUCKET_RATE TOKEN_BUCKET_SIZE PEAK_DATA_RATE MINIMUM_POLICED_UNIT MAXIMUM_PACKET_SIZE

	    These handlers are used to confirm the set parameters and to start sending messages:
                 write path
                 write resv
                 write patherr
                 write resverr
                 write pathtear
                 write resvtear
                 write resvconf

        These handlers can be used to read the current path and resv state inside RSVP elements:
                read pathstatetable (also available in RSVPNode)
                read resvstatetable (also available in RSVPNode)
                read senderstable
                read reservationstable
        where senderstable and reservationstable are those paths and reservations initiated on the element, and pathstatetable and resvstatetable are the path and resv state received from other nodes.

For example, to initiate a sender, call the following handlers: session, senderdescriptor, path.
For path messages and pathtear, specify session and senderdescriptor, followed by their respective message handlers.
For resv and resvtear, specify session and flowdescriptor.
For patherr, specify session and errorspec.
For resverr, specify session, errorspec and flowdescriptor.

The path and resv handlers will setup timers to periodically send messages, except if the REFRESH parameter is explicitly set to false.

Types of variables
-------------------------------------
IPAddress: DEST, NEIGHBOR, ERROR_NODE_ADDRESS, RECEIVER_ADDRESS, SRC_ADDRESS, 
Unsigned:  PROTOCOL, PORT, LIH, ERROR_CODE, ERROR_VALUE, REFRESH, SRC_PORT, MINIMUM_POLICED_UNIT, MAXIMUM_PACKET_SIZE
Bool:      POLICE, INPLACE, NOTGUILTY
Double:    TOKEN_BUCKET_RATE, TOKEN_BUCKET_SIZE, PEAK_DATA_RATE
