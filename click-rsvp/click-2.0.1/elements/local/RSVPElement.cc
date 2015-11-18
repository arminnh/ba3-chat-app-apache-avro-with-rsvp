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

void initRSVPCommonHeader(RSVPCommonHeader* header, uint8_t msg_type, uint8_t send_TTL, uint16_t length)
{
	header->vers = 1;
	header->flags = 0;
	header->msg_type = msg_type;
	header->send_TTL = send_TTL;
	header->RSVP_checksum = 0;
	header->reserved = 0;
	header->RSVP_length = htons(length);

	return;
}

void initRSVPObjectHeader(RSVPObjectHeader* header, uint8_t class_num, uint8_t c_type)
{
	
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
	session->dst_port = htons(dst_port);

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

	timeValues->refresh_period_r = htonl(refresh_period_r);

	return;
}

void initRSVPStyle(RSVPStyle* style)
{
	initRSVPObjectHeader(&style->header, RSVP_CLASS_STYLE, 1);

	style->flags = 0;
	style->option_vector = htons(10) << 8; // three rightmost bits: 010 for explicit sender selection, next two bits: 01 for distinct reservations

	return;
}

RSVPElement::RSVPElement() : _timer(this)
{}

RSVPElement::~ RSVPElement()
{}

int RSVPElement::configure(Vector<String> &conf, ErrorHandler *errh) {
	return 0;
}

int RSVPElement::initialize(ErrorHandler* errh) {
	_timer.initialize(this);
	_timer.schedule_after_msec(1000);
}

void RSVPElement::run_timer(Timer *) {
	output(0).push(createResvMessage());
	output(0).push(createPathMessage());
	output(0).push(createPathErrMessage(true, true, 5, 6));
	output(0).push(createResvErrMessage(true, true, 7, 8));
	output(0).push(createPathTearMessage());
	output(0).push(createResvTearMessage());
	output(0).push(createResvConfMessage(true, true, 9, 10));
	
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

Packet* RSVPElement::createPathMessage()
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPTimeValues);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createPathMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPTimeValues* timeValues     = (RSVPTimeValues *)   (hop          + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATH, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	initRSVPTimeValues(timeValues, UINT32_MAX);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESV, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	initRSVPTimeValues(timeValues, UINT32_MAX);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathErrMessage(bool inPlace, bool notGuilty, uint8_t errorCode, uint16_t errorValue)
{

	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPErrorSpec);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createPathErrMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (session      + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHERR, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPErrorSpec(errorSpec, IPAddress("1.3.2.4").in_addr(), inPlace, notGuilty, errorCode, errorValue);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvErrMessage(bool inPlace, bool notGuilty, uint8_t errorCode, uint16_t errorValue)
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPErrorSpec) +
		sizeof(RSVPStyle);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createResvErrMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (hop          + 1);
	RSVPStyle* style               = (RSVPStyle *)        (errorSpec    + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVERR, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	initRSVPErrorSpec(errorSpec, IPAddress("1.3.2.4").in_addr(), inPlace, notGuilty, errorCode, errorValue);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathTearMessage()
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createPathTearMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHTEAR, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvTearMessage()
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPStyle);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createResvTearMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPStyle* style               = (RSVPStyle *)        (hop          + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVTEAR, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPHop(hop, IPAddress("2.3.4.254").in_addr(), 0);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvConfMessage(bool inPlace, bool notGuilty, uint8_t errorCode, uint16_t errorValue)
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPErrorSpec) +
		sizeof(RSVPResvConf) +
		sizeof(RSVPStyle);
	unsigned tailroom = 0;
	
	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);
	
	if (!message) click_chatter("RSVPElement::createResvConfMessage: cannot make element!");
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (session      + 1);
	RSVPResvConf* resvConf         = (RSVPResvConf *)     (errorSpec    + 1);
	RSVPStyle* style               = (RSVPStyle *)        (resvConf     + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVCONF, 250, packetSize);
	initRSVPSession(session, IPAddress("1.2.3.4").in_addr(), 1, false, 1);
	initRSVPErrorSpec(errorSpec, IPAddress("1.3.2.4").in_addr(), inPlace, notGuilty, errorCode, errorValue);
	initRSVPResvConf(resvConf, IPAddress("1.3.2.4").in_addr());
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}
	

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPElement)
