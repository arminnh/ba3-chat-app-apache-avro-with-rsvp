#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpelement.hh"
#include <stdexcept>

CLICK_DECLS

const void* RSVPOjectOfType(Packet* packet, uint8_t wanted_class_num) {
	uint8_t class_num, c_type;
	uint8_t obj_type;
	
	const RSVPObjectHeader* p = (const RSVPObjectHeader*) packet->data();
	p = (const RSVPObjectHeader*) (((const RSVPCommonHeader*) p) + 1);
	
	while (p < (const RSVPObjectHeader*) packet->end_data()) {
		readRSVPObjectHeader(const_cast<RSVPObjectHeader*>(p), class_num, c_type);
		if (class_num == wanted_class_num) {
			return p;
		}
		
		p = nextRSVPObject(p);
	}
	
	return NULL;
}


void RSVPElement::push(int, Packet *packet) {
	/*click_chatter("Received RSVP packet at RSVPElement %s", _name.c_str());

	const void* p = packet->data();
	const void* end_data = packet->end_data();
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	p = readRSVPCommonHeader((RSVPCommonHeader*) p, msg_type, send_TTL, length);
	
	switch (msg_type) {
		case RSVP_MSG_PATH:
			clean();
			RSVPSession session = *const_cast<RSVPSession*>((const RSVPSession*) RSVPOjectOfType(packet, 1));
			RSVPNodeSession nodeSession(session);
			// reply with a resv message
			HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.find(nodeSession);
			if (it != _pathStates.end()) {
				initRSVPHop(&_hop, it->second.previous_hop_node, 0);
				_session = session;
				initRSVPTimeValues(&_timeValues, 10);
			}
			break;
		case RSVP_MSG_RESV:
			// update state table
			break;
		case RSVP_MSG_PATHERR:
			// read rfc
			break;
		case RSVP_MSG_RESVERR:
			// read rfc
			break;
		case RSVP_MSG_PATHTEAR:
			// remove session, send resv tear
			break;
		case RSVP_MSG_RESVTEAR:
			// remove session, send path tear
			break;
		case RSVP_MSG_RESVCONF:
			// notify application if session present
			break;
		default:
			click_chatter("RSVPElement %s: no message type %d", _name.c_str(), msg_type);
	}
	
	uint8_t class_num, c_type;
	
	uint8_t obj_type;
	while (p < end_data) {
		readRSVPObjectHeader((RSVPObjectHeader*) p, class_num, c_type);
		
		uint8_t a = 0; uint16_t b = 0; uint32_t e = 0; uint32_t ee = 0; bool c = true; bool cc = false; float f = 0; float ff = 0; float fff = 0;
		in_addr d = IPAddress("0.0.0.0").in_addr();
		Vector<in_addr> srcs;
		
		click_chatter("\n");
		switch(class_num) {
			case RSVP_CLASS_SESSION:
				click_chatter("class SESSION");
				p = readRSVPSession((RSVPSession*) p, d, a, c, b);
				break;
			case RSVP_CLASS_RSVP_HOP:
				click_chatter("class HOP");
				p = readRSVPHop((RSVPHop*) p, d, e);
				break;
			case RSVP_CLASS_TIME_VALUES:
				click_chatter("class VALUES");
				p = readRSVPTimeValues((RSVPTimeValues*) p, e);
				break;
			case RSVP_CLASS_ERROR_SPEC:
				click_chatter("class SPEC");
				p = readRSVPErrorSpec((RSVPErrorSpec*) p, d, c, cc, a, b);
				break;
			case RSVP_CLASS_STYLE:
				click_chatter("class STYLE");
				p = readRSVPStyle((RSVPStyle*) p);
				break;
			case RSVP_CLASS_SCOPE:
				click_chatter("class SCOPE");
				p = readRSVPScope((RSVPObjectHeader*) p, srcs);
				break;
			case RSVP_CLASS_FLOWSPEC:
				click_chatter("class FLOWSPEC");
				p = readRSVPFlowspec((RSVPFlowspec*) p,f,ff,fff, e, ee);
				break;
			case RSVP_CLASS_FILTER_SPEC:
				click_chatter("class SPEC");
				p = readRSVPFilterSpec((RSVPFilterSpec*) p, d, b);
				break;
			case RSVP_CLASS_SENDER_TEMPLATE:
				click_chatter("class TEMPLATE");
				p = readRSVPSenderTemplate((RSVPSenderTemplate*) p, d, b);
				break;
			case RSVP_CLASS_SENDER_TSPEC:
				click_chatter("class TSPEC");
				p =  readRSVPSenderTSpec((RSVPSenderTSpec*) p,f,ff,fff,e,ee);
				break;
			case RSVP_CLASS_RESV_CONF:
				click_chatter("class CONF");
				p = readRSVPResvConf((RSVPResvConf*) p, d);
				break;
			default:
				click_chatter("class_num %d not found", class_num);
		}
	}
	
	// updateState();

	output(0).push(packet);	*/
}

