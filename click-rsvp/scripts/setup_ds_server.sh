#!/bin/bash

sudo ip link set dev eth0 up
sudo tunctl -u student
sudo ip link set dev tap0 up
sudo ip addr add 192.168.11.1/24 brd + dev tap0
sudo ip route add 192.168.10.0/24 via 192.168.11.254 dev tap0
