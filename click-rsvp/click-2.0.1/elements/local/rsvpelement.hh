#ifndef CLICK_RSVPELEMENT_HH
#define CLICK_RSVPELEMENT_HH
#include <click/element.hh>
#include <clicknet/ip.h>
#include <clicknet/ether.h>
#include <click/timer.hh>
#include "rsvpnode.hh"
CLICK_DECLS



uint16_t sizeofRSVPObject(uint8_t class_num, uint8_t c_type);
uint16_t sizeofRSVPScopeObject(size_t num_addresses);
const RSVPObjectHeader* nextRSVPObject(const RSVPObjectHeader*);
const void* RSVPObjectOfType(Packet*, uint8_t class_num);

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

class RSVPElement : public RSVPNode {
public:
	RSVPElement();
	~RSVPElement();

	const char *class_name() const	{ return "RSVPElement"; }
	const char *port_count() const	{ return "-1/1"; }
	const char *processing() const	{ return "h/h"; }
	int configure(Vector<String>&, ErrorHandler*);
	int initialize(ErrorHandler *);
	void run_timer(Timer *);

	virtual void push(int, Packet *);
	Packet* pull(int);

	// specify object fields handlers
	static int sessionHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int hopHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int errorSpecHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int timeValuesHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int resvConfObjectHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int scopeHandle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
	static int senderDescriptorHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh);

	static int nameHandle(const String &conf, Element *e, void *thunk, ErrorHandler *errh);

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
	String _name;
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
