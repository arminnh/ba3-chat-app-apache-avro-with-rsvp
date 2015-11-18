RSVPElement -> 
    IPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) -> 
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) -> 
    ToDump(test.dump) -> 
    Discard
