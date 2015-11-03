/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
/// !!!!!!! DO NOT CHANGE THIS FILE:  Any changes made will be removed prior to the project defense !!!!!!!!
/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
elementclass GlobalRouter {
	$address_info |

	// Shared IP input path and routing table
	ip :: Strip(14)
		-> CheckIPHeader
		-> rt :: StaticIPLookup(
			$address_info:ip/32 0,
			192.168.12.0/24 1,
			192.168.10.0/24 192.168.12.1 1,
			192.168.11.0/24 192.168.12.2 1,
			0.0.0.0/0 2);

	// Input and output path (in- & output 0)
	input	-> HostEtherFilter($address_info:eth)
		-> class :: Classifier(12/0806 20/0001, 12/0806 20/0002, 12/0800)
		-> arpr :: ARPResponder($address_info)
		-> output;
	
	arpq :: ARPQuerier($address_info)
		-> output;

	class[1]
		-> [1]arpq;

	class[2]
		-> Paint(1)
		-> ip;

	// Local delivery
	rt	-> Discard;

	// Forwarding path
	rt[1]	-> DropBroadcasts
		-> ipgw :: IPGWOptions($address_info)
		-> FixIPSrc($address_info)
		-> ttl :: DecIPTTL
		-> frag :: IPFragmenter(1500)
		-> arpq;

	ipgw[1]	-> ICMPError($address_info, parameterproblem)
		-> rt;

	ttl[1]	-> ICMPError($address_info, timeexceeded)
		-> rt;

	frag[1]	-> ICMPError($address_info, unreachable, needfrag)
		-> rt;

	// Throw away any packets that are not destined for the defined networks
	rt[2]	-> Discard;
}
