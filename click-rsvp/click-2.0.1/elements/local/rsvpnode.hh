#ifndef CLICK_RSVPNODE_HH
#define CLICK_RSVPNODE_HH
#include <click/element.hh>
#include <clicknet/ip.h>
#include <clicknet/ether.h>
#include <click/timer.hh>
#include <click/hashtable.hh>
#include <elements/ip/lookupiproute.hh>
#include <click/router.hh>

CLICK_DECLS

#define RSVP_MSG_PATH		    1
#define RSVP_MSG_RESV		    2
#define RSVP_MSG_PATHERR		3
#define RSVP_MSG_RESVERR		4
#define RSVP_MSG_PATHTEAR		5
#define RSVP_MSG_RESVTEAR		6
#define RSVP_MSG_RESVCONF		7

#define RSVP_CLASS_NULL                 0 //
#define RSVP_CLASS_SESSION              1 // all
#define RSVP_CLASS_RSVP_HOP             3 // path, resv, ptear, rtear, rerr
#define RSVP_CLASS_INTEGRITY            4 // [all]
#define RSVP_CLASS_TIME_VALUES          5 // path, resv
#define RSVP_CLASS_ERROR_SPEC           6 // perr, rerr, rconf
#define RSVP_CLASS_STYLE                8 // resv, rtear, rerr, rconf
#define RSVP_CLASS_SCOPE                7 // [resv], [rtear], [rerr]
#define RSVP_CLASS_FLOWSPEC             9 // [resv], [rtear], [[rerr]], [[rconf]]
#define RSVP_CLASS_FILTER_SPEC          10 // [resv], [rtear], [rerr], [rconf]
#define RSVP_CLASS_SENDER_TEMPLATE      11 // [path], [ptear], [perr]
#define RSVP_CLASS_SENDER_TSPEC         12 // [path], [ptear], [perr]
//ADSPEC NIET
#define RSVP_CLASS_ADSPEC               13 //
#define RSVP_CLASS_POLICY_DATA          14 // [path], [resv], [perr], [rerr]
#define RSVP_CLASS_RESV_CONF            15 // [resv], rconf

struct RSVPCommonHeader {
	// RSVPCommonHeader() : vers(1), flags(0), RSVP_checksum(0), reserved(0) {}

#if CLICK_BYTE_ORDER == CLICK_BIG_ENDIAN
	unsigned    vers : 4;
	unsigned    flags : 4;
#elif CLICK_BYTE_ORDER == CLICK_LITTLE_ENDIAN
	unsigned    flags : 4;
	unsigned    vers : 4;
#endif
	uint8_t     msg_type;
	uint16_t    RSVP_checksum;
	uint8_t     send_TTL;
	uint8_t     reserved;
	uint16_t    RSVP_length;
};

struct RSVPObjectHeader {
	uint16_t length;
	uint8_t class_num;
	uint8_t c_type;
};

struct RSVPSession { // class num = 1, C-type = 1
	RSVPObjectHeader header;
	in_addr IPv4_dest_address;
	uint8_t protocol_id;
	uint8_t flags; // 0x01 = E_Police flag
	uint16_t dst_port;
};

struct RSVPHop { // class num = 3, C-type = 1
	RSVPObjectHeader header;
	in_addr IPv4_next_previous_hop_address;
	uint32_t logical_interface_handle;
};

struct RSVPIntegrity { // class num = 4, C-type = 1
	RSVPObjectHeader header;
	unsigned flags : 4;
	unsigned reserved : 4;
	uint64_t key_identifier : 48;
	uint64_t sequence_number;
	uint64_t keyed_message_digest_1;
	uint64_t keyed_message_digest_2;
};

struct RSVPTimeValues { // class num = 5, C-type = 1
	RSVPObjectHeader header;
	uint32_t refresh_period_r;
};

struct RSVPErrorSpec { // class num = 6, C-type = 1
	RSVPObjectHeader header;
	in_addr IPv4_error_node_address;
	uint8_t flags;
	uint8_t error_code;
	uint16_t error_value;
};