Packet* RSVPElement::pull(int){
	return NULL;
}

RSVPElement::RSVPElement() : _timer(this)
{}

RSVPElement::~ RSVPElement()
{}

int RSVPElement::configure(Vector<String> &conf, ErrorHandler *errh) {
	return 0;
}

int RSVPElement::initialize(ErrorHandler* errh) {
	// _timer.initialize(this);
	
	clean();
	
	// _timer.schedule_after_msec(1000);

	return 0;
}

void RSVPElement::run_timer(Timer *) {
	clean();
	/*output(0).push(createResvMessage());
	output(0).push(createPathMessage());
	output(0).push(createPathErrMessage());
	output(0).push(createResvErrMessage());
	output(0).push(createPathTearMessage());
	output(0).push(createResvTearMessage());
	output(0).push(createResvConfMessage());*/
	
	//_timer.reschedule_after_msec(1000);
	
	return;
}

int RSVPElement::sessionHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	uint8_t protocol_ID;
	bool police;
	uint16_t destination_port;
	
	in_addr* destination_address = new in_addr;
	
	if (cp_va_kparse(conf, me, errh, 
		"DEST", cpkM, cpIPAddress, destination_address, 
		"PROTOCOL", cpkM, cpUnsigned, &protocol_ID,
		"POLICE", cpkM, cpBool, &police,
		"PORT", cpkM, cpUnsigned, &destination_port, 
		cpEnd) < 0) return -1;
	
	//click_chatter("READING SESSION OBJECT; destination address: %s", IPAddress(*destination_address).s().c_str());
	
	initRSVPSession(&me->_session, *destination_address, protocol_ID, police, destination_port);

	delete destination_address;

	return 0;
}

int RSVPElement::hopHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	in_addr neighbor_address;
	uint32_t logical_interface_handle;

	if (cp_va_kparse(conf, me, errh, 
		"NEIGHBOR", cpkM, cpIPAddress, &neighbor_address, 
		"LIH", cpkM, cpUnsigned, &logical_interface_handle,  
		cpEnd) < 0) return -1;
	
	initRSVPHop(&me->_hop, neighbor_address, logical_interface_handle);

	return 0;
}

int RSVPElement::errorSpecHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;
	
	in_addr* error_node_address = new in_addr;
	bool inPlace;
	bool notGuilty;
	uint8_t errorCode;
	uint16_t errorValue;

	if (cp_va_kparse(conf, me, errh, 
		"ERROR_NODE_ADDRESS", cpkM, cpIPAddress, error_node_address,
		"INPLACE", cpkM, cpBool, &inPlace,
		"NOTGUILTY", cpkM, cpBool, &notGuilty,
		"ERROR_CODE", cpkM, cpUnsigned, &errorCode,
		"ERROR_VALUE", cpkM, cpUnsigned, &errorValue,
		cpEnd) < 0) return -1;
	
	initRSVPErrorSpec(&me->_errorSpec, *error_node_address, inPlace, notGuilty, errorCode, errorValue);

	delete error_node_address;

	return 0;
}

