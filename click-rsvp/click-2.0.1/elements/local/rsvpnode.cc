#include <click/config.h>
#include <click/confparse.hh>
#include <click/error.hh>
#include "rsvpnode.hh"
#include "rsvpelement.hh"
#include <clicknet/ether.h>
#include <clicknet/ip.h>
#include <clicknet/udp.h>
#include <stdexcept>
#include <cassert>

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
click_chatter("SET PROTOCOL ID TO %d AND DST PORT TO %d", protocol_id, dst_port);
	return;
}

void initRSVPSession(RSVPSession* session, const RSVPNodeSession* nodeSession)
{
	initRSVPSession(session, nodeSession->_dst_ip_address, nodeSession->_protocol_id, false, nodeSession->_dst_port);
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

void initRSVPFlowspec(RSVPFlowspec* flowspec, const RSVPSenderTSpec* senderTSpec) {
	flowspec->nothing_1 = 0;
	flowspec->overall_length = htons(7);
	flowspec->service_header = 1;
	flowspec->nothing_2 = 0;
	flowspec->controlled_load_data_length = htons(6);
	flowspec->parameter_id = 127;
	flowspec->flags = 0;
	flowspec->parameter_127_length = 5;
	flowspec->token_bucket_rate_float = senderTSpec->token_bucket_rate_float;
	flowspec->token_bucket_size_float = senderTSpec->token_bucket_size_float;
	flowspec->peak_data_rate_float = senderTSpec->peak_data_rate_float;
	flowspec->minimum_policed_unit = senderTSpec->minimum_policed_unit;
	flowspec->maximum_packet_size = senderTSpec->maximum_packet_size;
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

const void* readRSVPCommonHeader(const RSVPCommonHeader* commonHeader, uint8_t* msg_type, uint8_t* send_TTL, uint16_t* length)
{
	if (msg_type)
		*msg_type = commonHeader->msg_type;
	if (send_TTL)
		*send_TTL = commonHeader->send_TTL;
	if (length)
		*length = htons(commonHeader->RSVP_length);
	return commonHeader + 1;
}

const void readRSVPObjectHeader(const RSVPObjectHeader* header, uint8_t* class_num, uint8_t* c_type)
{
	if (class_num)
		*class_num = header->class_num;
	if (c_type)
		*c_type = header->c_type;
	return;
}

const void* readRSVPSession(const RSVPSession* session, in_addr* destinationAddress, uint8_t* protocol_id, bool* police, uint16_t* dst_port)
{
	if (destinationAddress)
		*destinationAddress = session->IPv4_dest_address;
	if (protocol_id)
		*protocol_id = session->protocol_id;
	if (police)
		*police = session->flags & 0x01;
	if (dst_port)
		*dst_port = htons(session->dst_port);

	return session + 1;
}

const void* readRSVPHop(const RSVPHop* hop, in_addr* next_previous_hop_address, uint32_t* logical_interface_handle)
{
	if (next_previous_hop_address)
		*next_previous_hop_address = hop->IPv4_next_previous_hop_address;
	if (logical_interface_handle)
		*logical_interface_handle = htonl(hop->logical_interface_handle);
	//click_chatter("Hop OBJECT DATA: next: %s, lih: %d", IPAddress(next_previous_hop_address).s().c_str(), logical_interface_handle);
	return hop + 1;
}

const void* readRSVPTimeValues(const RSVPTimeValues* timeValues, uint32_t* refresh_period_r)
{
	if (refresh_period_r)
		*refresh_period_r = htonl(timeValues->refresh_period_r);
	//click_chatter("TimeValues OBJECT DATA: refresh period: %d", refresh_period_r);
	return timeValues + 1;
}

const void* readRSVPStyle(const RSVPStyle* style)
{
	return style + 1;
}

const void* readRSVPErrorSpec(const RSVPErrorSpec* errorSpec, in_addr* error_node_address, bool* inPlace, bool* notGuilty, uint8_t* errorCode, uint16_t* errorValue)
{
	if (error_node_address)
		*error_node_address = errorSpec->IPv4_error_node_address;
	if (inPlace)
		*inPlace = errorSpec->flags & 0x1;
	if (notGuilty)
		*notGuilty = errorSpec->flags & 0x2;
	if (errorCode)
		*errorCode = errorSpec->error_code;
	if (errorValue)
		*errorValue = htons(errorSpec->error_value);
	//click_chatter("ErrorSpec OBJECT DATA: node_address: %s, inPlace: %d, notGuilty: %d, errorCode: %d, errorValue: %d", IPAddress(error_node_address).s().c_str(), inPlace, notGuilty, errorCode, errorValue);
	return errorSpec + 1;
}

const void* readRSVPResvConf(const RSVPResvConf* resvConf, in_addr* receiverAddress)
{
	if (receiverAddress)
		*receiverAddress = resvConf->receiver_address;
	//click_chatter("ResvConf OBJECT DATA: receiverAddr: %s", IPAddress(receiverAddress).s().c_str());
	return resvConf + 1;
}

const void* readRSVPScope(const RSVPObjectHeader* objectHeader, Vector<in_addr>* src_addresses)
{
	unsigned length = htons(objectHeader->length);
	unsigned nrAddresses = (length - sizeof(RSVPObjectHeader)) / 4;
	//click_chatter("readRSVPScope: number of addresses: %d", nrAddresses);
	
	in_addr* addr = (in_addr*) (objectHeader + 1);
	
	for (int i = 0; i < nrAddresses; ++i) {
		if (src_addresses)
			src_addresses->push_back(*addr);
		
		//click_chatter("Scope OBJECT DATA: src_addresses: %s", IPAddress(*addr).s().c_str());
		addr++;
	}
	return addr;
}

const void* readRSVPFlowspec(const RSVPFlowspec* flowSpec,
	float* token_bucket_rate,
	float* token_bucket_size,
	float* peak_data_rate,
	uint32_t* minimum_policed_unit,
	uint32_t* maximum_packet_size)
{
	uint32_t tbr = htonl(flowSpec->token_bucket_rate_float),
		tbs = htonl(flowSpec->token_bucket_size_float),
		pdr = htonl(flowSpec->peak_data_rate_float);
	if (token_bucket_rate)
		*token_bucket_rate = *(float*) &tbr;
	if (token_bucket_size)
		*token_bucket_size = *(float*) &tbs;
	if (peak_data_rate)
		*peak_data_rate = *(float*) &pdr;
	if (minimum_policed_unit)
		*minimum_policed_unit = htonl(flowSpec->minimum_policed_unit);
	if (maximum_packet_size)
		*maximum_packet_size = htonl(flowSpec->maximum_packet_size);
	//click_chatter("Flowspec OBJECT DATA: bucket rate: %f, bucket size: %f, peak rate: %f, min policed: %d, max size: %d", token_bucket_rate, token_bucket_size, peak_data_rate, minimum_policed_unit, maximum_packet_size);
	return flowSpec + 1;
}

const void* readRSVPFilterSpec(const RSVPFilterSpec* filterSpec, in_addr* src_address, uint16_t* src_port)
{
	if (src_address)
		*src_address = filterSpec->src_address;
	if (src_port)
		*src_port = htons(filterSpec->src_port);
	//click_chatter("FilterSpec OBJECT DATA: src: %s, port: %d", IPAddress(src_address).s().c_str(), src_port);
	return filterSpec + 1;
}

const void* readRSVPSenderTemplate(const RSVPSenderTemplate* senderTemplate, in_addr* src_address, uint16_t* src_port)
{
	return readRSVPFilterSpec(senderTemplate, src_address, src_port);
}

const void* readRSVPSenderTSpec(const RSVPSenderTSpec* senderTSpec,
	float* token_bucket_rate,
	float* token_bucket_size,
	float* peak_data_rate,
	uint32_t* minimum_policed_unit,
	uint32_t* maximum_packet_size)
{
	uint32_t tbr = htonl(senderTSpec->token_bucket_rate_float),
		tbs = htonl(senderTSpec->token_bucket_size_float),
		pdr = htonl(senderTSpec->peak_data_rate_float);
	if (token_bucket_rate)
		*token_bucket_rate    = *(float*) &tbr;
	if (token_bucket_size)
		*token_bucket_size    = *(float*) &tbs;
	if (peak_data_rate)
		*peak_data_rate       = *(float*) &pdr;
	if (minimum_policed_unit)
		*minimum_policed_unit = htonl(senderTSpec->minimum_policed_unit);
	if (maximum_packet_size)
		*maximum_packet_size  = htonl(senderTSpec->maximum_packet_size);
	return senderTSpec + 1;
}

RSVPNodeSession::RSVPNodeSession() : _own(false) {
	setKey();
}

RSVPNodeSession::RSVPNodeSession(in_addr dst_addr, uint8_t protocol_id, uint8_t dst_port) : _own(false), _dst_ip_address(dst_addr), _protocol_id(protocol_id), _dst_port(dst_port) {
	setKey();
	// key = IPAddress(_dst_ip_address).unparse() + String(_protocol_id) + String(_dst_port);
}

RSVPNodeSession::RSVPNodeSession(const RSVPSession& session) : _own(false) {
	readRSVPSession(const_cast<RSVPSession*>(&session), &_dst_ip_address, &_protocol_id, NULL, &_dst_port);
	setKey();
}

RSVPNodeSession::key_const_reference RSVPNodeSession::hashcode() const {
	return key;
	//return *reinterpret_cast<const long unsigned int*>(&_dst_ip_address);
}

void RSVPNodeSession::setKey() {
	key = *reinterpret_cast<const long unsigned int*>(&_dst_ip_address);
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
	// packet analysis
	
	in_addr srcIP, dstIP;
	click_ip* ip_header = (click_ip *) (packet->network_header());
	srcIP = ip_header->ip_src;
	dstIP = ip_header->ip_dst;
	
	uint8_t msg_type, send_TTL;
	uint16_t length;
	WritablePacket* forward;
	
	readRSVPCommonHeader((RSVPCommonHeader*) packet->transport_header(), &msg_type, &send_TTL, &length);

	RSVPSession* session = (RSVPSession *) RSVPObjectOfType(packet, RSVP_CLASS_SESSION);
	
	if (msg_type == RSVP_MSG_PATH) {
		forward = updatePathState(packet->clone());
		addIPHeader(forward, dstIP, srcIP, _tos);
		
		packet->kill();
		output(0).push(forward);
	} else if (msg_type == RSVP_MSG_RESV) {
		RSVPFilterSpec* filterSpec = (RSVPFilterSpec *) RSVPObjectOfType(packet, RSVP_CLASS_FILTER_SPEC);
		RSVPFlowspec* flowspec = (RSVPFlowspec *) RSVPObjectOfType(packet, RSVP_CLASS_FLOWSPEC);
		uint32_t refresh_period_r;
		readRSVPTimeValues((RSVPTimeValues *) RSVPObjectOfType(packet, RSVP_CLASS_TIME_VALUES), &refresh_period_r);
		
		updateReservation(*session, *filterSpec, *flowspec, refresh_period_r);

		forward = packet->uniqueify();
		RSVPHop* hop = (RSVPHop *) RSVPObjectOfType(forward, RSVP_CLASS_RSVP_HOP);
		addIPHeader(forward, hop->IPv4_next_previous_hop_address, IPAddress("0.0.0.0"), _tos);
		hop->IPv4_next_previous_hop_address = _myIP;

		packet->kill();
		output(0).push(forward);
	}
	click_chatter("pushing packet %p out the door", (void *) packet);
}

WritablePacket* RSVPNode::updatePathState(Packet* packet) {
	//click_chatter("packet %p entering updatePathState", (void *) packet);
	//click_chatter("updatePathState: start");
	WritablePacket* wp = packet->uniqueify();

	//click_chatter("packet, after uniquefying: %p", (void *) packet);
	const void* p = packet->data();
	RSVPCommonHeader* commonHeader = (RSVPCommonHeader*) p;
	p = (void*) (commonHeader + 1);
	RSVPObjectHeader* header;
	uint8_t class_num;
	
	// get the necessary objects from the packet
	RSVPSenderTemplate* senderTemplate = (RSVPSenderTemplate *) RSVPObjectOfType(wp, RSVP_CLASS_SENDER_TEMPLATE);
	RSVPSenderTSpec* senderTSpec =  (RSVPSenderTSpec *) RSVPObjectOfType(wp, RSVP_CLASS_SENDER_TSPEC);
	RSVPHop* hop = (RSVPHop *) RSVPObjectOfType(wp, RSVP_CLASS_RSVP_HOP);
	RSVPTimeValues* timeValues = (RSVPTimeValues *) RSVPObjectOfType(wp, RSVP_CLASS_TIME_VALUES);
	RSVPSession* session = (RSVPSession *) RSVPObjectOfType(wp, RSVP_CLASS_SESSION);

	// create the key to look for in the hash table
	RSVPNodeSession nodeSession(*session);
	HashTable<RSVPNodeSession, RSVPPathState>::iterator it = _pathStates.find(nodeSession);
	if (it != _pathStates.end() && it->second.timer) {
		click_chatter("updatePathState: found table entry");
		Timer* t = it->second.timer;
		click_chatter("%s t: %p", _name.c_str(), (void *) t);
		// unschedule running timer so it won't run out
		it->second.timer->unschedule();
		delete it->second.timer;
		it->second.timer = NULL;
	}
	//click_chatter("updatePathState: setting table entry");

	// make new path state
	RSVPPathState pathState;
	pathState.previous_hop_node = hop->IPv4_next_previous_hop_address;
	if (senderTemplate && senderTSpec) {
		pathState.senderTemplate = *senderTemplate;
		pathState.senderTSpec = *senderTSpec;
	}

	uint32_t refresh_period_r;
	readRSVPTimeValues(timeValues, &refresh_period_r);
	//click_chatter("updatePathState: set table entry stuff");
	//click_chatter("updatePathState: read refresh period");
	
	// schedule new timer
	pathState.timer = new Timer(this);
	pathState.timer->initialize(this);
	pathState.timer->schedule_after_sec(refresh_period_r); //updatereser TODO: change !!!11
	
	// add new / updated path state to path state table
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
	
	packet->kill();

	return wp;
}

WritablePacket* RSVPNode::updateReservation(const RSVPNodeSession& session, const RSVPFilterSpec& filterSpec, const RSVPFlowspec& flowspec, uint32_t refresh_period_r) {
	
	HashTable<RSVPNodeSession, RSVPResvState>::iterator it = find(_resvStates, session);
	if (it != _resvStates.end() && it->second.timer) {
		it->second.timer->unschedule();
		delete it->second.timer;
		it->second.timer = NULL;
	}

	RSVPResvState resvState;
	resvState.filterSpec = filterSpec;
	resvState.flowspec = flowspec;
	resvState.refresh_period_r = refresh_period_r;
	resvState.timer = new Timer(this);
	resvState.timer->initialize(this);
	resvState.timer->schedule_after_sec(refresh_period_r); // TODO
	
	_resvStates.set(session, resvState);

	return NULL;
}

int RSVPNode::initialize(ErrorHandler* errh) {
	// find all qos classifiers
	/*Vector<RSVPQoSClassifier> classifiers;
	
	StringAccum name;
	Element* e;
	int i = 0;
	do {
		name << "qos_cl" << i;
		click_chatter("looking for %s", name.c_str());
		e = this->router()->find(name.c_str(), this);
		classifiers.push_back((RSVPQoSClassifier *) e);
		name.clear();
	 } while (e != NULL);
	 
	 // last element is NULL, so remove it
	 classifiers.pop_back();*/
	 return 0;
}

void RSVPNode::run_timer(Timer* timer) {
	RSVPNodeSession* session;
	if (session = (RSVPNodeSession *) sessionForPathStateTimer(timer)) {
		_pathStates.erase(find(_pathStates, *session));
		click_chatter("Deleted path state from %s", _name.c_str());
		delete timer;
	}else if (session = (RSVPNodeSession *) sessionForResvStateTimer(timer)) {
		_resvStates.erase(find(_resvStates, *session));
		click_chatter("Deleted resv state from %s", _name.c_str());
	} // TODO
}

const RSVPNodeSession* RSVPNode::sessionForPathStateTimer(const Timer* timer) const {	
	for (HashTable<RSVPNodeSession, RSVPPathState>::const_iterator it = _pathStates.begin(); it != _pathStates.end(); it++) {
		if (it->second.timer == timer) {
			return &it->first;
		}
	}
	return NULL;
}

const RSVPNodeSession* RSVPNode::sessionForResvStateTimer(const Timer* timer) const {	
	for (HashTable<RSVPNodeSession, RSVPResvState>::const_iterator it = _resvStates.begin(); it != _resvStates.end(); it++) {
		if (it->second.timer == timer) {
			return &it->first;
		}
	}
	return NULL;
}

int RSVPNode::nameHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh) {
	RSVPElement* me = (RSVPElement*) e;
	if (cp_va_kparse(conf, me, errh, "NAME", cpkP + cpkM, cpString, &me->_name, cpEnd) < 0)
		return -1;
	
	click_chatter("Set host RSVP element name: %s", me->_name.c_str());
	
	return 0;
}

int RSVPNode::configure(Vector<String> &conf, ErrorHandler *errh) {
	if (cp_va_kparse(conf, this, errh, 
		"IP", cpkM + cpkP, cpIPAddress, &_myIP,
		cpEnd) < 0) return -1;
	return 0;
}

void RSVPNode::add_handlers() {
	add_write_handler("name", &nameHandle, (void *) 0);
}

void RSVPNode::addIPHeader(WritablePacket* p, in_addr dst_ip, in_addr src_ip, uint8_t tos) {
	if (IPAddress(src_ip) == IPAddress("0.0.0.0")) {
		p->set_user_anno_i(3, 1);
		src_ip = _myIP;
	}

	int transportSize = p->transport_header() ? p->end_data() - p->transport_header() : p->length();

	if (p->data() != p->network_header() || p->network_header() == 0) {
		p = p->push(sizeof(click_ip));
	}
	//p->set_network_header(p->data(), sizeof(click_ip));

	struct click_ip* ip = (click_ip *) p->data();
	memset(ip, 0, sizeof(click_ip));

	ip->ip_v = 4;
	ip->ip_hl = sizeof(click_ip) >> 2;

	ip->ip_tos = tos;
	ip->ip_len = htons(sizeof(click_ip) + transportSize);
	ip->ip_id = 0;
	ip->ip_off = 0;
	ip->ip_ttl = 200;
	ip->ip_p = 46; // RSVP
	ip->ip_src = src_ip;
	ip->ip_dst = dst_ip;
	p->set_dst_ip_anno(dst_ip);

	ip->ip_sum = click_in_cksum((const unsigned char*) ip, sizeof(click_ip));

}

CLICK_ENDDECLS
EXPORT_ELEMENT(RSVPNode)
