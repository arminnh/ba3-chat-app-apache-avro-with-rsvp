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
		click_chatter("RSVPNODE msg_type == PATH");
		updatePathState(packet);
	} else if (msg_type == RSVP_MSG_RESV) {
		click_chatter("RSVPNODE msg_type == RESV");
		
	}
	click_chatter("RSVPNODE push pushing out my baby");
	output(0).push(packet);
	click_chatter("RSVPNODE push pushed it out");
}

void RSVPNode::updatePathState(Packet* packet) {
	click_chatter("updatePathState: start");
	WritablePacket* wp = packet->uniqueify();
	packet = wp;
	const void* p = packet->data();
	p = (void*) ((RSVPCommonHeader*) p + 1);
	RSVPObjectHeader* header;
	uint8_t class_num;
	
	RSVPSenderTemplate* senderTemplate = NULL;
	RSVPSenderTSpec* senderTSpec = NULL;
	RSVPHop* hop = NULL;
	RSVPTimeValues* timeValues = NULL;
	RSVPSession* session = NULL;
	
	click_chatter("updatePathState: enter while loop");
	while (p < packet->end_data()) {
		header = (RSVPObjectHeader*) p;
		class_num = header->class_num;
		switch (class_num) {
			case RSVP_CLASS_SENDER_TEMPLATE:
				click_chatter("updatePathState switch: TEMPLATE");
				senderTemplate = (RSVPSenderTemplate*) p;
			case RSVP_CLASS_SENDER_TSPEC:
				click_chatter("updatePathState switch: TSPEC");
				senderTSpec = (RSVPSenderTSpec*) p;
			break;
			case RSVP_CLASS_RSVP_HOP:
				click_chatter("updatePathState switch: HOP");
				hop = (RSVPHop*) p;
			break;
			case RSVP_CLASS_TIME_VALUES:
				click_chatter("updatePathState switch: VALUES");
				timeValues = (RSVPTimeValues*) p;
			break;
			case RSVP_CLASS_SESSION:
				click_chatter("updatePathState switch: SESSION");
				session = (RSVPSession*) p;
			default:
				click_chatter("updatePathState switch: class num noope");
				
		}
		p = (const void*) nextRSVPObject((RSVPObjectHeader*) p);
		click_chatter("updatePathState: p = next rsvp object");
	}
	click_chatter("updatePathState: left while loop");
	
	RSVPNodeSession nodeSession(session->IPv4_dest_address, session->protocol_id, session->dst_port);
	HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.find(nodeSession);
	if (it != _pathStates.end()) {
		click_chatter("updatePathState: found table entry");
		it->second.timer->unschedule();
		delete it->second.timer;
	}
	click_chatter("updatePathState: setting table entry");
	// make new
	RSVPPathState pathState;
	pathState.previous_hop_node = hop->IPv4_next_previous_hop_address;
	if (senderTemplate && senderTSpec) {
		pathState.senderTemplate = *senderTemplate;
		pathState.senderTSpec = *senderTSpec;
	}
	pathState.timer = new Timer(this);
	pathState.timer->initialize(this);
	click_chatter("updatePathState: set table entry stuff");
	uint32_t refresh_period_r;
	readRSVPTimeValues(timeValues, refresh_period_r);
	click_chatter("updatePathState: read refresh period");
	pathState.timer->schedule_after_sec(refresh_period_r); // TODO: change !!!11
	_pathStates.set(nodeSession, pathState);
	click_chatter("updatePathState: set new refresh period");
	click_chatter("Address: %s", IPAddress(hop->IPv4_next_previous_hop_address).unparse().c_str());
	hop->IPv4_next_previous_hop_address = _myIP;
	click_chatter("Address: %s", IPAddress(hop->IPv4_next_previous_hop_address).unparse().c_str());
	click_chatter("updatePathState: end");
}

void RSVPNode::run_timer(Timer* timer) {
	click_chatter("timer ran out");
}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
	if (cp_va_kparse(conf, this, errh, 
		"IP", cpkM + cpkP, cpIPAddress, &_myIP,
		cpEnd) < 0) return -1;
	return 0;
}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
