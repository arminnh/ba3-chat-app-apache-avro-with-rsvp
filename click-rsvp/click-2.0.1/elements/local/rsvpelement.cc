#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpelement.hh"
#include <stdexcept>

CLICK_DECLS

uint16_t sizeofRSVPObject(uint8_t class_num, uint8_t c_type)
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
	case 12:
		size = sizeof(RSVPSenderTSpec);
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

uint16_t sizeofRSVPScopeObject(size_t num_addresses) {
	return num_addresses ? (sizeof(RSVPObjectHeader) + num_addresses * sizeof(in_addr)) : 0;
}

const RSVPObjectHeader* nextRSVPObject(const RSVPObjectHeader* header) {
	if (header->class_num == RSVP_CLASS_SCOPE) {
		throw std::runtime_error("Calling nextRSVPObject on a scope object");
	}
	
	switch(header->class_num) {
		case RSVP_CLASS_SESSION:
			return (const RSVPObjectHeader*) (((const RSVPSession*) header) + 1);
		case RSVP_CLASS_RSVP_HOP:
			return (const RSVPObjectHeader*) (((const RSVPHop*) header) + 1);
		case RSVP_CLASS_TIME_VALUES:
			return (const RSVPObjectHeader*) (((const RSVPTimeValues*) header) + 1);
		case RSVP_CLASS_ERROR_SPEC:
			return (const RSVPObjectHeader*) (((const RSVPErrorSpec*) header) + 1);
		case RSVP_CLASS_STYLE:
			return (const RSVPObjectHeader*) (((const RSVPStyle*) header) + 1);
		case RSVP_CLASS_FLOWSPEC:
			return (const RSVPObjectHeader*) (((const RSVPFlowspec*) header) + 1);
		case RSVP_CLASS_FILTER_SPEC:
			return (const RSVPObjectHeader*) (((const RSVPFilterSpec*) header) + 1);
		case RSVP_CLASS_SENDER_TEMPLATE:
			return (const RSVPObjectHeader*) (((const RSVPSenderTemplate*) header) + 1);
		case RSVP_CLASS_SENDER_TSPEC:
			return (const RSVPObjectHeader*) (((const RSVPSenderTSpec*) header) + 1);
		case RSVP_CLASS_RESV_CONF:
			return (const RSVPObjectHeader*) (((const RSVPResvConf*) header) + 1);
		default:
			click_chatter("nextRSVPObject: class num %d not found", header->class_num);
			throw std::runtime_error("");
	}
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
	hop->logical_interface_handle = htonl(logical_interface_handle);

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

	return;
}

void initRSVPResvConf(RSVPResvConf* resvConf, in_addr receiverAddress) {
	initRSVPObjectHeader(&resvConf->header, RSVP_CLASS_RESV_CONF, 1);
	
	resvConf->receiver_address = receiverAddress;
	
	return;
}

// returns pointer to the position just after the scope object
void* initRSVPScope(RSVPObjectHeader* header, const Vector<in_addr>& src_addresses)
{
	if (!src_addresses.size()) {
		return (void *) header;
	}

	header->length = htons(sizeofRSVPScopeObject(src_addresses.size()));
	header->class_num = RSVP_CLASS_SCOPE;
	header->c_type = 1;

	in_addr* address = (in_addr *) (header + 1);

	for (int i = 0; i < src_addresses.size(); ++i) {
		*address = src_addresses.at(i);
		address += 1;
	}

	return (void *) address;
}

