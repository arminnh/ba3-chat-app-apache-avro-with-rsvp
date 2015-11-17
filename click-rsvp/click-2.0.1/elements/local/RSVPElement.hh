#ifndef CLICK_RSVPElement_HH
#define CLICK_RSVPElement_HH
#include <click/element.hh>
#include <clicknet/ip.h>
CLICK_DECLS

#define RSVP_MSG_PATH		    1
#define RSVP_MSG_RESV		    2
#define RSVP_MSG_PATHERR		3
#define RSVP_MSG_RESVERR		4
#define RSVP_MSG_PATHTEAR		5
#define RSVP_MSG_RESVTEAR		6
#define RSVP_MSG_RESVCONF		7


#define RSVP_CLASS_NULL                 0
#define RSVP_CLASS_SESSION              1
#define RSVP_CLASS_RSVP_HOP             3
#define RSVP_CLASS_INTEGRITY            4
#define RSVP_CLASS_TIME_VALUES          5
#define RSVP_CLASS_ERROR_SPEC           6
#define RSVP_CLASS_STYLE                8
#define RSVP_CLASS_SCOPE                7
#define RSVP_CLASS_FLOWSPEC             9
#define RSVP_CLASS_FILTER_SPEC          10
#define RSVP_CLASS_SENDER_TEMPLATE      11
#define RSVP_CLASS_SENDER_TSPEC         12
//ADSPEC NIET
#define RSVP_CLASS_ADSPEC               13
#define RSVP_CLASS_POLICY_DATA          14
#define RSVP_CLASS_RESV_CONFIRM         15

struct RSVPCommonHeader {
	// RSVPCommonHeader() : vers(1), flags(0), RSVP_checksum(0), reserved(0) {}
	unsigned    vers : 4;
	unsigned    flags : 4;
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
	// RFC2210
};

struct RSVPFilterSpec { // class num = 10, C-type = 1
	RSVPObjectHeader header;
	in_addr src_address;
	uint16_t nothing;
	uint16_t src_port;
};

typedef RSVPFilterSpec RSVPSenderTemplate; // class num = 11, C-type = 1

// RSVPSenderTSpec class num = 12, C-type = 2
// RSVPPolicyData class num = 14, C-type = 1

struct RSVPResvConfirm { // class num = 15, C-type = 1
	in_addr receiver_address;
};

size_t sizeofRSVPObject(uint16_t class_num, uint16_t c_type);

void initRSVPCommonHeader(RSVPCommonHeader*, uint8_t msg_type, uint8_t send_TTL);
void initRSVPObjectHeader(RSVPObjectHeader*, uint8_t class_num, uint8_t c_type);
void initRSVPSession(RSVPSession*, in_addr destinationAddress, uint8_t protocol_id, bool police);
void initRSVPHop(RSVPHop*, in_addr next_previous_hop_address, uint32_t logical_interface_handle);
void initRSVPTimeValues(RSVPTimeValues*, uint32_t refresh_period_r);
void initRSVPStyle(RSVPStyle*);

class RSVPElement : public Element {
	public:
		RSVPElement();
		~RSVPElement();

		const char *class_name() const	{ return "RSVPElement"; }
		const char *port_count() const	{ return "1/1"; }
		const char *processing() const	{ return "h/h"; }
		int configure(Vector<String>&, ErrorHandler*);

		void push(int, Packet *);
		Packet* pull(int);

    static int handle(const String &conf, Element *e, void * thunk, ErrorHandler *errh);
    static String handle2(Element *e, void * thunk);
    void add_handlers();
	private:

};

CLICK_ENDDECLS
#endif
