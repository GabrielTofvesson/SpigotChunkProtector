package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.claim.ClaimChunk
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.wizcompat.OfflinePlayers
import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

private val inaccessible = Particle.DustOptions(Color.fromRGB(255, 0, 0), 10.0F)
private val accessible = Particle.DustOptions(Color.fromRGB(0, 255, 0), 10.0F)

class ShowClaimCommand(private val claimManager: ClaimManager, private val plugin: Plugin): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        assertTrue(sender is Player, "Claims can only be visualised by players", sender) && return true
        assertTrue(args.size <= 2, "Too many arguments supplied", sender) && return true
        sender as Player

        val targetPlayer = if(args.size == 2) OfflinePlayers.getKnownPlayer(Bukkit.getServer(), args[1]) else sender
        assertTrue(targetPlayer != null, "Specified player could not be found", sender) && return true
        targetPlayer!!

        val targetRegion = if(args.isEmpty()) claimManager.getClaimAt(sender.location.chunk) else claimManager.getClaimByName(targetPlayer, args[0])
        assertTrue(targetRegion != null, "Region is either not claimed, or a claim with the given name does not exist", sender) && return true
        targetRegion!!

        fun ClaimChunk.asCoords() = (chunkX shl 4).toDouble() to (chunkZ shl 4).toDouble()
        fun Pair<Double, Double>.lerp(other: Pair<Double, Double>) = (first + other.first).div(2.0) to (second + other.second).div(2.0)

        val baseVerts = arrayOf(
            ClaimChunk(targetRegion.topLeft.chunkX, targetRegion.topLeft.chunkZ + 1).asCoords(),
            ClaimChunk(targetRegion.topLeft.chunkX, targetRegion.bottomRight.chunkZ).asCoords(),
            ClaimChunk(targetRegion.bottomRight.chunkX + 1, targetRegion.bottomRight.chunkZ).asCoords(),
            ClaimChunk(targetRegion.bottomRight.chunkX + 1, targetRegion.topLeft.chunkZ + 1).asCoords()
        )

        val allVerts = arrayOf(
            *baseVerts,
            baseVerts[0].lerp(baseVerts[1]),
            baseVerts[1].lerp(baseVerts[2]),
            baseVerts[2].lerp(baseVerts[3]),
            baseVerts[3].lerp(baseVerts[0])
        )
        var count = 0
        var task: BukkitTask? = null

        val particleData = if (targetRegion.isAccessible(sender)) accessible else inaccessible

        task = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                if (count == 40)
                    task!!.cancel()

                count += 1
                for (vert in allVerts)
                    sender.spawnParticle(
                        Particle.REDSTONE,
                        vert.first,
                        sender.location.y + 1.0,
                        vert.second,
                        10,
                        particleData
                    )
            },
            0,
            10
        )

        return true
    }
}