void initRSVPFlowspec(RSVPFlowspec* flowspec,
	float token_bucket_rate,
	float token_bucket_size,
	float peak_data_rate,
	uint32_t minimum_policed_unit,
	uint32_t maximum_packet_size)
{
	initRSVPObjectHeader(&flowspec->header, RSVP_CLASS_FLOWSPEC, 2);

	flowspec->nothing_1 = 0;
	flowspec->overall_length = htons(7);
	flowspec->service_header = 1;
	flowspec->nothing_2 = 0;
	flowspec->controlled_load_data_length = htons(6);
	flowspec->parameter_id = 127;
	flowspec->flags = 0;
	flowspec->parameter_127_length = 5;
	flowspec->token_bucket_rate_float = htonl(* (uint32_t *) (&token_bucket_rate));
	flowspec->token_bucket_size_float = htonl(* (uint32_t *) (&token_bucket_size));
	flowspec->peak_data_rate_float = htonl(* (uint32_t *) (&peak_data_rate));
	flowspec->minimum_policed_unit = htonl(minimum_policed_unit);
	flowspec->maximum_packet_size = htonl(maximum_packet_size);
}

void initRSVPFilterSpec(RSVPFilterSpec* filterSpec, in_addr src_address, uint16_t src_port)
{
	initRSVPObjectHeader(&filterSpec->header, RSVP_CLASS_FILTER_SPEC, 1);

	filterSpec->src_address = src_address;
	filterSpec->nothing = 0;
	filterSpec->src_port = htons(src_port);

	return;
}

void initRSVPSenderTemplate(RSVPSenderTemplate* senderTemplate, in_addr src_address, uint16_t src_port)
{
	initRSVPFilterSpec(senderTemplate, src_address, src_port);

	senderTemplate->header.class_num = RSVP_CLASS_SENDER_TEMPLATE;
}

void initRSVPSenderTSpec(RSVPSenderTSpec* senderTSpec,
	float token_bucket_rate,
	float token_bucket_size,
	float peak_data_rate,
	uint32_t minimum_policed_unit,
	uint32_t maximum_packet_size)
{
	initRSVPObjectHeader(&senderTSpec->header, RSVP_CLASS_SENDER_TSPEC, 2);

	senderTSpec->nothing_1 = 0;
	senderTSpec->overall_length = htons(7);
	senderTSpec->service_header = 1;
	senderTSpec->nothing_2 = 0;
	senderTSpec->service_data_length = htons(6);
	senderTSpec->parameter_id = 127;
	senderTSpec->flags = 0;
	senderTSpec->parameter_127_length = 5;
	senderTSpec->token_bucket_rate_float = htonl(* (uint32_t *) (&token_bucket_rate));
	senderTSpec->token_bucket_size_float = htonl(* (uint32_t *) (&token_bucket_size));
	senderTSpec->peak_data_rate_float = htonl(* (uint32_t *) (&peak_data_rate));
	senderTSpec->minimum_policed_unit = htonl(minimum_policed_unit);
	senderTSpec->maximum_packet_size = htonl(maximum_packet_size);

	return;
}

void* readRSVPCommonHeader(RSVPCommonHeader* commonHeader, uint8_t& msg_type, uint8_t& send_TTL, uint16_t& length)
{
	msg_type = commonHeader->msg_type;
	send_TTL = commonHeader->send_TTL;
	length = htons(commonHeader->RSVP_length);
	return commonHeader + 1;
}

void readRSVPObjectHeader(RSVPObjectHeader* header, uint8_t& class_num, uint8_t& c_type)
{
	class_num = header->class_num;
	c_type = header->c_type;
	return;
}

void* readRSVPSession(RSVPSession* session, in_addr& destinationAddress, uint8_t& protocol_id, bool& police, uint16_t& dst_port)
{
	destinationAddress = session->IPv4_dest_address;
	protocol_id = session->protocol_id;
	police = session->flags & 0x01;
	dst_port = htons(session->dst_port);
	//click_chatter("SESSION OBJECT DATA: dest addr: %s, protocol id: %d, police: %d, dst port: %d", IPAddress(destinationAddress).s().c_str(), protocol_id, police, dst_port);
	return session + 1;
}

