package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClaimOwnerCommand(private val claimManager: ClaimManager): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player, "Only players can check the owner of a chunk", sender) && return true
        assertTrue(args.isEmpty(), "No arguments expected for this command", sender) && return true
        sender as Player

        val claim = claimManager.getClaimAt(sender.location.chunk)
        assertTrue(claim != null, "No one owns this chunk", sender) && return true
        claim!!

        sender.spigot().sendMessage(successMessage("Chunk is owned by ${if(claim.owner == sender.uniqueId) "you" else Bukkit.getOfflinePlayer(claim.owner).name} (name: ${claim.name})"))
        return true
    }
}