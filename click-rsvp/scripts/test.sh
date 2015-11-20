#path messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.timevalues REFRESH 6
write 1.path TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 27.36.40.5, LIH 54
write 1.timevalues REFRESH 6896
write 1.path TTL 123

#resv messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.scope SRC_ADDRESS 5.6.7.8
write 1.scope SRC_ADDRESS 9.10.11.12
write 1.scope SRC_ADDRESS 13.14.15.16
write 1.timevalues REFRESH 6
write 1.resv TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 21.13.14.51, LIH 51
write 1.timevalues REFRESH 6896
write 1.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write 1.resv TTL 123

write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.scope SRC_ADDRESS 5.6.7.8
write 1.scope SRC_ADDRESS 13.14.15.16
write 1.timevalues REFRESH 6
write 1.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write 1.resv TTL 246

#path error messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write 1.patherr TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write 1.patherr TTL 123

#resv error messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write 1.resverr TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 27.36.40.5, LIH 54
write 1.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write 1.resverr TTL 123

write 1.scope SRC_ADDRESS 5.6.7.8
write 1.scope SRC_ADDRESS 13.14.15.16
write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 27.36.40.5, LIH 54
write 1.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write 1.resverr TTL 123

#path tear messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50 
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.pathtear TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 27.36.40.5, LIH 54
write 1.pathtear TTL 123

#resv tear messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50
write 1.hop NEIGHBOR 2.3.4.5, LIH 5
write 1.resvtear TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.hop NEIGHBOR 27.36.40.5, LIH 54
write 1.resvtear TTL 123

#resv conf messages
write 1.session DEST 1.2.3.4, PROTOCOL 1, POLICE true, PORT 50 
write 1.errorspec ERROR_NODE_ADDRESS 7.8.9.10, INPLACE true, NOTGUILTY true, ERROR_CODE 78, ERROR_VALUE 456
write 1.resvconfobj RECEIVER_ADDRESS 20.21.23.24
write 1.resvconf TTL 246

write 1.session DEST 5.6.4.8, PROTOCOL 10, POLICE false, PORT 500
write 1.errorspec ERROR_NODE_ADDRESS 11.12.13.14, INPLACE false, NOTGUILTY true, ERROR_CODE 123, ERROR_VALUE 789
write 1.resvconfobj RECEIVER_ADDRESS 31.32.33.34
write 1.resvconf TTL 123