void* readRSVPHop(RSVPHop* hop, in_addr& next_previous_hop_address, uint32_t& logical_interface_handle)
{
	next_previous_hop_address = hop->IPv4_next_previous_hop_address;
	logical_interface_handle = htonl(hop->logical_interface_handle);
	//click_chatter("Hop OBJECT DATA: next: %s, lih: %d", IPAddress(next_previous_hop_address).s().c_str(), logical_interface_handle);
	return hop + 1;
}

void* readRSVPTimeValues(RSVPTimeValues* timeValues, uint32_t& refresh_period_r)
{
	refresh_period_r = htonl(timeValues->refresh_period_r);
	//click_chatter("TimeValues OBJECT DATA: refresh period: %d", refresh_period_r);
	return timeValues + 1;
}

void* readRSVPStyle(RSVPStyle* style)
{
	return style + 1;
}

void* readRSVPErrorSpec(RSVPErrorSpec* errorSpec, in_addr& error_node_address, bool& inPlace, bool& notGuilty, uint8_t& errorCode, uint16_t& errorValue)
{
	error_node_address = errorSpec->IPv4_error_node_address;
	inPlace = errorSpec->flags & 0x1;
	notGuilty = errorSpec->flags & 0x2;
	errorCode = errorSpec->error_code;
	errorValue = htons(errorSpec->error_value);
	//click_chatter("ErrorSpec OBJECT DATA: node_address: %s, inPlace: %d, notGuilty: %d, errorCode: %d, errorValue: %d", IPAddress(error_node_address).s().c_str(), inPlace, notGuilty, errorCode, errorValue);
	return errorSpec + 1;
}

void* readRSVPResvConf(RSVPResvConf* resvConf, in_addr& receiverAddress)
{
	receiverAddress = resvConf->receiver_address;
	//click_chatter("ResvConf OBJECT DATA: receiverAddr: %s", IPAddress(receiverAddress).s().c_str());
	return resvConf + 1;
}

void* readRSVPScope(RSVPObjectHeader* objectHeader, Vector<in_addr>& src_addresses)
{
	unsigned length = htons(objectHeader->length);
	unsigned nrAddresses = (length - sizeof(RSVPObjectHeader)) / 4;
	//click_chatter("readRSVPScope: number of addresses: %d", nrAddresses);
	
	in_addr* addr = (in_addr*) (objectHeader + 1);
	
	for (int i = 0; i < nrAddresses; ++i) {
		src_addresses.push_back(*addr);
		
		//click_chatter("Scope OBJECT DATA: src_addresses: %s", IPAddress(*addr).s().c_str());
		addr++;
	}
	return addr;
}

void* readRSVPFlowspec(RSVPFlowspec* flowSpec,
	float& token_bucket_rate,
	float& token_bucket_size,
	float& peak_data_rate,
	uint32_t& minimum_policed_unit,
	uint32_t& maximum_packet_size)
{
	uint32_t tbr = htonl(flowSpec->token_bucket_rate_float),
		tbs = htonl(flowSpec->token_bucket_size_float),
		pdr = htonl(flowSpec->peak_data_rate_float);
	token_bucket_rate    = *(float*) &tbr;
	token_bucket_size    = *(float*) &tbs;
	peak_data_rate       = *(float*) &pdr;
	minimum_policed_unit = htonl(flowSpec->minimum_policed_unit);
	maximum_packet_size  = htonl(flowSpec->maximum_packet_size);
	//click_chatter("Flowspec OBJECT DATA: bucket rate: %f, bucket size: %f, peak rate: %f, min policed: %d, max size: %d", token_bucket_rate, token_bucket_size, peak_data_rate, minimum_policed_unit, maximum_packet_size);
	return flowSpec + 1;
}

void* readRSVPFilterSpec(RSVPFilterSpec* filterSpec, in_addr& src_address, uint16_t& src_port)
{
	src_address = filterSpec->src_address;
	src_port    = htons(filterSpec->src_port);
	//click_chatter("FilterSpec OBJECT DATA: src: %s, port: %d", IPAddress(src_address).s().c_str(), src_port);
	return filterSpec + 1;
}

