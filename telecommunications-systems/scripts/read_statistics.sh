#!/bin/bash

#########################################################################################
# This script:										#
# 1. Connects to the local instance of the Click router running on the defined port.	#
# 2. Resets all counters								#
# 3. Waits for several seconds to let the counters reach sensible average values again. #
# 4. Reads out all counters and prints them.						#
#########################################################################################

PORT=10000

echo "Connecting to localhost on port $PORT:"
echo "> Resetting counters (This takes a few seconds...)"

(echo "write host1_tx_be_ctr.reset"; sleep 0.5; echo "write host1_tx_qos_ctr.reset"; sleep 0.5; echo "write host1_rx_be_ctr.reset"; sleep 0.5; echo "write host1_rx_qos_ctr.reset"; sleep 0.5) | telnet localhost $PORT &>/dev/null
(echo "write router2_tx_be_ctr.reset"; sleep 0.5; echo "write router2_tx_qos_ctr.reset"; sleep 0.5; echo "write router2_rx_be_ctr.reset"; sleep 0.5; echo "write router2_rx_qos_ctr.reset"; sleep 0.5) | telnet localhost $PORT &>/dev/null

echo "> Gathering statistics (This takes a few seconds...)"
sleep 2

echo "> Host 1"

TXBE=`(echo "read host1_tx_be_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`	
RXBE=`(echo "read host1_rx_be_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`
TXQoS=`(echo "read host1_tx_qos_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`
RXQoS=`(echo "read host1_rx_qos_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`	
	
if [ -z "$TXBE" ]
then
	TXBE=0
fi

if [ -z "$RXBE" ]
then
	RXBE=0
fi

if [ -z "$TXQoS" ]
then
	TXQoS=0
fi

if [ -z "$RXQoS" ]
then
	RXQoS=0
fi

TXBE=`echo "scale=2; $TXBE/125" | bc`
RXBE=`echo "scale=2; $RXBE/125" | bc`
TXQoS=`echo "scale=2; $TXQoS/125" | bc`
RXQoS=`echo "scale=2; $RXQoS/125" | bc`

echo -e "\tSent\tReceived"
echo -e "BE\t$TXBE\t$RXBE"
echo -e "QoS\t$TXQoS\t$RXQoS\n"

echo "> Router 2"

TXBE=`(echo "read router2_tx_be_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`           
RXBE=`(echo "read router2_rx_be_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`
TXQoS=`(echo "read router2_tx_qos_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`
RXQoS=`(echo "read router2_rx_qos_ctr.byte_rate"; sleep 0.5) | telnet localhost $PORT  2>/dev/null | grep -E "^[0-9]+\.[0-9]+" | cut -f 1 -d " "`         

if [ -z "$TXBE" ]
then
        TXBE=0
fi

if [ -z "$RXBE" ]
then
        RXBE=0
fi

if [ -z "$TXQoS" ]
then
        TXQoS=0
fi

if [ -z "$RXQoS" ]
then
        RXQoS=0
fi

TXBE=`echo "scale=2; $TXBE/125" | bc`
RXBE=`echo "scale=2; $RXBE/125" | bc`
TXQoS=`echo "scale=2; $TXQoS/125" | bc`
RXQoS=`echo "scale=2; $RXQoS/125" | bc`

echo -e "\tSent\tReceived"
echo -e "BE\t$TXBE\t$RXBE"
echo -e "QoS\t$TXQoS\t$RXQoS\n"

exit
