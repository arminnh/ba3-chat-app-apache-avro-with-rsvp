rsvp::RSVPElement -> 
    myipencap::MyIPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) ->
    SetIPChecksum ->
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) ->
    ToDump(test.dump) -> 
    
    Strip(34) ->
    RSVPNode(2.5.8.6) ->
    MyIPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) ->
    SetIPChecksum ->
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) ->
    ToDump(test2.dump) -> 
    
    Strip(34) ->
    RSVPNode(2.5.8.7) ->
    MyIPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) ->
    SetIPChecksum ->
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) ->
    ToDump(test3.dump) -> 
    
    Strip(34) ->
    RSVPNode(2.5.8.8) ->
    MyIPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) ->
    SetIPChecksum ->
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) ->
    ToDump(test4.dump) -> 
    
    Strip(34) ->
    rsvp2::RSVPElement ->
    //Print ->
    Discard