// scope class: class num = 7, C-type = 1
// array of in_addrs

struct RSVPStyle { // class num = 8, C-type = 1
	RSVPObjectHeader header;
	uint8_t flags;
	uint32_t option_vector : 24;
};

struct RSVPFlowspec { // class num = 9
	RSVPObjectHeader header;
	uint16_t nothing_1;
	uint16_t overall_length; // 7 words not including header
	uint8_t service_header; // service number 5
	uint8_t nothing_2;
	uint16_t controlled_load_data_length; // 6 words not including per-service header
	uint8_t parameter_id; // 127
	uint8_t flags; // 0
	uint16_t parameter_127_length; // 5 words not including header;
	uint32_t token_bucket_rate_float;
	uint32_t token_bucket_size_float;
	uint32_t peak_data_rate_float;
	uint32_t minimum_policed_unit;
	uint32_t maximum_packet_size;
};

struct RSVPFilterSpec { // class num = 10, C-type = 1
	RSVPObjectHeader header;
	in_addr src_address;
	uint16_t nothing;
	uint16_t src_port;
};

typedef RSVPFilterSpec RSVPSenderTemplate; // class num = 11, C-type = 1

struct RSVPSenderTSpec { // class num = 12, C-type = 2
	RSVPObjectHeader header;
	uint16_t nothing_1;
	uint16_t overall_length; // 7 words not including header
	uint8_t service_header; // 1
	uint8_t nothing_2;
	uint16_t service_data_length; // 6 words not counting header
	uint8_t parameter_id; // 127
	uint8_t flags; // 0
	uint16_t parameter_127_length; // 5 words not including header
	uint32_t token_bucket_rate_float;
	uint32_t token_bucket_size_float;
	uint32_t peak_data_rate_float;
	uint32_t minimum_policed_unit;
	uint32_t maximum_packet_size;
};

// RSVPPolicyData class num = 14, C-type = 1

bool operator==(const RSVPSenderTSpec&, const RSVPSenderTSpec&);
bool operator!=(const RSVPSenderTSpec&, const RSVPSenderTSpec&);

bool operator==(const RSVPFlowspec&, const RSVPFlowspec&);
bool operator!=(const RSVPFlowspec&, const RSVPFlowspec&);

struct RSVPResvConf { // class num = 15, C-type = 1
	RSVPObjectHeader header;
	in_addr receiver_address;
};

// used to locally store a session
struct RSVPNodeSession {
	RSVPNodeSession();
	RSVPNodeSession(in_addr, uint8_t protocol_id, uint16_t dst_port);
	RSVPNodeSession(const RSVPSession&);
	typedef long unsigned int key_type;
	typedef const key_type& key_const_reference;
	const key_type& hashcode() const;
	key_type key;
	void setKey();
	bool operator==(const RSVPNodeSession& other) const;
	in_addr _dst_ip_address;
	uint8_t _protocol_id;
	uint16_t _dst_port;
	bool _own;
};

struct RSVPPathState {
	in_addr previous_hop_node;
	RSVPSenderTSpec senderTSpec;
	RSVPSenderTemplate senderTemplate;
	uint32_t refresh_period_r;
	Timer* timer;
};

struct RSVPResvState {
	RSVPFilterSpec filterSpec;
	RSVPFlowspec flowspec;
	uint32_t refresh_period_r;
	bool confirm;
	Timer* timer;
};

struct RSVPSender {
	RSVPSender();
	RSVPSender(in_addr _src_address, uint16_t _src_port);
	RSVPSender(const RSVPSenderTemplate&);
	typedef long unsigned int key_type;
	typedef const key_type& key_const_reference;
	key_const_reference hashcode() const;
	key_type key;
	bool operator==(const RSVPSender& other) const;
	IPAddress src_address;
	uint16_t src_port;
};

uint16_t sizeofRSVPObject(uint8_t class_num, uint8_t c_type);
uint16_t sizeofRSVPScopeObject(size_t num_addresses);
const RSVPObjectHeader* nextRSVPObject(const RSVPObjectHeader*);
const void* RSVPObjectOfType(Packet*, uint8_t wanted_class_num);

