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
		-> arpq :: ARPQuerier($address)
		-> qos_cl::IPClassifier(tos 0, -)
		-> beQueue::Queue
		-> Shaper(87500)
		-> [1]prio::PrioSched
		-> LinkUnqueue(LATENCY 0, BANDWIDTH 1000kbps)
		-> output;
	
	qos_cl[1]
		-> qosQueue::Queue
		-> Shaper(37500)
		-> [0]prio;

	rsvp_cl[0]
		-> rsvp::RSVPElement($address, AUTORESV false)
		-> rt;
		
	ipgw[1]	-> ICMPError($address, parameterproblem)
		-> qos_cl;

	ttl[1]	-> ICMPError($address, timeexceeded)
		-> qos_cl;

	frag[1]	-> ICMPError($address, unreachable, needfrag)
		-> qos_cl;

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
