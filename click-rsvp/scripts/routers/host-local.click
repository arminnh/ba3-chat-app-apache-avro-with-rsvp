// Output configuration: 
//
// Packets for the network are put on output 0
// Packets for the host are put on output 1
elementclass Host {
	$address, $gateway |

	// Shared IP input path
	ip :: Strip(14)
		-> CheckIPHeader
		-> RSVPToSSetter(rsvp)
		-> rsvp_cl::IPClassifier(proto 46, -)[1]
		-> rt :: StaticIPLookup(
			$address:ip/32 0,
			$address:ipnet 1,
			0.0.0.0/0 $gateway 1)
		-> [1]output;

	rt[1]
		-> ipgw :: IPGWOptions($address)
		-> FixIPSrc($address)
		-> ttl :: DecIPTTL
		-> frag :: IPFragmenter(1500)
		-> qos_cl::IPClassifier(tos 0, -)
		-> beQueue::Queue
		-> Shaper(700000)
		-> [1]prio::PrioSched
		-> LinkUnqueue(LATENCY 0, BANDWIDTH 1000kbps)
		-> arpq :: ARPQuerier($address)
		-> output;
	
	qos_cl[1]
		-> qosQueue::Queue
		-> Shaper(300000)
		-> [0]prio;

	rsvp_cl[0]
		-> rsvp::RSVPElement($address, AUTORESV true)
		-> rt;
		
	ipgw[1]	-> ICMPError($address, parameterproblem)
		-> output;

	ttl[1]	-> ICMPError($address, timeexceeded)
		-> output;

	frag[1]	-> ICMPError($address, unreachable, needfrag)
		-> output;

	// incoming packets
	input	-> HostEtherFilter($address)
		-> in_cl :: Classifier(12/0806 20/0001, 12/0806 20/0002, 12/0800)
		-> arp_res :: ARPResponder($address)
		-> output;

	in_cl[1]
		-> [1]arpq;

	in_cl[2]
		-> ip ;
}
