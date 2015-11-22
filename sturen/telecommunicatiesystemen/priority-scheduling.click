AddressInfo(sourceAddr 10.0.0.1 1A:7C:3E:90:78:41)

//Generate icmp ping packets, these will get priotity
source::ICMPPingSource(sourceAddr, 10.0.0.2);

//Generate random packets
source2::RandomSource(64);

//Priority scheduling element
prio::PrioSched;

source
    -> Queue
    -> [0]prio
	
source2
    //Encapsulate the random packets and send them to the network
	-> UDPIPEncap(sourceAddr, 1234, 10.0.0.2, 1234)
	-> [1]prio

prio
    -> Shaper(2)
    -> Unqueue
	-> IPPrint
	-> ToDump(priority-scheduling.dump)
	-> Discard



