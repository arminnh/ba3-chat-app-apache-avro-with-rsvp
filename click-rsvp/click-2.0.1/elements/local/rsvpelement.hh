#ifndef CLICK_RSVPELEMENT_HH
#define CLICK_RSVPELEMENT_HH
#include <click/element.hh>
#include <clicknet/ip.h>
#include <clicknet/ether.h>
#include <click/timer.hh>
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

struct RSVPResvConf { // class num = 15, C-type = 1
	RSVPObjectHeader header;
	in_addr receiver_address;
};

uint16_t sizeofRSVPObject(uint8_t class_num, uint8_t c_type);
uint16_t sizeofRSVPScopeObject(size_t num_addresses);
const RSVPObjectHeader* nextRSVPObject(const RSVPObjectHeader*);

void initRSVPCommonHeader(RSVPCommonHeader*, uint8_t msg_type, uint8_t send_TTL, uint16_t length);
void initRSVPObjectHeader(RSVPObjectHeader*, uint8_t class_num, uint8_t c_type);
void initRSVPSession(RSVPSession*, in_addr destinationAddress, uint8_t protocol_id, bool police, uint16_t dst_port);
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
void initRSVPFilterSpec(RSVPFilterSpec*, in_addr src_address, uint16_t src_port);
void initRSVPSenderTemplate(RSVPSenderTemplate*, in_addr src_address, uint16_t src_port);
void initRSVPSenderTSpec(RSVPSenderTSpec*,
	float token_bucket_rate,
	float token_bucket_size,
	float peak_data_rate,
	uint32_t minimum_policed_unit,
	uint32_t maximum_packet_size);
	
void* readRSVPCommonHeader(RSVPCommonHeader*, uint8_t& msg_type, uint8_t& send_TTL, uint16_t& length);
void readRSVPObjectHeader(RSVPObjectHeader*, uint8_t& class_num, uint8_t& c_type);
void* readRSVPSession(RSVPSession*, in_addr& destinationAddress, uint8_t& protocol_id, bool& police, uint16_t& dst_port);
void* readRSVPHop(RSVPHop*, in_addr& next_previous_hop_address, uint32_t& logical_interface_handle);
void* readRSVPTimeValues(RSVPTimeValues*, uint32_t& refresh_period_r);
void* readRSVPStyle(RSVPStyle*);
void* readRSVPErrorSpec(RSVPErrorSpec*, in_addr& error_node_address, bool& inPlace, bool& notGuilty, uint8_t& errorCode, uint16_t& errorValue);
void* readRSVPResvConf(RSVPResvConf*, in_addr& receiverAddress);
void* readRSVPScope(RSVPObjectHeader*, Vector<in_addr>& src_addresses);
void* readRSVPFlowspec(RSVPFlowspec*,
	float& token_bucket_rate,
	float& token_bucket_size,
	float& peak_data_rate,
	uint32_t& minimum_policed_unit,
	uint32_t& maximum_packet_size);
void* readRSVPFilterSpec(RSVPFilterSpec*, in_addr& src_address, uint16_t& src_port);
void* readRSVPSenderTemplate(RSVPSenderTemplate*, in_addr& src_address, uint16_t& src_port);
void* readRSVPSenderTSpec(RSVPSenderTSpec*,
	float& token_bucket_rate,
	float& token_bucket_size,
	float& peak_data_rate,
	uint32_t& minimum_policed_unit,
	uint32_t& maximum_packet_size);

class RSVPElement : public Element {
public:
	RSVPElement();
	~RSVPElement();

	const char *class_name() const	{ return "RSVPElement"; }
	const char *port_count() const	{ return "-1/1"; }
	const char *processing() const	{ return "h/h"; }
	int configure(Vector<String>&, ErrorHandler*);
	int initialize(ErrorHandler *);
	void run_timer(Timer *);

	void push(int, Packet *);
	Packet* pull(int);

	// specify object fields handlers
	static int sessionHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int hopHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int errorSpecHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int timeValuesHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvConfObjectHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int scopeHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int senderDescriptorHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh);

	// send message handlers
	static int pathHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int pathErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvErrHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int pathTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvTearHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvConfHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	
	static String getTTLHandle(Element *e, void * thunk);
	
	void add_handlers();
	
	WritablePacket* createPacket(uint16_t packetSize) const;

	Packet* createPathMessage() const;
	Packet* createResvMessage() const;
	Packet* createPathErrMessage() const;
	Packet* createResvErrMessage() const;
	Packet* createPathTearMessage() const;
	Packet* createResvTearMessage() const;
	Packet* createResvConfMessage() const;
	
private:
	void clean();

	Timer _timer;
	uint8_t _TTL;
	
	RSVPSession _session;
	RSVPErrorSpec _errorSpec;
	RSVPHop _hop;
	RSVPTimeValues _timeValues;
	
	bool _flowspec;
	bool _filterspec;
	bool _senderDescriptor;

	RSVPSenderTemplate _senderTemplate;
	RSVPSenderTSpec _senderTSpec;

	bool _resvConf_given;
	RSVPResvConf _resvConf;

	Vector<in_addr> _scope_src_addresses;
};

CLICK_ENDDECLS
#endif
