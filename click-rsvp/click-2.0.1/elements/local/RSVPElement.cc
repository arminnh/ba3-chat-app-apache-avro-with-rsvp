#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "RSVPElement.hh"
#include <stdexcept>

CLICK_DECLS

size_t sizeofRSVPObject(uint16_t class_num, uint16_t c_type)
{
	size_t size;

	switch (class_num) {
	case 1:
		size = sizeof(RSVPSession);
		break;
	case 3:
		size = sizeof(RSVPHop);
		break;
	case 4:
		size = sizeof(RSVPIntegrity);
		break;
	case 5:
		size = sizeof(RSVPTimeValues);
		break;
	case 6:
		size = sizeof(RSVPErrorSpec);
		break;
	case 8:
		size = sizeof(RSVPStyle);
		break;
	case 9:
		size = sizeof(RSVPFlowspec);
		break;
	case 10:
		size = sizeof(RSVPFilterSpec);
		break;
	case 11:
		size = sizeof(RSVPSenderTemplate);
		break;
	default:
		throw std::runtime_error("sizeofRSVPClass: requesting size of undefined class num");
	}

	return size;
}

void initRSVPCommonHeader(RSVPCommonHeader* header, uint8_t msg_type, uint8_t send_TTL)
{
	header->vers = 1;
	header->flags = 0;
	header->msg_type = msg_type;
	header->send_TTL = send_TTL;
	header->RSVP_checksum = 0;
	header->reserved = 0;
	header->RSVP_length = 0;

	return;
}

void initRSVPObjectHeader(RSVPObjectHeader* header, uint8_t class_num, uint8_t c_type)
{
	if (class_num == RSVP_CLASS_SESSION) {
		click_chatter("session object size: %d", sizeofRSVPObject(class_num, c_type));
		click_chatter("header size: %d", sizeof(RSVPObjectHeader));
	}
	header->length = htons(sizeofRSVPObject(class_num, c_type));
	header->class_num = class_num;
	header->c_type = c_type;

	return;
}

void initRSVPSession(RSVPSession* session, in_addr destinationAddress, uint8_t protocol_id, bool police, uint16_t dst_port)
{
	initRSVPObjectHeader(&session->header, RSVP_CLASS_SESSION, 1);

	session->IPv4_dest_address = destinationAddress;
	session->protocol_id = protocol_id;
	session->flags = 0;
	if (police) {
		session->flags |= 0x01;
	}
	session->dst_port = dst_port;

	return;
}

void initRSVPHop(RSVPHop* hop, in_addr next_previous_hop_address, uint32_t logical_interface_handle)
{
	initRSVPObjectHeader(&hop->header, RSVP_CLASS_RSVP_HOP, 1);

	hop->IPv4_next_previous_hop_address = next_previous_hop_address;
	hop->logical_interface_handle = logical_interface_handle;

	return;
}

void initRSVPTimeValues(RSVPTimeValues* timeValues, uint32_t refresh_period_r)
{
	initRSVPObjectHeader(&timeValues->header, RSVP_CLASS_TIME_VALUES, 1);

	timeValues->refresh_period_r = refresh_period_r;

	return;
}

void initRSVPStyle(RSVPStyle* style)
{
	initRSVPObjectHeader(&style->header, RSVP_CLASS_STYLE, 1);

	style->flags = 0;
	style->option_vector = 0x10; // three rightmost bits: 010 for explicit sender selection, next two bits: 01 for distinct reservations

	return;
}

RSVPElement::RSVPElement() : _timer(this)
{}

RSVPElement::~ RSVPElement()
{}

int RSVPElement::configure(Vector<String> &conf, ErrorHandler *errh) {
	if (conf.size() > 0)
		return errh->error("Only empty configuraion string");
	
	return 0;
}

int RSVPElement::initialize(ErrorHandler* errh) {
	_timer.initialize(this);
	_timer.schedule_after_msec(1000);
}

void RSVPElement::run_timer(Timer *) {
	static bool resv = true;
	if (resv) {
		output(0).push(createResvMessage());
	}
	
	_timer.reschedule_after_msec(1000);
	
	return;
}

void RSVPElement::push(int, Packet *p){

}

Packet* RSVPElement::pull(int){

}

int RSVPElement::sendHandler(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, type, cpEnd) < 0) return -1;

	Packet* message;

	if (type == "resv") {
		message = me->createResvMessage();
	} else if (type == "path") {
		message = me->createPathMessage();
	}

	me->output(0).push(message);

	return 0;
}

String RSVPElement::handle2(Element *e, void * thunk) {
	RSVPElement *me = (RSVPElement *) e;
	return "123211232132131";
}

void RSVPElement::add_handlers() {
	add_write_handler("send", &sendHandler, (void *)0);
	add_read_handler("b", &handle2, (void *)0);
}

Packet* RSVPElement::createResvMessage() {
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPTimeValues) +
		sizeof(RSVPStyle);
	unsigned tailroom = 0;

	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createResvMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPTimeValues* timeValues     = (RSVPTimeValues *)   (hop          + 1);
	RSVPStyle* style               = (RSVPStyle *)        (timeValues   + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESV, 250);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, htons(1));
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	initRSVPTimeValues(timeValues, 5000);
	initRSVPStyle(style);
	
	commonHeader->RSVP_length = htons(packetSize);
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathMessage()
{}
	

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPElement)