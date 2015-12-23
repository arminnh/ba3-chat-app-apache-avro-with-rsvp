elementclass Host {
	$address, $interface, $gateway |

	// Linux will send ARP queries to the fake device.
	// You must respond to these queries in order to receive any IP packets,
	// but you can obviously respond with any Ethernet address you'd like
	FromHost($interface)
		-> SetTimestamp
		-> fh_class :: Classifier(12/0806, 12/0800)
		-> ARPResponder(0.0.0.0/0 0:0:0:0:0:1)
		-> th :: ToHost($interface);

	// Shared IP input path
	ip :: Strip(14)
		-> CheckIPHeader
		-> rsvp_cl::IPClassifier(proto 46, -)[1]
		-> rt :: StaticIPLookup(
			$address:ip/32 0,
			$address:ipnet 1,
			0.0.0.0/0 $gateway 1)
		-> EtherEncap(0x0800, 0:0:0:0:0:1, $address)
		-> th;

	fh_class[1]
		-> ip;

	rt[1]	-> ipgw :: IPGWOptions($address)
		-> FixIPSrc($address)
		-> ttl :: DecIPTTL
		-> frag :: IPFragmenter(1500)
		-> arpq :: ARPQuerier($address)
		-> output;

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

	rsvp_cl[0]
		-> rsvp::RSVPElement($address)
		-> EtherEncap(0x0800, $address, $gateway)
		-> output;

	in_cl[1]
		-> [1]arpq;

	in_cl[2]
		-> ip;
}
