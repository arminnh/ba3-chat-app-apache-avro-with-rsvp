#ifndef CLICK_RSVPNODE_HH
#define CLICK_RSVPNODE_HH
#include <click/element.hh>
#include <click/hashtable.hh>
#include "rsvpelement.hh"
CLICK_DECLS


struct RSVPNodeSession {
	RSVPNodeSession(in_addr, uint8_t protocol_id, uint8_t dst_port);
	typedef long unsigned int key_type;
	typedef const key_type& key_const_reference;
	const key_type& hashcode() const;
	key_type key;
	bool operator==(const RSVPNodeSession& other) const;
public:
	in_addr _dst_ip_address;
	uint8_t _protocol_id;
	uint8_t _dst_port;
};

struct RSVPPathState {
	in_addr previous_hop_node;
	RSVPSenderTSpec senderTSpec;
	RSVPSenderTemplate senderTemplate;
	Timer* timer;
};

struct RSVPResvState {
	RSVPFlowspec flowspec;
	
};

class RSVPNode: public Element { 
public:
	RSVPNode();
	~RSVPNode();
	
	void push(int port, Packet* packet);
	void updatePathState(Packet*);
	
	void run_timer(Timer*);
	
	const char *class_name() const	{ return "RSVPNode"; }
	const char *port_count() const	{ return "1/1"; }
	const char *processing() const	{ return PUSH; }
	
	int configure(Vector<String>&, ErrorHandler*);
private:
	in_addr _myIP;
	HashTable<RSVPNodeSession, RSVPPathState> _pathStates;
	HashTable<RSVPNodeSession, RSVPResvState> _resvStates;
};

CLICK_ENDDECLS
#endif // CLICK_RSVPNODE_HH
