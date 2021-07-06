package dev.w1zzrd.spigot.chunkprotector.listener

import dev.w1zzrd.spigot.chunkprotector.claim.Claim
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.PlayerLeashEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerShearEntityEvent

@Suppress("unused")
class PlayerActionListener(private val claimManager: ClaimManager): Listener {
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) =
        doConditionalCancellation(event.player, event)

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) =
        doConditionalCancellation(event.player, event, event.block.location)

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) =
        doConditionalCancellation(event.player, event, event.blockPlaced.location)

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.isCancelled)
            return

        filterProtectedBlocks(event.blockList(), Claim::allowEntityInteract)
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (event.isCancelled)
            return

        filterProtectedBlocks(event.blockList(), Claim::allowTNT)
    }

    @EventHandler
    fun onLiquidMove(event: BlockFromToEvent) {
        if (event.isCancelled)
            return

        // Cancel the event if a liquid is moving into a claimed area from an area that is not owned by the claimer
        val toClaim = claimManager.getClaimAt(event.toBlock.chunk) ?: return
        val fromClaim = claimManager.getClaimAt(event.toBlock.location.subtract(event.face.direction).chunk)
        if (fromClaim == null) {
            event.isCancelled = true
            return
        }

        if (toClaim.allowAllLiquids || toClaim.owner == fromClaim.owner || (toClaim.allowGuestLiquids && toClaim.hasGuest(Bukkit.getPlayer(fromClaim.owner)!!)))
            return

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (!event.isCancelled && checkEntityInteraction(event.player, event.rightClicked, Claim::allowPlayerEntityInteract))
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        if (!event.isCancelled && checkEntityInteraction(event.player, event.entity, Claim::allowPlayerEntityInteract))
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerLeashEntity(event: PlayerLeashEntityEvent) {
        if (!event.isCancelled && checkEntityInteraction(event.player, event.entity, Claim::allowPlayerEntityInteract))
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerAttractAnimal(event: EntityTargetEvent) {
        if (!event.isCancelled && event.target is Player && event.reason == EntityTargetEvent.TargetReason.TEMPT && checkEntityInteraction(event.target as Player, event.entity, Claim::allowPlayerEntityInteract))
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerAttackMob(event: EntityDamageByEntityEvent) {
        if (!event.isCancelled && checkEntityInteraction(
                event.damager,
                event.entity,
                if(event.damager is Player) Claim::allowPlayerEntityInteract
                else Claim::allowEntityInteract
            ))
            event.isCancelled = true
    }

    private fun checkEntityInteraction(source: Entity, entity: Entity, permissionType: Claim.() -> Boolean): Boolean {
        if (entity is Monster && entity.customName == null)
            return false

        val claim = claimManager.getClaimAt(entity.location.chunk)

        return (claim != null && claim.disablePVP && source is Player && entity is Player) || !(entity is Player || claim == null || claim.permissionType() || (source is Player && (claim.isAccessible(source) || source.hasPermission("chunkprotector.ignore"))))
    }

    private fun filterProtectedBlocks(changedBlocks: MutableList<Block>, configCheck: Claim.() -> Boolean) {
        val chunks = HashMap<Chunk, MutableList<Block>>()

        for (block in changedBlocks) {
            if (block.chunk !in chunks)
                chunks[block.chunk] = ArrayList()
            chunks[block.chunk]!!.add(block)
        }

        for ((chunk, blocks) in chunks) {
            val claim = claimManager.getClaimAt(chunk)
            if (claim != null && !claim.configCheck())
                changedBlocks.removeAll(blocks)
        }
    }

    private fun doConditionalCancellation(player: Player, event: Cancellable, eventLocation: Location = player.location) {
        if (event.isCancelled || player.hasPermission("chunkprotector.ignore"))
            return

        if (claimManager.getClaimAt(eventLocation.chunk)?.isAccessible(player) == false)
            event.isCancelled = true
    }
}