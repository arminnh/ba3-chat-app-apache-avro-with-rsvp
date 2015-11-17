#ifndef CLICK_RSVPElement_HH
#define CLICK_RSVPElement_HH
#include <click/element.hh>
CLICK_DECLS

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