void* readRSVPSenderTemplate(RSVPSenderTemplate* senderTemplate, in_addr& src_address, uint16_t& src_port)
{
	src_address = senderTemplate->src_address;
	src_port    = htons(senderTemplate->src_port);
	//click_chatter("SenderTemplate OBJECT DATA: src: %s, port: %d", IPAddress(src_address).s().c_str(), src_port);
	return senderTemplate + 1;
}

void* readRSVPSenderTSpec(RSVPSenderTSpec* senderTSpec,
	float& token_bucket_rate,
	float& token_bucket_size,
	float& peak_data_rate,
	uint32_t& minimum_policed_unit,
	uint32_t& maximum_packet_size)
{
	uint32_t tbr = htonl(senderTSpec->token_bucket_rate_float),
		tbs = htonl(senderTSpec->token_bucket_size_float),
		pdr = htonl(senderTSpec->peak_data_rate_float);
	token_bucket_rate    = *(float*) &tbr;
	token_bucket_size    = *(float*) &tbs;
	peak_data_rate       = *(float*) &pdr;
	minimum_policed_unit = htonl(senderTSpec->minimum_policed_unit);
	maximum_packet_size  = htonl(senderTSpec->maximum_packet_size);
	//click_chatter("Flowspec OBJECT DATA: bucket rate: %f, bucket size: %f, peak rate: %f, min policed: %d, max size: %d", token_bucket_rate, token_bucket_size, peak_data_rate, minimum_policed_unit, maximum_packet_size);
	return senderTSpec + 1;
}


void RSVPElement::push(int, Packet *packet) {
	const void* p = packet->data();
	const void* end_data = packet->end_data();
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	p = readRSVPCommonHeader((RSVPCommonHeader*) p, msg_type, send_TTL, length);
	
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
				//click_chatter("class SESSION");
				p = readRSVPSession((RSVPSession*) p, d, a, c, b);
				break;
			case RSVP_CLASS_RSVP_HOP:
				//click_chatter("class HOP");
				p = readRSVPHop((RSVPHop*) p, d, e);
				break;
			case RSVP_CLASS_TIME_VALUES:
				//click_chatter("class VALUES");
				p = readRSVPTimeValues((RSVPTimeValues*) p, e);
				break;
			case RSVP_CLASS_ERROR_SPEC:
				//click_chatter("class SPEC");
				p = readRSVPErrorSpec((RSVPErrorSpec*) p, d, c, cc, a, b);
				break;
			case RSVP_CLASS_STYLE:
				//click_chatter("class STYLE");
				p = readRSVPStyle((RSVPStyle*) p);
				break;
			case RSVP_CLASS_SCOPE:
				//click_chatter("class SCOPE");
				p = readRSVPScope((RSVPObjectHeader*) p, srcs);
				break;
			case RSVP_CLASS_FLOWSPEC:
				//click_chatter("class FLOWSPEC");
				p = readRSVPFlowspec((RSVPFlowspec*) p,f,ff,fff, e, ee);
				break;
			case RSVP_CLASS_FILTER_SPEC:
				//click_chatter("class SPEC");
				p = readRSVPFilterSpec((RSVPFilterSpec*) p, d, b);
				break;
			case RSVP_CLASS_SENDER_TEMPLATE:
				//click_chatter("class TEMPLATE");
				p = readRSVPSenderTemplate((RSVPSenderTemplate*) p, d, b);
				break;
			case RSVP_CLASS_SENDER_TSPEC:
				//click_chatter("class TSPEC");
				p =  readRSVPSenderTSpec((RSVPSenderTSpec*) p,f,ff,fff,e,ee);
				break;
			case RSVP_CLASS_RESV_CONF:
				//click_chatter("class CONF");
				p = readRSVPResvConf((RSVPResvConf*) p, d);
				break;
			default:
				click_chatter("class_num %d not found", class_num);
		}
	}
	
	// updateState();

	output(0).push(packet);	
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