int RSVPElement::timeValuesHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	uint32_t refresh_period_r;

	if (cp_va_kparse(conf, me, errh, 
		"REFRESH", cpkM, cpUnsigned, &refresh_period_r, 
		cpEnd) < 0) return -1;
	
	initRSVPTimeValues(&me->_timeValues, refresh_period_r);

	return 0;
}

int RSVPElement::resvConfObjectHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	in_addr receiver_address;

	if (cp_va_kparse(conf, me, errh,
		"RECEIVER_ADDRESS", cpkM, cpIPAddress, &receiver_address,
		cpEnd) < 0) return -1;

	initRSVPResvConf(&me->_resvConf, receiver_address);

	me->_resvConf_given = true;

	return 0;
}

int RSVPElement::pathHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createPathMessage();
	me->output(0).push(message);
	
	me->clean();
	return 0;
}

int RSVPElement::resvHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createResvMessage();
	me->output(0).push(message);
	
	me->clean();
	return 0;
}

int RSVPElement::pathErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createPathErrMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createResvErrMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::pathTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createPathTearMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;

	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createResvTearMessage();
	me->output(0).push(message);
	
	me->clean();

	return 0;
}

int RSVPElement::resvConfHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh) {
	RSVPElement * me = (RSVPElement *) e;
	if (cp_va_kparse(conf, me, errh, "TTL", cpkM, cpInteger, &me->_TTL, cpEnd) < 0) return -1;
	
	Packet* message = me->createResvConfMessage();
	me->output(0).push(message);
	
	me->clean();
	return 0;
}

int RSVPElement::scopeHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh)
{
	RSVPElement *me = (RSVPElement *) e;

	in_addr src_address;

	if (cp_va_kparse(conf, me, errh, "SRC_ADDRESS", cpkM, cpIPAddress, &src_address, cpEnd) < 0) return -1;

	me->_scope_src_addresses.push_back(src_address);

	return 0;
}

int RSVPElement::senderDescriptorHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh)
{
	RSVPElement *me = (RSVPElement *) e;

	double tbr, tbs, pdr;

	in_addr* senderTemplate_src_address = new in_addr;
	uint16_t senderTemplate_src_port;

	float senderTSpec_token_bucket_rate;
	float senderTSpec_token_bucket_size;
	float senderTSpec_peak_data_rate;
	uint32_t senderTSpec_minimum_policed_unit;
	uint32_t senderTSpec_maximum_packet_size;

	if (!cp_va_kparse(conf, me, errh,
		// sender template
		"SRC_ADDRESS", cpkM, cpIPAddress, senderTemplate_src_address,
		"SRC_PORT", cpkM, cpUnsigned, &senderTemplate_src_port,
		// sender tspec
		"TOKEN_BUCKET_RATE", cpkM, cpDouble, &tbr,
		"TOKEN_BUCKET_SIZE", cpkM, cpDouble, &tbs,
		"PEAK_DATA_RATE", cpkM, cpDouble, &pdr,
		"MINIMUM_POLICED_UNIT", cpkM, cpInteger, &senderTSpec_minimum_policed_unit,
		"MAXIMUM_PACKET_SIZE", cpkM, cpInteger, &senderTSpec_maximum_packet_size,
		cpEnd)) return -1;

	senderTSpec_token_bucket_rate = tbr;
	senderTSpec_token_bucket_size = tbs;
	senderTSpec_peak_data_rate = pdr;

	initRSVPSenderTemplate(&me->_senderTemplate, *senderTemplate_src_address, senderTemplate_src_port);
	initRSVPSenderTSpec(&me->_senderTSpec, senderTSpec_token_bucket_rate,
		senderTSpec_token_bucket_size, senderTSpec_peak_data_rate,
		senderTSpec_minimum_policed_unit, senderTSpec_maximum_packet_size);

	delete senderTemplate_src_address;

	me->_senderDescriptor = true;

	return 0;
}