void initRSVPCommonHeader(RSVPCommonHeader*, uint8_t msg_type, uint8_t send_TTL, uint16_t length);
void initRSVPObjectHeader(RSVPObjectHeader*, uint8_t class_num, uint8_t c_type);
void initRSVPSession(RSVPSession*, in_addr destinationAddress, uint8_t protocol_id, bool police, uint16_t dst_port);
void initRSVPSession(RSVPSession*, const RSVPNodeSession*);
void initRSVPHop(RSVPHop*, in_addr next_previous_hop_address, uint32_t logical_interface_handle);
void initRSVPTimeValues(RSVPTimeValues*, uint32_t refresh_period_r);
void initRSVPStyle(RSVPStyle*);
void initRSVPErrorSpec(RSVPErrorSpec*, in_addr error_node_address, bool inPlace, bool notGuilty, uint8_t errorCode, uint16_t errorValue);
void initRSVPResvConf(RSVPResvConf*, in_addr receiverAddress);
void* initRSVPScope(RSVPObjectHeader*, const Vector<in_addr>& src_addresses);
void initRSVPFlowspec(RSVPFlowspec*,
	float token_bucket_rate,
	float token_bucket_size,
	float peak_data_rate,
	uint32_t minimum_policed_unit,
	uint32_t maximum_packet_size);
void initRSVPFlowspec(RSVPFlowspec*, const RSVPSenderTSpec*);
void initRSVPFilterSpec(RSVPFilterSpec*, in_addr src_address, uint16_t src_port);
void initRSVPSenderTemplate(RSVPSenderTemplate*, in_addr src_address, uint16_t src_port);
void initRSVPSenderTemplate(RSVPSenderTemplate*, const RSVPSender&);
void initRSVPSenderTSpec(RSVPSenderTSpec*,
	float token_bucket_rate,
	float token_bucket_size,
	float peak_data_rate,
	uint32_t minimum_policed_unit,
	uint32_t maximum_packet_size);
	
const void* readRSVPCommonHeader(const RSVPCommonHeader*, uint8_t* msg_type, uint8_t* send_TTL, uint16_t* length);
const void readRSVPObjectHeader(const RSVPObjectHeader*, uint8_t* class_num, uint8_t* c_type);
const void* readRSVPSession(const RSVPSession*, in_addr* destinationAddress, uint8_t* protocol_id, bool* police, uint16_t* dst_port);
const void* readRSVPHop(const RSVPHop*, in_addr* next_previous_hop_address, uint32_t* logical_interface_handle);
const void* readRSVPTimeValues(const RSVPTimeValues*, uint32_t* refresh_period_r);
const void* readRSVPStyle(const RSVPStyle*);
const void* readRSVPErrorSpec(const RSVPErrorSpec*, in_addr* error_node_address, bool* inPlace, bool* notGuilty, uint8_t* errorCode, uint16_t* errorValue);
const void* readRSVPResvConf(const RSVPResvConf*, in_addr* receiverAddress);
const void* readRSVPScope(const RSVPObjectHeader*, Vector<in_addr>* src_addresses);
const void* readRSVPFlowspec(const RSVPFlowspec*,
	float* token_bucket_rate,
	float* token_bucket_size,
	float* peak_data_rate,
	uint32_t* minimum_policed_unit,
	uint32_t* maximum_packet_size);
void* readRSVPFilterSpec(RSVPFilterSpec*, in_addr* src_address, uint16_t* src_port);
void* readRSVPSenderTemplate(RSVPSenderTemplate*, in_addr* src_address, uint16_t* src_port);
void* readRSVPSenderTSpec(RSVPSenderTSpec*,
	float* token_bucket_rate,
	float* token_bucket_size,
	float* peak_data_rate,
	uint32_t* minimum_policed_unit,
	uint32_t* maximum_packet_size);

template<typename K, typename T>
typename HashTable<K, T>::const_iterator find(const HashTable<K, T>& table, const K& k) {
	for (typename HashTable<K, T>::const_iterator it = table.begin(); it != table.end(); it++) {
		if (k == it->first) {
			return it;
		}
	}
	return table.end();
}

