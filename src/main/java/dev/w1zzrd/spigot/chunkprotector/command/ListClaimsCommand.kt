package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.Claim
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.wizcompat.OfflinePlayers
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.successMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ListClaimsCommand(private val claimManager: ClaimManager): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player || args.size == 1, "Command can only be issued by players", sender) && return true
        assertTrue(
            ((sender !is Player || sender.hasPermission("chunkprotector.bypass")) && args.size == 1) || (sender is Player && args.isEmpty()),
            "This command expects no arguments",
            sender
        ) && return true

        val targetPlayer = if(args.isNotEmpty()) OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[0]) else sender as Player
        assertTrue(targetPlayer != null, "Could not find given player", sender) && return true

        val owned = claimManager.getClaimsForOwner(targetPlayer!!)
        if (owned.isEmpty())
            sender.spigot().sendMessage(successMessage("${if(targetPlayer != sender) "${targetPlayer.name} has" else "You have" } no claims"))
        else
            sender.spigot().sendMessage(successMessage("Claims: ${owned.map(Claim::name).joinToString(", ")}"))

        return true
    }
}