int RSVPElement::nameHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh) {
	RSVPElement* me = (RSVPElement*) e;
	if (cp_va_kparse(conf, me, errh, "NAME", cpkP + cpkM, cpString, &me->_name, cpEnd) < 0)
		return -1;
	
	click_chatter("Set host RSVP element name: %s", me->_name.c_str());
	
	return 0;
}

String RSVPElement::getTTLHandle(Element *e, void * thunk) {
	RSVPElement *me = (RSVPElement *) e;
	return String((int) me->_TTL);
}

void RSVPElement::add_handlers() {
	// types of messages
	add_write_handler("path", &pathHandle, (void *) 0);
	add_write_handler("resv", &resvHandle, (void *) 0);
	add_write_handler("patherr", &pathErrHandle, (void *) 0);
	add_write_handler("resverr", &resvErrHandle, (void *) 0);
	add_write_handler("pathtear", &pathTearHandle, (void *) 0);
	add_write_handler("resvtear", &resvTearHandle, (void *) 0);
	add_write_handler("resvconf", &resvConfHandle, (void *) 0);
	add_write_handler("name", &nameHandle, (void *) 0);
	
	// types of objects
	add_write_handler("session", &sessionHandle, (void *) 0);
	add_write_handler("hop", &hopHandle, (void *)0);
	add_write_handler("errorspec", &errorSpecHandle, (void *) 0);
	add_write_handler("timevalues", &timeValuesHandle, (void *) 0);
	add_write_handler("resvconfobj", &resvConfObjectHandle, (void *) 0);
	add_write_handler("scope", &scopeHandle, (void *) 0);
	add_write_handler("senderdescriptor", &senderDescriptorHandle, (void *) 0);

	// random read handler
	add_read_handler("TTL", &getTTLHandle, (void *) 0);
}

WritablePacket* RSVPElement::createPacket(uint16_t packetSize) const
{
	unsigned headroom = sizeof(click_ip) + sizeof(click_ether);
	unsigned tailroom = 0;

	WritablePacket* message = Packet::make(headroom, 0, packetSize, tailroom);

	if (!message) click_chatter("RSVPElement::createPathMessage: cannot make element!");

	memset(message->data(), 0, message->length());

	return message;
}

