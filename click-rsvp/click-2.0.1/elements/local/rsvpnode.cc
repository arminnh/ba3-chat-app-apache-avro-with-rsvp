#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpnode.hh"
#include <clicknet/ether.h>
#include <clicknet/ip.h>
#include <clicknet/udp.h>

CLICK_DECLS

RSVPNodeSession::RSVPNodeSession(in_addr dst_addr, uint8_t protocol_id, uint8_t dst_port) : _dst_ip_address(dst_addr), _protocol_id(protocol_id), _dst_port(dst_port) {
	// key = IPAddress(_dst_ip_address).unparse() + String(_protocol_id) + String(_dst_port);
}

RSVPNodeSession::key_const_reference RSVPNodeSession::hashcode() const {
	return *reinterpret_cast<const long unsigned int*>(&_dst_ip_address);
}

RSVPNode::RSVPNode()
{}

RSVPNode::~RSVPNode()
{}


void RSVPNode::push(int port, Packet* packet) {
	click_chatter("RSVPNode: Got a packet of size %d", packet->length());
	// packet analyzation
	
	const void* p = packet->data();
	const void* end_data = packet->end_data();
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	
	p = readRSVPCommonHeader((RSVPCommonHeader*) p, msg_type, send_TTL, length);
	if (msg_type == RSVP_MSG_PATH) {
		updatePathState(packet);
	} else if (msg_type == RSVP_MSG_RESV) {
		
	}
	output(0).push(packet);
}

void RSVPNode::updatePathState(Packet* p) {
	
}

void RSVPNode::run_timer(Timer* timer) {
	
}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
    
	return 0;
}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
