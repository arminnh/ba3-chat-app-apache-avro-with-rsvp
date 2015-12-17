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

bool RSVPNodeSession::operator==(const RSVPNodeSession& other) const {
	return _dst_ip_address == other._dst_ip_address
		&& _protocol_id == other._protocol_id
		&& _dst_port == other._dst_port;
}

RSVPNode::RSVPNode()
{}

RSVPNode::~RSVPNode()
{}


void RSVPNode::push(int port, Packet* packet) {
	click_chatter("RSVPNode: Got a packet of size %d", packet->length());
	// packet analyzation
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	
	readRSVPCommonHeader((RSVPCommonHeader*) packet->data(), msg_type, send_TTL, length);
	if (msg_type == RSVP_MSG_PATH) {
		updatePathState(packet);
	} else if (msg_type == RSVP_MSG_RESV) {
		
	}
	output(0).push(packet);
}

void RSVPNode::updatePathState(Packet* packet) {
	const void* p = packet->data();
	p = (void*) ((RSVPCommonHeader*) p + 1);
	RSVPObjectHeader* header;
	uint8_t class_num;
	
	RSVPSenderTemplate* senderTemplate = NULL;
	RSVPSenderTSpec* senderTSpec = NULL;
	RSVPHop* hop = NULL;
	RSVPTimeValues* timeValues = NULL;
	RSVPSession* session = NULL;
	
	while (p < packet->end_data()) {
		header = (RSVPObjectHeader*) p;
		class_num = header->class_num;
		switch (class_num) {
			case RSVP_CLASS_SENDER_TEMPLATE:
				senderTemplate = (RSVPSenderTemplate*) p;
			case RSVP_CLASS_SENDER_TSPEC:
				senderTSpec = (RSVPSenderTSpec*) p;
			break;
			case RSVP_CLASS_RSVP_HOP:
				hop = (RSVPHop*) p;
			break;
			case RSVP_CLASS_TIME_VALUES:
				timeValues = (RSVPTimeValues*) p;
			break;
			case RSVP_CLASS_SESSION:
				session = (RSVPSession*) p;
				
		}
		p = (const void*) nextRSVPObject((RSVPObjectHeader*) p);
	}
	
	RSVPNodeSession nodeSession(session->IPv4_dest_address, session->protocol_id, session->dst_port);
	HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.find(nodeSession);
	if (it != _pathStates.end()) {
		it->second.timer->unschedule();
		delete it->second.timer;
	}
	// make new
	RSVPPathState pathState;
	pathState.previous_hop_node = hop->IPv4_next_previous_hop_address;
	pathState.senderTemplate = *senderTemplate;
	pathState.senderTSpec = *senderTSpec;
	pathState.timer = new Timer(this);
	_pathStates.set(nodeSession, pathState); 
	
}

void RSVPNode::run_timer(Timer* timer) {
	
}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
    
	return 0;
}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
