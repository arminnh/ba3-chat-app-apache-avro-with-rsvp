#ifndef CLICK_RSVPTOSSETTER_HH
#define CLICK_RSVPTOSSETTER_HH
#include <click/element.hh>
#include "rsvpelement.hh"
CLICK_DECLS

class RSVPToSSetter: public Element { 
	public:
		RSVPToSSetter();
		~RSVPToSSetter();
		
		const char *class_name() const	{ return "RSVPToSSetter"; }
		const char *port_count() const	{ return "1/1"; }
		const char *processing() const	{ return PUSH; }
		
		int configure(Vector<String>&, ErrorHandler*);
		
		void push(int, Packet *);
	
	private:
		RSVPNode* _rsvp;
};

CLICK_ENDDECLS
#endif // CLICK_RSVPTOSSETTER_HH