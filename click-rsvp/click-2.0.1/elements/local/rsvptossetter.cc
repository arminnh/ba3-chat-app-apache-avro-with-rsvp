#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvptossetter.hh"
#include <clicknet/ip.h>
#include <clicknet/udp.h>
#include <clicknet/tcp.h>
CLICK_DECLS
RSVPToSSetter::RSVPToSSetter()
{}

RSVPToSSetter::~ RSVPToSSetter()
{}

int RSVPToSSetter::configure(Vector<String> &conf, ErrorHandler *errh) {
	String rsvpElementName;

	if (cp_va_kparse(conf, this, errh,
		"RSVPELEMENTNAME", cpkP + cpkM, cpString, &rsvpElementName, cpEnd) < 0) return -1;

	_rsvp = (RSVPNode *) router()->find(rsvpElementName, this, errh);

	if (!_rsvp) {
		click_chatter("RSVPToSSetter: didn't find RSVPNode element.");
	}

	return 0;
}

void RSVPToSSetter::push(int, Packet *p)
{
	WritablePacket* wp = p->uniqueify();
	click_ip* ip = (click_ip*) wp->data();

	in_addr src_addr = ip->ip_src;
	in_addr dst_addr = ip->ip_dst;

	uint16_t src_port, dst_port;
	uint8_t protocol_id = ip->ip_p;

	if (protocol_id == 6) { // TCP
		const click_tcp* tcp = (const click_tcp *) (ip + 1);
		src_port = htons(tcp->th_sport);
		dst_port = htons(tcp->th_dport);
	} else if (protocol_id == 17) { // UDP
		const click_udp* udp = (const click_udp *) (ip + 1);
		src_port = htons(udp->uh_sport);
		dst_port = htons(udp->uh_dport);
	}
// click_chatter("src_addr: %s, dst_addr: %s, src_port: %d, dst_port: %d", IPAddress(src_addr).unparse().c_str(), IPAddress(dst_addr).unparse().c_str(), src_port, dst_port);
	RSVPNodeSession session(dst_addr, protocol_id, dst_port);
// click_chatter("read from session object: %d", dst_port);
	RSVPSender sender(src_addr, src_port);

	if (_rsvp->hasReservation(session, sender)) {
		click_chatter("setting tos to 1");
		ip->ip_tos = 1;
	} else {
		ip->ip_tos = 0;
	}

	ip->ip_sum = 0;
	ip->ip_sum = click_in_cksum((const unsigned char*) ip, sizeof(click_ip));

	output(0).push(wp);

}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPToSSetter)
