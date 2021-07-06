package dev.w1zzrd.spigot.chunkprotector.freecam

import dev.w1zzrd.spigot.wizcompat.packet.Players
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin

class FreeCamManager {
    private val freeCammers = ArrayList<FreeCammer>()
    private val freeCamEventListener = FreeCamEventListener()
    private val freeCamEnableListeners = ArrayList<(Player) -> Unit>()
    private val freeCamDisableListeners = ArrayList<(Player) -> Unit>()

    fun disableFreeCam(player: Player): Boolean {
        val index = freeCammers.binarySearch(FreeCammer(player))

        if (index < 0)
            return false

        // Disable before removal to prevent TOCTOU (in case I decide to multi-thread this)
        freeCammers[index].disable()
        freeCammers.removeAt(index)

        return true
    }

    fun enableFreeCam(player: Player): Boolean {
        val freeCammer = FreeCammer(player, player.location.clone(), player.gameMode)

        val index = freeCammers.binarySearch(freeCammer)

        if (index >= 0)
            return false

        // Enable after insertion to prevent TOCTOU (in case I decide to multi-thread this)
        freeCammers.add(-(index + 1), freeCammer)
        freeCammer.enable()

        return true
    }

    fun toggleFreeCam(player: Player): Boolean {
        val freeCammer = FreeCammer(player, player.location.clone(), player.gameMode)

        val index = freeCammers.binarySearch(freeCammer)

        return if (index >= 0) {
            freeCammers[index].disable()
            freeCammers.removeAt(index)
            false
        } else {
            freeCammers.add(-(index + 1), freeCammer)
            freeCammer.enable()
            true
        }
    }

    fun isFreeCamming(player: Player) = freeCammers.binarySearch(FreeCammer(player)) >= 0

    fun addOnPlayerEnterFreeCam(callback: (Player) -> Unit) = freeCamEnableListeners.add(callback)
    fun addOnPlayerExitFreeCam(callback: (Player) -> Unit) = freeCamDisableListeners.add(callback)

    fun onEnable(plugin: Plugin) = plugin.server.pluginManager.registerEvents(freeCamEventListener, plugin)
    fun onDisable() {
        HandlerList.unregisterAll(freeCamEventListener)

        freeCammers.forEach(FreeCammer::disable)
        freeCammers.clear()
    }

    private inner class FreeCamEventListener: Listener {
        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) = disableFreeCam(event.player)
    }

    inner class FreeCammer(val player: Player, val startLocation: Location? = null, val originalMode: GameMode? = null): Comparable<FreeCammer> {

        fun enable() {
            player.gameMode = GameMode.SPECTATOR
            Players.sendPlayerGameModePacket(player, GameMode.CREATIVE)

            freeCamEnableListeners.forEach { it(player) }
        }

        fun disable() {
            player.teleport(startLocation!!, PlayerTeleportEvent.TeleportCause.PLUGIN)
            player.gameMode = originalMode!!
            Players.sendPlayerGameModePacket(player, originalMode)

            freeCamDisableListeners.forEach { it(player) }
        }

        override fun compareTo(other: FreeCammer) = player.uniqueId.compareTo(other.player.uniqueId)
    }
}