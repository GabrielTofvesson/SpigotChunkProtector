package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.Claim
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.chunkprotector.kotlin.assertNotNull
import dev.w1zzrd.spigot.chunkprotector.listener.CompletionProcessor
import dev.w1zzrd.spigot.wizcompat.OfflinePlayers
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.successMessage
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private val OPTIONS = arrayOf(
    Claim::allowAllLiquids,
    Claim::allowEntityInteract,
    Claim::allowGuestLiquids,
    Claim::allowPlayerEntityInteract,
    Claim::allowTNT,
    Claim::disablePVP
)

class ClaimOptionCommand(private val claimManager: ClaimManager): TabCompletionCommandExecutor() {
    override val completionProcessor: CompletionProcessor
        get() = { sender, args ->
            if (args.isEmpty() || (args.size == 1 && args[0].isBlank()))
                if(sender is Player && !sender.hasPermission("chunkprotector.bypass")) claimManager.getClaimsForOwner(sender).map(Claim::name)
                else OfflinePlayers.getAllKnownPlayers(
                    Bukkit.getServer(),
                    true
                ).mapNotNull { Bukkit.getOfflinePlayer(it).name }
            else emptyList()
        }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player || args.size == 4, "Command can only be run by players", sender) && return true
        assertTrue(
            (sender is Player && (args.size in 2..3 || (sender.hasPermission("chunkprotector.bypass") && args.size in 2..4))) || (sender !is Player && args.size == 4),
            "Wrong number of arguments",
            sender
        ) && return true

        // It has been written. Let it nevermore be read
        (assertNotNull(OPTIONS.firstOrNull { it.name.equals(args[args.size - 2], ignoreCase = true) }, "Unknown option", sender) ?: return true).set(
            if(args.size == 2)
                assertNotNull(claimManager.getClaimAt((sender as Player).location.chunk), "This location is unclaimed", sender) ?: return true
            else
                assertNotNull(claimManager.getClaimByName(assertNotNull(
                    if(args.size == 3) sender as Player
                    else OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[0]), "Could not find a player with the given name", sender
                ) ?: return true, args[1]), "No claim with the given name could be found", sender) ?: return true,
            args[args.size - 1].lowercase().toBoolean()
        )

        sender.spigot().sendMessage(successMessage("Option has been updated!"))

        return true
    }
}