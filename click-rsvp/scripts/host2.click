/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
/// !!!!!!! DO NOT CHANGE THIS FILE:  Any changes made will be removed prior to the project defense !!!!!!!!
/// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

// Define imports
require(library routers/host.click)

// Address configuration
AddressInfo(host2_address 192.168.11.1/24 tap0:eth)
AddressInfo(router2_lan_address 192.168.11.254/24);

host2 :: Host(host2_address, tap0, router2_lan_address);

// Connect the host to the network
FromDevice(eth0, PROMISC true)
	-> host2
	-> Queue
	-> ToDevice(eth0)
