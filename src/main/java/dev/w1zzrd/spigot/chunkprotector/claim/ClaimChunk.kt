package dev.w1zzrd.spigot.chunkprotector.claim

import dev.w1zzrd.spigot.wizcompat.serialization.SimpleReflectiveConfigItem
import java.util.*

class ClaimChunk: SimpleReflectiveConfigItem, Comparable<ClaimChunk> {
    var chunkX: Int = 0
        private set

    var chunkZ: Int = 0
        private set

    constructor(map: Map<String, Any?>): super(map)

    constructor(chunkX: Int, chunkZ: Int): super(Collections.emptyMap()) {
        this.chunkX = chunkX
        this.chunkZ = chunkZ
    }

    private val longCoordinate: Long
        get() = chunkX.toLong().shl(32) or chunkZ.toLong().and(0xFFFFFFFFL)

    override fun compareTo(other: ClaimChunk) =
        longCoordinate.compareTo(other.longCoordinate)

    override fun equals(other: Any?) = other is ClaimChunk && compareTo(other) == 0
}