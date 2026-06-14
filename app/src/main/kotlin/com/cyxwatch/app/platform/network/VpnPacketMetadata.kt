package com.cyxwatch.app.platform.network

data class VpnPacketMetadata(
    val destinationIp: String,
    val destinationPort: Int?,
    val protocol: String,
    val packetBytes: Long,
)
