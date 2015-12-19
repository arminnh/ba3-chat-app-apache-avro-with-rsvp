#include "rsvp.hh"

CLICK_DECLS

size_t sizeofRSVPClass(uint16_t class_num, uint16_t c_type)
{
	size_t size;

	switch (class_num) {
		case 1:
			size = sizeof(RSVPSessionClass);
			break;
		case 3:
			size = sizeof(RSVPHopClass);
			break;
		case 4:
			size = sizeof(RSVPIntegrityClass);
			break;
		case 5:
			size = sizeof(RSVPTimeValuesClass);
			break;
		case 6:
			size = sizeof(RSVPErrorSpecClass);
			break;
		case 8:
			size = sizeof(RSVPStyleClass);
			break;
		case 9:
			size = sizeof(RSVPFlowspecClass);
			break;
		case 10:
			size = sizeof(RSVPFilterSpecClass);
			break;
		case 11:
			size = sizeof(RSVPSenderTemplateClass);
			break;
		default:
			throw Exception("sizeofRSVPClass: requesting size of undefined class num");

		size += sizeof(RSVPObjectHeader);

		return size;
	}
}

void initRSVPObjectHeader(RSVPObjectHeader* header, uint8_t class_num, uint8_t c_type)
{
	uint16_t length = 0;
}

CLICK_ENDDECLS
