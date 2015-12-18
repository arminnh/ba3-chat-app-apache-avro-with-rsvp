write host1/rsvp.name host1
write host2/rsvp.name host2
write host1/rsvpipencap.dst 192.168.11.1
write host2/rsvpipencap.dst 192.168.10.1
write host1/rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write host1/rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write host1/rsvp.timevalues REFRESH 6
write host1/rsvp.senderdescriptor SRC_ADDRESS 5.8.9.254, SRC_PORT 1001, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host1/rsvp.path TTL 246

