package dev.w1zzrd.spigot.chunkprotector.claim

import dev.w1zzrd.spigot.chunkprotector.collection.BinaryCache
import dev.w1zzrd.spigot.chunkprotector.collection.BinaryList.Companion.newBinaryList
import dev.w1zzrd.spigot.chunkprotector.collection.SerializableBinaryList
import dev.w1zzrd.spigot.chunkprotector.freecam.FreeCamManager
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.errorMessage
import dev.w1zzrd.spigot.wizcompat.serialization.PersistentData
import org.bukkit.Chunk
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

private val Chunk.longChunkCoordinate: Long
    get() = (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)

private fun Chunk.compareTo(other: Chunk): Int {
    val comp1 = world.uid.compareTo(other.world.uid)
    return if (comp1 == 0) longChunkCoordinate.compareTo(other.longChunkCoordinate) else comp1
}

private data class ClaimNameCacheEntry(val player: OfflinePlayer, val name: String): Comparable<ClaimNameCacheEntry> {
    override fun compareTo(other: ClaimNameCacheEntry): Int {
        val comp1 = player.uniqueId.compareTo(player.uniqueId)
        return if (comp1 == 0) name.compareTo(other.name) else comp1
    }
}

class ClaimManager(private val freeCamManager: FreeCamManager, persistentData: PersistentData) {
    private val claimSelectListener = ClaimSelectListener()
    private val claimBuilders = newBinaryList<ClaimBuilder>()
    private val claims: SerializableBinaryList<Claim> = persistentData.loadData("claims") { SerializableBinaryList(newBinaryList()) }
    private val chunkPermCache = BinaryCache.makeCache(512, Chunk::compareTo) { chunk ->
        claims.list.find { it.world == chunk.world.uid && it.contains(chunk) }
    }
    private val namePermCache = BinaryCache.makeCache<ClaimNameCacheEntry, Claim>(64, ClaimNameCacheEntry::compareTo) {
        claims.list.getByName(it.player.uniqueId, it.name)
    }

    init {
        freeCamManager.addOnPlayerExitFreeCam { player ->
            val builderIndex = claimBuilders.binarySearch { it.who.uniqueId.compareTo(player.uniqueId) }
            if (builderIndex >= 0) {
                val builder =  claimBuilders.removeAt(builderIndex)
                builder.clearRenders()
            }
        }
    }

    fun onEnable(plugin: Plugin) = plugin.server.pluginManager.registerEvents(claimSelectListener, plugin)
    fun onDisable() = HandlerList.unregisterAll(claimSelectListener)

    fun toggleClaim(player: Player, name: String) =
        if (isClaiming(player)) {
            freeCamManager.disableFreeCam(player)
            false
        } else {
            freeCamManager.enableFreeCam(player)
            claimBuilders.add(ClaimBuilder(player, player.world, name))
            true
        }

    fun isClaiming(player: Player) = claimBuilders.contains(player.uniqueId) { who.uniqueId }

    fun addClaim(claim: Claim): Boolean {
        if (claims.list.any { it.overlaps(claim) })
            return false

        claims.list.add(claim)
        return true
    }

    fun removeClaim(claim: Claim): Boolean {
        chunkPermCache.clearValues(claim)
        namePermCache.clearValues(claim)
        return claims.list.remove(claim)
    }

    fun invitePlayer(owner: Player, name: String, invitedPlayer: Player) =
        claims.list.getByName(owner.uniqueId, name)?.addGuest(invitedPlayer) ?: false

    fun unInvitePlayer(owner: Player, name: String, unInvitedPlayer: Player) =
        claims.list.getByName(owner.uniqueId, name)?.removeGuest(unInvitedPlayer) ?: false

    // This is slow, because a sequential search must be done
    fun getClaimAt(chunk: Chunk) = chunkPermCache[chunk]
    fun getClaimByName(owner: OfflinePlayer, name: String) = namePermCache[ClaimNameCacheEntry(owner, name)]
    fun getClaimsForOwner(owner: OfflinePlayer) = claims.list.getAllForOwner(owner.uniqueId)

    private inner class ClaimSelectListener: Listener {
        @EventHandler
        fun onFreeCamHit(event: PlayerInteractEvent) {
            if ((event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) && isClaiming(event.player)) {
                val claimBuilder = claimBuilders.find { it.who == event.player }!!

                claimBuilder.toggleCorner(event.player.location.chunk)

                if (claimBuilder.isValidRegion) {
                    event.player.spigot()
                        .sendMessage(CommandUtils.successMessage("Region selected! Right-click a block to confirm selections"))
                }
            } else if (event.action == Action.RIGHT_CLICK_BLOCK && isClaiming(event.player)) {
                val claimBuilder = claimBuilders.find { it.who == event.player }!!

                if (assertTrue(claimBuilder.isValidRegion, "You have not selected a region", event.player) ||
                    assertTrue(addClaim(claimBuilder.built), "This claim overlaps with another claim", event.player))
                    return

                freeCamManager.disableFreeCam(event.player)

                event.player.spigot().sendMessage(CommandUtils.successMessage("Region claimed!"))
            }
        }

        @EventHandler
        fun onPlayerWorldChange(event: PlayerChangedWorldEvent) {
            if (isClaiming(event.player)) {
                freeCamManager.disableFreeCam(event.player)
                event.player.spigot().sendMessage(errorMessage("Cancelled claiming!"))
            }
        }
    }
}