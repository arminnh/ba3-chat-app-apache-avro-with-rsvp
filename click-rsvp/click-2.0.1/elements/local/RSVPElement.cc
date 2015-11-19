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
	case 15:
		size = sizeof(RSVPResvConf);
		break;
	default:
		printf("sizeofRSVPClass: requesting size of undefined class num %d", class_num);
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

void initRSVPErrorSpec(RSVPErrorSpec* errorSpec, in_addr error_node_address, bool inPlace, bool notGuilty, uint8_t errorCode, uint16_t errorValue) {
	initRSVPObjectHeader(&errorSpec->header, RSVP_CLASS_ERROR_SPEC, 1);
	
	errorSpec->IPv4_error_node_address = error_node_address;
	errorSpec->flags = 0;
	
	errorSpec->flags |= (inPlace ? 0x1 : 0x0) | (notGuilty ? 0x2 : 0x0);
	errorSpec->error_code = errorCode;
	errorSpec->error_value = htons(errorValue);
}

void initRSVPResvConf(RSVPResvConf* resvConf, in_addr receiverAddress) {
	initRSVPObjectHeader(&resvConf->header, RSVP_CLASS_RESV_CONF, 1);
	
	resvConf->receiver_address = receiverAddress;
	
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
	
	clean();
	
	_timer.schedule_after_msec(1000);
}

void RSVPElement::run_timer(Timer *) {
	clean();
	output(0).push(createResvMessage());
	output(0).push(createPathMessage());
	output(0).push(createPathErrMessage());
	output(0).push(createResvErrMessage());
	output(0).push(createPathTearMessage());
	output(0).push(createResvTearMessage());
	output(0).push(createResvConfMessage());
	
	//_timer.reschedule_after_msec(1000);
	
	return;
}

void RSVPElement::push(int, Packet *p){

}

Packet* RSVPElement::pull(int){

}

int RSVPElement::sessionHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if(cp_va_kparse(conf, me, errh, 
		"DEST", cpkM, cpIPAddress, &me->_session_destination_address, 
		"PROTOCOL", cpkM, cpUnsigned, &me->_session_protocol_ID,
		"POLICE", cpkM, cpBool, &me->_session_police,
		"PORT", cpkM, cpUnsigned, &me->_session_destination_port, 
		cpEnd) < 0) return -1;
		
	return 0;
}

int RSVPElement::hopHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if(cp_va_kparse(conf, me, errh, 
		"NEIGHBOR", cpkM, cpIPAddress, &me->_hop_neighbor_address, 
		"LIH", cpkM, cpUnsigned, &me->_hop_logical_interface_handle,  
		cpEnd) < 0) return -1;
	
	return 0;
}

int RSVPElement::errorSpecHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;
	
	if(cp_va_kparse(conf, me, errh, 
		"ERROR_NODE_ADDRESS", cpkM, cpIPAddress, &me->_errorspec_error_node_address,
		"INPLACE", cpkM, cpBool, &me->_errorspec_inPlace,
		"NOTGUILTY", cpkM, cpBool, &me->_errorspec_notGuilty,
		"ERROR_CODE", cpkM, cpUnsigned, &me->_errorspec_errorCode,
		"ERROR_VALUE", cpkM, cpUnsigned, &me->_errorspec_errorValue,
		cpEnd) < 0) return -1;

	if(cp_va_kparse(conf, me, errh, 
	cpEnd) < 0) return -1;
	
	return 0;
}

int RSVPElement::timeValuesHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if(cp_va_kparse(conf, me, errh, 
		"REFRESH", cpkM, cpUnsigned, &me->_timeValues_refresh_period_r, 
		cpEnd) < 0) return -1;
	
	return 0;
}

int RSVPElement::resvConfObjectHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;


	if(cp_va_kparse(conf, me, errh,
		"RECEIVER_ADDRESS", cpkM, cpIPAddress, &me->_resvConf_receiver_address,
		cpEnd) < 0) return -1;
	
}

