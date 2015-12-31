/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
/// !!!!!!! DO NOT CHANGE THIS FILE:  Any changes made will be removed prior to the project defense !!!!!!!!
/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

///======================================================================///
/// This script implements a small IP network inside click. Several	 ///
/// compound elements are used to create the different network entities. ///
///									 ///
/// Authors: Bart Braem & Johan Bergs					 ///
///======================================================================///

// Define imports
require(library routers/bandwidthlimiter.click)
require(library routers/globalrouter.click)
require(library routers/host-local.click)
require(library routers/router.click)

// Address configuration
AddressInfo(host1_address 192.168.10.1/24 00:00:00:00:10:01)
AddressInfo(host2_address 192.168.11.1/24 00:00:00:00:11:01)
AddressInfo(global_router_address 192.168.12.254/24 00:FF:FF:FF:FF:FF);
AddressInfo(router1_lan_address 192.168.10.254/24 00:00:00:00:10:FE);
AddressInfo(router1_wan_address 192.168.12.1/24 00:00:00:00:12:01);
AddressInfo(router2_lan_address 192.168.11.254/24 00:00:00:00:11:FE);
AddressInfo(router2_wan_address 192.168.12.2/24 00:00:00:00:12:02);


// Host/router instantiation
global_router :: GlobalRouter(global_router_address);
router1 :: Router(router1_lan_address, router1_wan_address, global_router_address);
router2 :: Router(router2_lan_address, router2_wan_address, global_router_address);
host1 :: Host(host1_address, router1_lan_address);
host2 :: Host(host2_address, router2_lan_address);
network1 :: ListenEtherSwitch;
network2 :: ListenEtherSwitch;
cloud :: ListenEtherSwitch;

// Connect the host to the network
// While doing this, count the amount of QoS and BE traffic received and transmitted by each host
host1	-> Queue -> LinkUnqueue(0, 1000kbps) -> host1_tx_tee :: Tee(2)
	-> host1link :: BandwidthLimiter(1000kbps)
	-> [1]network1[1]
	-> host1_rx_tee :: Tee(2)
	-> host1;

// Count the TX and RX traffic
host1_tx_tee[1]
	-> Classifier(12/0800)
	-> host1_tx_qos_cl :: IPClassifier(ip tos 0, -)
	-> host1_tx_be_ctr :: AverageCounter
	-> Discard;
host1_tx_qos_cl[1]
	-> host1_tx_qos_ctr :: AverageCounter
	-> Discard;
host1_rx_tee[1]
	-> HostEtherFilter(host1_address:eth)
	-> Classifier(12/0800)
	-> host1_rx_qos_cl :: IPClassifier(ip tos 0, -)
	-> host1_rx_be_ctr :: AverageCounter
	-> Discard;
host1_rx_qos_cl[1]
	-> host1_rx_qos_ctr :: AverageCounter
	-> Discard;

host1[1]
	-> Discard;

host2	-> [1]network2[1]
	-> host2;

host2[1]
	-> Discard;

// Connect the routers to the network
global_router
	-> cloud
	-> global_router;
router1	-> router1lanlink :: BandwidthLimiter(1000kbps)
	-> network1
	-> router1;
router1[1]
        -> router1wanlink :: BandwidthLimiter(1000kbps)
        -> [1]cloud[1]
        -> [1]router1;

router2	-> router2lanlink :: BandwidthLimiter(1000kbps)
	-> router2_tx_tee :: Tee(2)
	-> network2
	-> router2;
router2[1]
	-> router2wanlink :: BandwidthLimiter(1000kbps)
	-> [2]cloud[2]
	-> router2_rx_tee :: Tee(2)
	-> [1]router2;

// Again count the traffic, but this time on router2
router2_rx_tee[1]
	-> HostEtherFilter(router2_lan_address:eth)
	-> Classifier(12/0800)
	-> MarkIPHeader(14)
	-> router2_rx_qos_cl :: IPClassifier(ip tos 0, -)
	-> router2_rx_be_ctr :: AverageCounter
	-> Discard;
router2_rx_qos_cl[1]
	-> router2_rx_qos_ctr :: AverageCounter
	-> Discard;
router2_tx_tee[1]
	-> router2_tx_qos_cl :: IPClassifier(ip tos 0, -)
	-> router2_tx_be_ctr :: AverageCounter
	-> Discard;
router2_tx_qos_cl[1]
	-> router2_tx_qos_ctr :: AverageCounter
	-> Discard;

// Create pcap dump files
network1[2]
	-> ToDump(network1.pcap);
network2[2]
	-> ToDump(network2.pcap);
cloud[3]
	-> ToDump(cloud.pcap);


// Traffic generators
// QoS traffic
RatedSource(LENGTH 83, RATE 300)
	-> DynamicUDPIPEncap(host1_address:ip, 7, host2_address:ip, 2222)
	-> EtherEncap(0x0800, host1_address:eth, host1_address:eth)
	-> host1;

// Best Effort traffic
RatedSource(LENGTH 83, RATE 1000)
        -> DynamicUDPIPEncap(host1_address:ip, 7, host2_address:ip, 3333)
        -> EtherEncap(0x0800, host1_address:eth, host1_address:eth)
        -> host1;
