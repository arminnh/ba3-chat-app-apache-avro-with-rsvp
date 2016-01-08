/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
/// !!!!!!! DO NOT CHANGE THIS FILE:  Any changes made will be removed prior to the project defense !!!!!!!!
/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
elementclass BandwidthLimiter {
	$rate |

	input	-> arp_class :: Classifier(12/0806, -)
		-> output;

	arp_class[1]
		-> Queue
		-> shaper :: LinkUnqueue(LATENCY 0, BANDWIDTH $rate)
		-> output;
}