int RSVPElement::pathHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if(cp_va_kparse(conf, me, errh, 
		"TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createPathMessage();
	me->output(0).push(message);
	
	me->clean();
	return 0;
}

int RSVPElement::resvHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_hop_neighbor_address = IPAddress("0.0.0.0").in_addr();
	me->_hop_logical_interface_handle = 0;
		
	me->_timeValues_refresh_period_r = 0;
	
	Packet* message = me->createResvMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::pathErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_errorspec_error_node_address = IPAddress("0.0.0.0").in_addr();
	me->_errorspec_inPlace = false;
	me->_errorspec_inPlace = 0;
	me->_errorspec_notGuilty = false;
	me->_errorspec_errorValue;
	
	Packet* message = me->createPathErrMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_errorspec_error_node_address = IPAddress("0.0.0.0").in_addr();
	me->_errorspec_inPlace = false;
	me->_errorspec_inPlace = 0;
	me->_errorspec_notGuilty = false;
	me->_errorspec_errorValue;
	
	me->_hop_neighbor_address = IPAddress("0.0.0.0").in_addr();
	me->_hop_logical_interface_handle = 0;
	
	Packet* message = me->createResvErrMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::pathTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_hop_neighbor_address = IPAddress("0.0.0.0").in_addr();
	me->_hop_logical_interface_handle = 0;
	
	Packet* message = me->createPathTearMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	
	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_hop_neighbor_address = IPAddress("0.0.0.0").in_addr();
	me->_hop_logical_interface_handle = 0;
	
	Packet* message = me->createResvTearMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvConfHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	String type;

	if(cp_va_kparse(conf, me, errh, "TYPE", cpkM + cpkP, cpString, &type, cpEnd) < 0) return -1;

	me->_TTL = 250;
	
	me->_session_destination_address = IPAddress("0.0.0.0").in_addr();
	me->_session_protocol_ID = 0;
	me->_session_police = false;
	me->_session_destination_port = 0;
	
	me->_errorspec_error_node_address = IPAddress("0.0.0.0").in_addr();
	me->_errorspec_inPlace = false;
	me->_errorspec_inPlace = 0;
	me->_errorspec_notGuilty = false;
	me->_errorspec_errorValue;
	
	me->_resvConf_receiver_address = IPAddress("0.0.0.0").in_addr();
	
	Packet* message = me->createResvConfMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

String RSVPElement::handle2(Element *e, void * thunk) {
	RSVPElement *me = (RSVPElement *) e;
	return "123211232132131";
}

void RSVPElement::add_handlers() {
	add_write_handler("sendpath", &pathHandle, (void *)0);
	add_write_handler("session", &sessionHandle, (void *)0);
	add_write_handler("hop", &hopHandle, (void *)0);
	add_write_handler("errorspec", &errorSpecHandle, (void *)0);
	add_write_handler("timevalues", &timeValuesHandle, (void *)0);
	add_write_handler("resvconfobj", &resvConfObjectHandle, (void *)0);
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATH, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPHop(hop, _hop_neighbor_address, _hop_logical_interface_handle);
	initRSVPTimeValues(timeValues, _timeValues_refresh_period_r);
	
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESV, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPHop(hop, _hop_neighbor_address, _hop_logical_interface_handle);
	initRSVPTimeValues(timeValues, _timeValues_refresh_period_r);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathErrMessage()
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHERR, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPErrorSpec(errorSpec, _errorspec_error_node_address, _errorspec_inPlace, _errorspec_notGuilty, _errorspec_errorCode, _errorspec_errorValue);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvErrMessage()
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVERR, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPHop(hop, _hop_neighbor_address, _hop_logical_interface_handle);
	initRSVPErrorSpec(errorSpec, _errorspec_error_node_address, _errorspec_inPlace, _errorspec_notGuilty, _errorspec_errorCode, _errorspec_errorValue);
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHTEAR, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPHop(hop, _hop_neighbor_address, _hop_logical_interface_handle);
	
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVTEAR, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPHop(hop, _hop_neighbor_address, _hop_logical_interface_handle);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvConfMessage()
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
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVCONF, _TTL, packetSize);
	initRSVPSession(session, _session_destination_address, _session_protocol_ID, _session_police, _session_destination_port);
	initRSVPErrorSpec(errorSpec, _errorspec_error_node_address, _errorspec_inPlace, _errorspec_notGuilty, _errorspec_errorCode, _errorspec_errorValue);
	initRSVPResvConf(resvConf, _resvConf_receiver_address);
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

void RSVPElement::clean() {
	_TTL = 250;
	
	_session_destination_address = IPAddress("0.0.0.0").in_addr();
	_session_protocol_ID = 0;
	_session_police = false;
	_session_destination_port = 0;
	
	_errorspec_error_node_address = IPAddress("0.0.0.0").in_addr();
	_errorspec_inPlace = false;
	_errorspec_notGuilty = false;
	_errorspec_errorCode = 0;
	_errorspec_errorValue = 0;
	
	_hop_neighbor_address = IPAddress("0.0.0.0").in_addr();
	_hop_logical_interface_handle = 0;
	
	_timeValues_refresh_period_r = 0;
	
	_flowspec = false;
	_filterspec = false;
	_senderTemplate = false;
	_senderTspec = false;
	_resvConf = false;
	_resvConf_receiver_address = IPAddress("0.0.0.0").in_addr();
}	

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPElement)
