rsvp::RSVPElement -> 
    myipencap::MyIPEncap(46, 0.0.0.0, 0.0.0.1, TOS 1) ->
    SetIPChecksum ->
    EtherEncap(0x0800, 0:0:0:0:0:0, 0:0:0:1:0:0) ->
    ToDump(test.dump) -> 
    Strip(34) ->
    rsvp2::RSVPElement ->
    Print ->
    Discard
