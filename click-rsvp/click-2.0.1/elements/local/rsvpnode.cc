#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpnode.hh"
#include <clicknet/ether.h>
#include <clicknet/ip.h>
#include <clicknet/udp.h>

CLICK_DECLS
RSVPNode::RSVPNode()
{}

RSVPNode::~RSVPNode()
{}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
    
	return 0;
}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
