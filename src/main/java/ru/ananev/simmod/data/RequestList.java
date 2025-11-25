package ru.ananev.simmod.data;

import java.util.Random;

public class RequestList {
    private static final Random random = new Random();

    // Типы сетевых пакетов
    public static final String[] PACKET_TYPES = {
            "TCP SYN",
            "TCP ACK",
            "TCP FIN",
            "UDP Datagram",
            "ICMP Echo Request",
            "ICMP Echo Reply",
            "HTTP GET",
            "HTTP POST",
            "DNS Query",
            "DNS Response",
            "ARP Request",
            "ARP Reply",
            "SSH Handshake",
            "TLS Handshake",
            "BGP Update",
            "OSPF Hello",
            "RIP Update",
            "SNMP Get",
            "NTP Sync",
            "DHCP Discover",
            "DHCP Offer",
            "FTP Control",
            "SMTP Command",
            "POP3 Request",
            "IMAP Command",
            "VoIP RTP",
            "Video Stream",
            "Audio Stream",
            "VPN Tunnel",
            "QoS Marked"
    };

    // Размеры пакетов (в байтах)
    public static final int[] PACKET_SIZES = {
            64,    // Minimum Ethernet
            128,   // Small packet
            256,   // Medium packet
            512,   // Large packet
            1024,  // Jumbo frame start
            1500,  // Standard MTU
            2048,  // Jumbo frame
            4096,  // Large jumbo
            8192   // Maximum typical
    };

    public static String getRandomPacketType() {
        return PACKET_TYPES[random.nextInt(PACKET_TYPES.length)];
    }

    public static int getRandomPacketSize() {
        return PACKET_SIZES[random.nextInt(PACKET_SIZES.length)];
    }

    public static String generatePacketInfo() {
        String type = getRandomPacketType();
        int size = getRandomPacketSize();
        int ttl = 32 + random.nextInt(224); // TTL от 32 до 255
        return String.format("%s (%d байт, TTL=%d)", type, size, ttl);
    }
}