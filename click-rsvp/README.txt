Required packages:
    click version 2.0.1
    telnet
    
To install the necessary components:
    *) cd into your click installation (configured with --disable-linuxmodule --enable-local --enable-etherswitch)
    *) copy the contents from local/ to elements/local/
    *) execute "make elemlist"
    *) execute "make"

Run the "test.click" file with:
> click test.click -p 12345

Then execute 
> telnet localhost 12345
Then copy the text in "test.txt" into the terminal where telnet is running.

Click will generate RSVP packets which will be printed in the terminal where click is running.
The can also be found in the "test.dump" file, for reading with wireshark.
