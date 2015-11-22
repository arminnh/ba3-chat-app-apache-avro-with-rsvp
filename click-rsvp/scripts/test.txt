#path messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.timevalues REFRESH 6
write rsvp.senderdescriptor SRC_ADDRESS 5.8.9.254, SRC_PORT 1001, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5
write rsvp.path TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.timevalues REFRESH 6896
write rsvp.path TTL 123

#resv messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 9.10.11.12
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.timevalues REFRESH 6
write rsvp.resv TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 21.13.14.51, LIH 51
write rsvp.timevalues REFRESH 6896
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resv TTL 123

write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.timevalues REFRESH 6
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resv TTL 246

#path error messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.patherr TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.patherr TTL 123

#resv error messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.resverr TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resverr TTL 123

write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resverr TTL 123

#path tear messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.pathtear TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.pathtear TTL 123

#resv tear messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.resvtear TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.resvtear TTL 123

#resv conf messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resvconf TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resvconfobj RECEIVER_ADDRESS 31.32.33.34
write rsvp.resvconf TTL 123

#set TOS byte 0
write 2.tos 0

#path messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.timevalues REFRESH 6
write rsvp.path TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.timevalues REFRESH 6896
write rsvp.path TTL 123

#resv messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 9.10.11.12
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.timevalues REFRESH 6
write rsvp.resv TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 21.13.14.51, LIH 51
write rsvp.timevalues REFRESH 6896
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resv TTL 123

write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.timevalues REFRESH 6
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resv TTL 246

#path error messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.patherr TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.patherr TTL 123

#resv error messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.resverr TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resverr TTL 123

write rsvp.scope SRC_ADDRESS 5.6.7.8
write rsvp.scope SRC_ADDRESS 13.14.15.16
write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resverr TTL 123

#path tear messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.pathtear TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.pathtear TTL 123

#resv tear messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.hop NEIGHBOR 2.3.4.5, LIH 5
write rsvp.resvtear TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.hop NEIGHBOR 27.36.40.5, LIH 54
write rsvp.resvtear TTL 123

#resv conf messages
write rsvp.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write rsvp.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write rsvp.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write rsvp.resvconf TTL 246

write rsvp.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write rsvp.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write rsvp.resvconfobj RECEIVER_ADDRESS 31.32.33.34
write rsvp.resvconf TTL 123