template<typename K, typename T>
typename HashTable<K, T>::iterator find(HashTable<K, T>& table, const K& k) {
	for (typename HashTable<K, T>::iterator it = table.begin(); it != table.end(); it++) {
		if (k == it->first) {
			return it;
		}
	}
	return table.end();
}

class RSVPNode: public Element { 
public:
	RSVPNode();
	~RSVPNode();
	
	virtual void push(int port, Packet* packet);
	void updatePathState(Packet*);
	void updateReservation(const RSVPNodeSession&, const RSVPFilterSpec*, const RSVPFlowspec*, uint32_t refresh_period_r);

	virtual void createSession(const RSVPNodeSession&);
	virtual void erasePathState(const RSVPNodeSession&, const RSVPSender&);
	virtual void eraseResvState(const RSVPNodeSession&, const RSVPSender&);

	virtual void removeAllState();

	virtual const RSVPPathState* pathState(const RSVPNodeSession&, const RSVPSender&) const;
	virtual const RSVPResvState* resvState(const RSVPNodeSession&, const RSVPSender&) const;
	RSVPPathState* pathState(const RSVPNodeSession&, const RSVPSender&);
	RSVPResvState* resvState(const RSVPNodeSession&, const RSVPSender&);

	bool hasReservation(const RSVPNodeSession&, const RSVPSender&) const;

	template<typename S>
	String stateTableToString(const HashTable<RSVPNodeSession, HashTable<RSVPSender, S> >&, String name) const;

	int initialize(ErrorHandler* errh);

	void run_timer(Timer*);
	const RSVPNodeSession* sessionForPathStateTimer(const Timer*, const RSVPSender** sender) const;
	const RSVPNodeSession* sessionForResvStateTimer(const Timer*, const RSVPSender** sender) const;
	
	const char *class_name() const	{ return "RSVPNode"; }
	const char *port_count() const	{ return "1/1"; }
	const char *processing() const	{ return PUSH; }
	
	static String pathStateTableHandle(Element *e, void *thunk);
	static String resvStateTableHandle(Element *e, void *thunk);
	static int dieHandle(const String& conf, Element *e, void *thunk, ErrorHandler *errh);
	static int nameHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh);

	virtual void die();

	int configure(Vector<String>&, ErrorHandler*);
	void add_handlers();

	void addIPHeader(WritablePacket*, in_addr dst_ip, in_addr src_ip, uint8_t tos) const;
protected:
	
	bool _dead;

	IPAddress ipForInterface(int port) const;

	Vector<IPAddress> _ips;
	LinearIPLookup* _ipLookup;
	
	int _tos;
	String _name;
	in_addr _myIP;
	HashTable<RSVPNodeSession, HashTable<RSVPSender, RSVPPathState> > _pathStates;
	HashTable<RSVPNodeSession, HashTable<RSVPSender, RSVPResvState> > _resvStates;
};

template <typename S>
String RSVPNode::stateTableToString(const HashTable<RSVPNodeSession, HashTable<RSVPSender, S> >& table, String type) const {
	String s = _name + ": " + type + " table contents: \n";
	for (typename HashTable<RSVPNodeSession, HashTable<RSVPSender, S> >::const_iterator it1 = table.begin(); it1 != table.end(); it1++) {
		const RSVPNodeSession& session = it1->first;
		const HashTable<RSVPSender, S>& subtable = it1->second;
		s += IPAddress(session._dst_ip_address).unparse() + "/" + String((int) session._protocol_id) + "/" + String((int) session._dst_port) + "\n";
		for (typename HashTable<RSVPSender, S>::const_iterator it2 = subtable.begin(); it2 != subtable.end(); it2++) {
			const RSVPSender& sender = it2->first;
			s += "\t" + IPAddress(sender.src_address).unparse() + "/" + String(sender.src_port) + "\n";
		}
	}
	
	return s;
}

CLICK_ENDDECLS
#endif // CLICK_RSVPNODE_HH