Packet* RSVPElement::createPathMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPTimeValues) +
		(_senderDescriptor ?
			sizeof(RSVPSenderTemplate) +
			sizeof(RSVPSenderTSpec)
			: 0);
	
	WritablePacket* message = createPacket(packetSize);
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPTimeValues* timeValues     = (RSVPTimeValues *)   (hop          + 1);
	RSVPSenderTemplate* senderTemplate = (RSVPSenderTemplate *) (timeValues + 1);
	RSVPSenderTSpec* senderTSpec   = (RSVPSenderTSpec *)  (senderTemplate + 1);

	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATH, _TTL, packetSize);
	*session = _session;
	*hop = _hop;
	*timeValues = _timeValues;
	if (_senderDescriptor) {
		*senderTemplate = _senderTemplate;
		*senderTSpec = _senderTSpec;
	}
	
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvMessage() const {
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPTimeValues) +
		(_resvConf_given ? sizeof(RSVPResvConf) : 0) +
		sizeofRSVPScopeObject(_scope_src_addresses.size()) +
		sizeof(RSVPStyle) +
		sizeof(RSVPFlowspec);

	WritablePacket* message = createPacket(packetSize);
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPTimeValues* timeValues     = (RSVPTimeValues *)   (hop          + 1);
	RSVPResvConf* resvConf         = (RSVPResvConf *)     (timeValues   + 1);
	RSVPStyle* style               = (RSVPStyle *)        initRSVPScope((RSVPObjectHeader *) (resvConf + (_resvConf_given ? 1 : 0)), _scope_src_addresses);
	RSVPFlowspec* flowspec         = (RSVPFlowspec *)     (style        + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESV, _TTL, packetSize);
	*session = _session;
	*hop = _hop;
	*timeValues = _timeValues;
	if (_resvConf_given) {
		*resvConf = _resvConf;
	}
	initRSVPStyle(style);
	initRSVPFlowspec(flowspec, 30.5, 0.4e38f, -5.0, 50, 100);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathErrMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPErrorSpec);
	unsigned tailroom = 0;
	
	WritablePacket* message = createPacket(packetSize);
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (session      + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHERR, _TTL, packetSize);
	*session = _session;
	*errorSpec = _errorSpec;
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvErrMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeof(RSVPErrorSpec) +
		sizeofRSVPScopeObject(_scope_src_addresses.size()) +
		sizeof(RSVPStyle);
	
	WritablePacket* message = createPacket(packetSize);
	
	memset(message->data(), 0, message->length());
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (hop          + 1);
	RSVPStyle* style               = (RSVPStyle *)        initRSVPScope((RSVPObjectHeader *) (errorSpec + 1), _scope_src_addresses);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVERR, _TTL, packetSize);
	*session = _session;
	*hop = _hop;
	*errorSpec = _errorSpec;
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createPathTearMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop);
	
	WritablePacket* message = createPacket(packetSize);
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_PATHTEAR, _TTL, packetSize);
	*session = _session;
	*hop = _hop;
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvTearMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPHop) +
		sizeofRSVPScopeObject(_scope_src_addresses.size()) +
		sizeof(RSVPStyle);
	
	WritablePacket* message = createPacket(packetSize);
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPHop* hop                   = (RSVPHop *)          (session      + 1);
	RSVPStyle* style               = (RSVPStyle *)        initRSVPScope((RSVPObjectHeader *) (hop + 1), _scope_src_addresses);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVTEAR, _TTL, packetSize);
	*session = _session;
	*hop = _hop;
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

Packet* RSVPElement::createResvConfMessage() const
{
	uint16_t packetSize =
		sizeof(RSVPCommonHeader) +
		sizeof(RSVPSession) +
		sizeof(RSVPErrorSpec) +
		sizeof(RSVPResvConf) +
		sizeof(RSVPStyle);
	
	WritablePacket* message = createPacket(packetSize);
	
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader *) (message->data());
	RSVPSession* session           = (RSVPSession *)      (commonHeader + 1);
	RSVPErrorSpec* errorSpec       = (RSVPErrorSpec *)    (session      + 1);
	RSVPResvConf* resvConf         = (RSVPResvConf *)     (errorSpec    + 1);
	RSVPStyle* style               = (RSVPStyle *)        (resvConf     + 1);
	
	initRSVPCommonHeader(commonHeader, RSVP_MSG_RESVCONF, _TTL, packetSize);
	*session = _session;
	*errorSpec = _errorSpec;
	*resvConf = _resvConf;
	initRSVPStyle(style);
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) commonHeader, packetSize);
	
	return message;
}

void RSVPElement::clean() {
	_TTL = 250;
	
	memset(&_session, 0, sizeof(RSVPSession));
	
	memset(&_errorSpec, 0, sizeof(RSVPErrorSpec));
	
	memset(&_hop, 0, sizeof(RSVPHop));
	
	memset(&_timeValues, 0, sizeof(RSVPTimeValues));
	
	_flowspec = false;
	_filterspec = false;
	_senderDescriptor = false;

	memset(&_senderTemplate, 0, sizeof(RSVPSenderTemplate));

	memset(&_senderTSpec, 0, sizeof(RSVPSenderTSpec));

	_resvConf_given = false;
	memset(&_resvConf, 0, sizeof(RSVPResvConf));

	_scope_src_addresses.clear();
}	

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPElement)
