#ifndef CLICK_RSVPNODE_HH
#define CLICK_RSVPNODE_HH
#include <click/element.hh>
CLICK_DECLS

class RSVPNode: public Element { 
	public:
		RSVPNode();
		~RSVPNode();
		
		const char *class_name() const	{ return "RSVPNode"; }
		const char *port_count() const	{ return "1/1"; }
		const char *processing() const	{ return PUSH; }
		
		int configure(Vector<String>&, ErrorHandler*);
	private:
};

CLICK_ENDDECLS
#endif // CLICK_RSVPNODE_HH
