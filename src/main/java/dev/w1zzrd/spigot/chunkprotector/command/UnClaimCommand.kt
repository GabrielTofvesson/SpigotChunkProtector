package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.wizcompat.OfflinePlayers
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.successMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class UnClaimCommand(private val claimManager: ClaimManager): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player || args.size == 2, "Command can only be run by players", sender) && return true
        assertTrue((sender is Player && args.size in 0..1) || ((sender !is Player || sender.hasPermission("chunkprotector.bypass")) && args.size == 2), "Wrong number of arguments", sender) && return true

        val targetPlayer = if (args.size == 2) OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[1]) else sender as Player
        assertTrue(targetPlayer != null, "Could not find target player", sender) && return true

        val claim = if(args.isEmpty()) claimManager.getClaimAt((targetPlayer!! as Player).location.chunk) else claimManager.getClaimByName(targetPlayer!!, args[0])
        assertTrue(claim != null, "Could not find a claim with that name", sender) && return true

        claimManager.removeClaim(claim!!)
        sender.spigot().sendMessage(successMessage("Unclaimed ${args[0]}"))

        return true
    }
}