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

class InviteCommand(private val claimManager: ClaimManager): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player || args.size == 3, "Command can only be issued by players", sender) && true
        assertTrue(
            (sender is Player && args.size in 1..2) || ((sender !is Player || sender.hasPermission("chunkprotector.bypass")) && args.size == 3),
            "Wrong number of arguments supplied!",
            sender
        ) && return true

        val targetPlayer = if(args.size == 3) OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[2]) else sender as Player
        assertTrue(targetPlayer != null, "Could not find given player", sender) && return true
        assertTrue(args[0] != targetPlayer!!.name, "The owner of a claim cannot be invited to said claim", sender) && return true

        val claim = if(args.size == 1) claimManager.getClaimAt((targetPlayer as Player).location.chunk) else claimManager.getClaimByName(targetPlayer, args[1])
        assertTrue(claim != null && claim.owner == targetPlayer.uniqueId, "Could not find a claim with that name", sender) && return true

        val guest = OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[0])
        assertTrue(guest != null, "Cannot find a player with that name", sender) && return true

        if (claim!!.addGuest(guest!!) && guest.isOnline)
            (guest as Player).spigot().sendMessage(successMessage("You have been invited to \"${claim.name}\" (owned by ${targetPlayer.name})"))
        sender.spigot().sendMessage(successMessage("${guest.name} has been invited to ${claim.name}"))

        return true
    }
}