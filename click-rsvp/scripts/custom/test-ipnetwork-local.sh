write host1/rsvp.session DEST 192.168.11.1, PROTOCOL 17, POLICE false, PORT 2222
write host1/rsvp.timevalues REFRESH 3
write host1/rsvp.senderdescriptor SRC_ADDRESS 192.168.10.1, SRC_PORT 7, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host1/rsvp.path TTL 246, REFRESH true

write host1/rsvp.session DEST 192.168.11.1, PROTOCOL 17, POLICE false, PORT 2222
write host1/rsvp.senderdescriptor SRC_ADDRESS 192.168.10.1, SRC_PORT 7, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host1/rsvp.pathtear

write host2/rsvp.session DEST 192.168.11.1, PROTOCOL 17, POLICE false, PORT 2222
write host2/rsvp.flowdescriptor SRC_ADDRESS 192.168.10.1, SRC_PORT 7, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host2/rsvp.resvtear

write host2/rsvp.session DEST 192.168.10.1, PROTOCOL 1, POLICE true, PORT 50
write host2/rsvp.timevalues REFRESH 6
write host2/rsvp.senderdescriptor SRC_ADDRESS 192.168.11.1, SRC_PORT 1001, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host2/rsvp.path TTL 246

write host2/rsvp.session DEST 192.168.11.1, PROTOCOL 1, POLICE true, PORT 50
write host2/rsvp.hop NEIGHBOR 192.168.12.254, LIH 0
write host2/rsvp.timevalues REFRESH 6
write host2/rsvp.flowdescriptor SRC_ADDRESS 5.8.9.254, SRC_PORT 1001, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write host2/rsvp.resv TTL 246

read host1/rsvp.pathstatetable
read host1/rsvp.resvstatetable
read host1/rsvp.senderstable
read host1/rsvp.reservationstable

read host2/rsvp.pathstatetable
read host2/rsvp.resvstatetable
read host2/rsvp.senderstable
read host2/rsvp.reservationstable
