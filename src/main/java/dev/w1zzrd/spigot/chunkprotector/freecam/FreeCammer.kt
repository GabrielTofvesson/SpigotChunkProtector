package dev.w1zzrd.spigot.chunkprotector.freecam

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent

data class FreeCammer(val player: Player, val startLocation: Location? = null, val originalMode: GameMode? = null): Comparable<FreeCammer> {

    fun enable() {
        player.gameMode = GameMode.SPECTATOR
    }

    fun disable() {
        player.teleport(startLocation!!, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.gameMode = originalMode!!
    }

    override fun compareTo(other: FreeCammer) = player.uniqueId.compareTo(other.player.uniqueId)
}