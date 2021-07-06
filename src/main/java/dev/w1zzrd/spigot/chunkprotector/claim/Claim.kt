package dev.w1zzrd.spigot.chunkprotector.claim

import dev.w1zzrd.spigot.chunkprotector.collection.BinaryList
import dev.w1zzrd.spigot.wizcompat.packet.EntityCreator.*
import dev.w1zzrd.spigot.wizcompat.serialization.SimpleReflectiveConfigItem
import dev.w1zzrd.spigot.wizcompat.serialization.UUIDList
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

class ClaimBuilder(val who: Player, val world: World, val name: String): Comparable<ClaimBuilder> {
    private val first = CornerSelection()
    private val second = CornerSelection()

    val isValidRegion: Boolean
        get() = second.corner != null

    val built: Claim
        get() = Claim(who.uniqueId, world.uid, name, ClaimChunk(first.corner!!.x, first.corner!!.z), ClaimChunk(second.corner!!.x, second.corner!!.z))

    fun toggleCorner(corner: Chunk) {
        when (corner) {
            first.corner -> first.corner = null
            second.corner -> second.corner = null
            else -> {
                if ((second.corner != null && first.corner != null) || first.corner == null) {
                    second.corner = null
                    first.corner = corner
                } else {
                    second.corner = corner
                }
            }
        }

        first.render()
        second.render()
    }

    fun clearRenders() {
        first.clearRender()
        second.clearRender()
    }

    override fun compareTo(other: ClaimBuilder) = who.uniqueId.compareTo(other.who.uniqueId)
    override fun equals(other: Any?) = other is ClaimBuilder && hashCode() == other.hashCode()
    override fun hashCode() = who.hashCode()

    private inner class CornerSelection {
        private var render = -1
        private var location: Location? = null

        var isDirty: Boolean = true
            private set
        var corner: Chunk? = null
            set(value) {
                isDirty = isDirty || (field != value)
                field = value

                adjustHeight()
            }

        fun adjustHeight() {
            val currentCorner = corner

            if (currentCorner != null) {
                location = Location(
                    who.world,
                    (currentCorner.x shl 4) + 8.5,
                    kotlin.math.max(who.location.y - 3.0, 0.0),
                    (currentCorner.z shl 4) + 8.5,
                    0f,
                    0f
                )
            }
        }

        fun render() {
            if (isDirty) {
                clearRender()

                if (corner != null) {
                    val renderShulker = createFakeShulker(who)
                    setEntityInvisible(renderShulker, true)
                    setEntityInvulnerable(renderShulker, true)
                    setEntityLocation(
                        renderShulker,
                        location!!.x,
                        location!!.y,
                        location!!.z,
                        location!!.yaw,
                        location!!.pitch
                    )
                    setEntityCollision(renderShulker, false)
                    setEntityGlowing(renderShulker, true)

                    sendEntitySpawnPacket(who, renderShulker)
                    sendEntityMetadataPacket(who, renderShulker)

                    render = getEntityID(renderShulker)
                }

                isDirty = false
            }
        }

        fun clearRender() {
            if (render != -1) {
                sendEntityDespawnPacket(who, render)
                render = -1
            }
        }
    }
}

class Claim: SimpleReflectiveConfigItem, Comparable<Claim> {
    @Transient
    var owner: UUID
        private set
    private lateinit var ownerString: String

    @Transient
    var world: UUID
        private set
    private lateinit var worldString: String

    lateinit var name: String
        private set

    lateinit var topLeft: ClaimChunk
        private set

    lateinit var bottomRight: ClaimChunk
        private set

    private lateinit var guests: UUIDList


    // Per-claim settings
    var allowPlayerEntityInteract = false
    var allowTNT = false
    var allowEntityInteract = false
    var allowAllLiquids = false
    var allowGuestLiquids = true
    var disablePVP = false

    constructor(map: Map<String, Any?>): super(map) {
        owner = UUID.fromString(ownerString)
        world = UUID.fromString(worldString)
    }
    constructor(owner: UUID, world: UUID, name: String, corner1: ClaimChunk, corner2: ClaimChunk): super(Collections.emptyMap()) {
        this.owner = owner
        ownerString = owner.toString()
        this.world = world
        worldString = world.toString()
        this.name = name
        this.topLeft = ClaimChunk(min(corner1.chunkX, corner2.chunkX), max(corner1.chunkZ, corner2.chunkZ))
        this.bottomRight = ClaimChunk(max(corner1.chunkX, corner2.chunkX), min(corner1.chunkZ, corner2.chunkZ))
        guests = UUIDList(BinaryList.newBinaryList())
    }

    fun addGuest(player: OfflinePlayer) = guests.uuids.add(player.uniqueId)
    fun removeGuest(player: OfflinePlayer) = guests.uuids.remove(player.uniqueId)

    fun hasGuest(player: OfflinePlayer) = guests.uuids.contains(player.uniqueId)

    fun isAccessible(player: OfflinePlayer) = owner == player.uniqueId || guests.uuids.contains(player.uniqueId)

    fun overlaps(other: Claim) =
        !(topLeft.chunkX > other.bottomRight.chunkX || other.topLeft.chunkX > bottomRight.chunkZ) &&
                !(bottomRight.chunkZ > other.topLeft.chunkZ || other.bottomRight.chunkZ > topLeft.chunkZ)

    fun contains(chunk: Chunk) = contains(chunk.x, chunk.z)
    fun contains(chunkX: Int, chunkZ: Int) =
        !(topLeft.chunkX > chunkX || chunkX > bottomRight.chunkX) &&
                !(bottomRight.chunkZ > chunkZ || chunkZ > topLeft.chunkZ)

    override fun compareTo(other: Claim) = compareRaw(other.owner, other.name)

    fun compareRaw(owner: UUID, name: String): Int {
        val comp1 = this.owner.compareTo(owner)
        return if (comp1 == 0) this.name.compareTo(name) else comp1
    }

    fun compareFindFirst(owner: UUID): Int {
        val comp1 = this.owner.compareTo(owner)
        return if (comp1 == 0) 1 else comp1
    }

    fun compareFindLast(owner: UUID): Int {
        val comp1 = this.owner.compareTo(owner)
        return if (comp1 == 0) -1 else comp1
    }
}

fun BinaryList<Claim>.getByName(owner: UUID, name: String): Claim? {
    val index = binarySearch { it.compareRaw(owner, name) }

    if (index >= 0)
        return this[index]

    return null
}

fun BinaryList<Claim>.getAllForOwner(owner: UUID): List<Claim> {
    val startIndex = -(binarySearch { it.compareFindFirst(owner) } + 1)
    val endIndex = -(binarySearch { it.compareFindLast(owner) } + 1)

    if (startIndex >= endIndex)
        return emptyList()

    return subList(startIndex, endIndex)
}