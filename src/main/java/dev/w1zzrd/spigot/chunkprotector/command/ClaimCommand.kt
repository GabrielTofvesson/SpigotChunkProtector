package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.chunkprotector.listener.TabCompleteListener
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClaimCommand(private val claimManager: ClaimManager): CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (assertTrue(sender is Player, "Only players can claim land", sender))
            return true

        val isClaiming = claimManager.isClaiming(sender as Player)

        if (assertTrue(isClaiming || args.size == 1, "Expected exactly one argument", sender) ||
                assertTrue(!isClaiming || args.isEmpty(), "You are already claiming a region!", sender) ||
                assertTrue(isClaiming || claimManager.getClaimByName(sender, args[0]) == null, "You already have a claim with that name", sender))
                    return true

        if (claimManager.toggleClaim(sender, if(isClaiming) "" else args[0]))
            sender.spigot().sendMessage(CommandUtils.successMessage("Enabled claim mode"))
        else
            sender.spigot().sendMessage(CommandUtils.successMessage("Cancelled claim!"))

        return true
    }
}