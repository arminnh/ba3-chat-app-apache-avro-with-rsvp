// Router with two interfaces
// The input/output configuration is as follows:
//
// Input:
//	[0]: packets received on the LAN interface
//	[1]: packets received on the WAN interface
//
// Output:
//	[0]: packets sent to the LAN interface
//	[1]: packets sent to the WAN interface

elementclass Router {
	$lan_address, $wan_address, $default_gw |

	// Shared IP input path and routing table
	ip :: Strip(14)
		-> CheckIPHeader
		-> rt :: StaticIPLookup(
			$lan_address/32 0,
			$wan_address/32 0,
			$lan_address:ipnet 1,
			$wan_address:ipnet 2,
			0.0.0.0/0 $default_gw 2);

	// ARP responses are copied to each ARPQuerier.
	arpt :: Tee(2);
	
	// Input and output paths for the LAN interface
	input[0]
		-> HostEtherFilter($lan_address)
		-> lan_class :: Classifier(12/0806 20/0001, 12/0806 20/0002, 12/0800)
		-> lan_arpr :: ARPResponder($lan_address)
		-> [0]output;

	lan_arpq :: ARPQuerier($lan_address)
		-> [0]output;

	lan_class[1]
		-> arpt[0]
		-> [1]lan_arpq;

	lan_class[2]
		-> Paint(1)
		-> ip;

	// Input and output paths for the WAN interface
	input[1]
		-> HostEtherFilter($wan_address)
		-> wan_class :: Classifier(12/0806 20/0001, 12/0806 20/0002, 12/0800)
		-> wan_arpr :: ARPResponder($wan_address)
		-> [1]output;

	wan_arpq :: ARPQuerier($wan_address)
		-> [1]output;

	wan_class[1]
		-> arpt[1]
		-> [1]wan_arpq;

	wan_class[2]
		-> Paint(2)
		-> ip;

	// Local delivery
	rt[0]	-> Discard;

	// Forwarding path for LAN interface
	rt[1]	-> DropBroadcasts
		-> lan_paint :: PaintTee(1)
		-> lan_ipgw :: IPGWOptions($lan_address)
		-> FixIPSrc($lan_address)
		-> lan_ttl :: DecIPTTL
		-> lan_frag :: IPFragmenter(1500)
		-> [0]lan_arpq;

	lan_paint[1]
		-> ICMPError($lan_address, redirect, host)
		-> rt;

	lan_ipgw[1]
		-> ICMPError($lan_address, parameterproblem)
		-> rt;

	lan_ttl[1]
		-> ICMPError($lan_address, timeexceeded)
		-> rt;

	lan_frag[1]
		-> ICMPError($lan_address, unreachable, needfrag)
		-> rt;

	// Forwarding path for WAN interface
	rt[2]	-> DropBroadcasts
		-> wan_paint :: PaintTee(2)
		-> wan_ipgw :: IPGWOptions($wan_address)
		-> FixIPSrc($wan_address)
		-> wan_ttl :: DecIPTTL
		-> wan_frag :: IPFragmenter(1500)
		-> [0]wan_arpq;

	wan_paint[1]
		-> ICMPError($wan_address, redirect, host)
		-> rt;

	wan_ipgw[1]
		-> ICMPError($wan_address, parameterproblem)
		-> rt;

	wan_ttl[1]
		-> ICMPError($wan_address, timeexceeded)
		-> rt;

	wan_frag[1]
		-> ICMPError($wan_address, unreachable, needfrag)
		-> rt;
}

