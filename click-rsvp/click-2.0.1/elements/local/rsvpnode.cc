#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpnode.hh"
#include "rsvpelement.hh"
#include <clicknet/ether.h>
#include <clicknet/ip.h>
#include <clicknet/udp.h>
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


RSVPNodeSession::RSVPNodeSession(in_addr dst_addr, uint8_t protocol_id, uint8_t dst_port) : _dst_ip_address(dst_addr), _protocol_id(protocol_id), _dst_port(dst_port) {
	// key = IPAddress(_dst_ip_address).unparse() + String(_protocol_id) + String(_dst_port);
}

RSVPNodeSession::RSVPNodeSession(const RSVPSession& session) {
	bool b;
	readRSVPSession(const_cast<RSVPSession*>(&session), _dst_ip_address, _protocol_id, b, _dst_port);
}

RSVPNodeSession::key_const_reference RSVPNodeSession::hashcode() const {
	return *reinterpret_cast<const long unsigned int*>(&_dst_ip_address);
}

bool RSVPNodeSession::operator==(const RSVPNodeSession& other) const {
	return IPAddress(_dst_ip_address) == IPAddress(other._dst_ip_address)
		&& _protocol_id == other._protocol_id
		&& _dst_port == other._dst_port;
}

RSVPNode::RSVPNode()
{}

RSVPNode::~RSVPNode()
{}


void RSVPNode::push(int port, Packet* packet) {
	click_chatter("RSVPNode: Got a packet of size %d", packet->length());
	// packet analyzation
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	
	readRSVPCommonHeader((RSVPCommonHeader*) packet->data(), msg_type, send_TTL, length);
	if (msg_type == RSVP_MSG_PATH) {
		//click_chatter("RSVPNODE msg_type == PATH");
		packet = updatePathState(packet);
	} else if (msg_type == RSVP_MSG_RESV) {
		//click_chatter("RSVPNODE msg_type == RESV");
		
	}
	//click_chatter("RSVPNODE push pushing out my baby");
	click_chatter("pushing packet %p out the door", (void *) packet);
	output(0).push(packet);
	//click_chatter("RSVPNODE push pushed it out");
}

Packet* RSVPNode::updatePathState(Packet* packet) {
	click_chatter("packet %p entering updatePathState", (void *) packet);
	//click_chatter("updatePathState: start");
	WritablePacket* wp = packet->uniqueify();
	packet = wp;
	click_chatter("packet, after uniquefying: %p", (void *) packet);
	const void* p = packet->data();
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader*) p;
	p = (void*) (commonHeader + 1);
	RSVPObjectHeader* header;
	uint8_t class_num;
	
	RSVPSenderTemplate* senderTemplate = NULL;
	RSVPSenderTSpec* senderTSpec = NULL;
	RSVPHop* hop = NULL;
	RSVPTimeValues* timeValues = NULL;
	RSVPSession* session = NULL;
	
	//click_chatter("updatePathState: enter while loop");
	while (p < packet->end_data()) {
		header = (RSVPObjectHeader*) p;
		class_num = header->class_num;
		switch (class_num) {
			case RSVP_CLASS_SENDER_TEMPLATE:
				//click_chatter("updatePathState switch: TEMPLATE");
				senderTemplate = (RSVPSenderTemplate*) p;
			case RSVP_CLASS_SENDER_TSPEC:
				//click_chatter("updatePathState switch: TSPEC");
				senderTSpec = (RSVPSenderTSpec*) p;
			break;
			case RSVP_CLASS_RSVP_HOP:
				//click_chatter("updatePathState switch: HOP");
				hop = (RSVPHop*) p;
			break;
			case RSVP_CLASS_TIME_VALUES:
				//click_chatter("updatePathState switch: VALUES");
				timeValues = (RSVPTimeValues*) p;
			break;
			case RSVP_CLASS_SESSION:
				//click_chatter("updatePathState switch: SESSION");
				session = (RSVPSession*) p;
			default:
				//click_chatter("updatePathState switch: class num noope");
				;
				
		}
		p = (const void*) nextRSVPObject((RSVPObjectHeader*) p);
		//click_chatter("updatePathState: p = next rsvp object");
	}
	//click_chatter("updatePathState: left while loop");
	
	RSVPNodeSession nodeSession(session->IPv4_dest_address, session->protocol_id, session->dst_port);
	HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.find(nodeSession);
	if (it != _pathStates.end()) {
		//click_chatter("updatePathState: found table entry");
		it->second.timer->unschedule();
		delete it->second.timer;
	}
	//click_chatter("updatePathState: setting table entry");
	// make new
	RSVPPathState pathState;
	pathState.previous_hop_node = hop->IPv4_next_previous_hop_address;
	if (senderTemplate && senderTSpec) {
		pathState.senderTemplate = *senderTemplate;
		pathState.senderTSpec = *senderTSpec;
	}
	pathState.timer = new Timer(this);
	pathState.timer->initialize(this);
	//click_chatter("updatePathState: set table entry stuff");
	uint32_t refresh_period_r;
	readRSVPTimeValues(timeValues, refresh_period_r);
	//click_chatter("updatePathState: read refresh period");
	pathState.timer->schedule_after_sec(refresh_period_r); // TODO: change !!!11
	_pathStates.set(nodeSession, pathState);
	//click_chatter("updatePathState: set new refresh period");
	//click_chatter("Address: %s", IPAddress(hop->IPv4_next_previous_hop_address).unparse().c_str());
	hop->IPv4_next_previous_hop_address = _myIP;
	//click_chatter("Address: %s", IPAddress(hop->IPv4_next_previous_hop_address).unparse().c_str());
	//click_chatter("updatePathState: end");
	
	commonHeader->RSVP_checksum = click_in_cksum((unsigned char *) packet->data(), packet->length());
	
	click_chatter("Table contents:");
	for (HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.begin();
		it != _pathStates.end(); it++) {
		const RSVPNodeSession& nodeSession = it->first;
		const RSVPPathState& pathState = it->second;
		
		click_chatter("\tdst ip: %s", IPAddress(nodeSession._dst_ip_address).s().c_str());
		click_chatter("\tprotocol id: %d", nodeSession._protocol_id);
		click_chatter("\tdst port: %d", nodeSession._dst_port);
		click_chatter("\tprevious node address: %s", IPAddress(pathState.previous_hop_node).s().c_str());
	}
	
	return packet;
}

void RSVPNode::run_timer(Timer* timer) {
	click_chatter("timer ran out");
}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
	if (cp_va_kparse(conf, this, errh, 
		"IP", cpkM + cpkP, cpIPAddress, &_myIP,
		cpEnd) < 0) return -1;
	return 0;
}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
