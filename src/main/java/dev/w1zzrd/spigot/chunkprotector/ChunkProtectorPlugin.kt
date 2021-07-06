package dev.w1zzrd.spigot.chunkprotector

import dev.w1zzrd.spigot.chunkprotector.claim.Claim
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimChunk
import dev.w1zzrd.spigot.chunkprotector.claim.ClaimManager
import dev.w1zzrd.spigot.chunkprotector.collection.SerializableBinaryList
import dev.w1zzrd.spigot.chunkprotector.command.*
import dev.w1zzrd.spigot.chunkprotector.freecam.FreeCamManager
import dev.w1zzrd.spigot.chunkprotector.listener.PlayerActionListener
import dev.w1zzrd.spigot.chunkprotector.listener.TabCompleteListener
import dev.w1zzrd.spigot.wizcompat.serialization.PersistentData
import dev.w1zzrd.spigot.wizcompat.serialization.UUIDList
import kr.entree.spigradle.annotations.SpigotPlugin
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

@SpigotPlugin
class ChunkProtectorPlugin: JavaPlugin() {
    private val freeCamManager = FreeCamManager()

    private val tabCompleteListener = TabCompleteListener()

    // Properties that should be initialized only after serializers are enabled
    private val persistentData: PersistentData by lazy { PersistentData("data", this) }
    private val claimManager: ClaimManager by lazy { ClaimManager(freeCamManager, persistentData) }
    private val actionListener: PlayerActionListener by lazy {
        PlayerActionListener(claimManager)
    }

    override fun onEnable() {
        super.onEnable()
        enableSerializers()
        freeCamManager.onEnable(this)
        claimManager.onEnable(this)
        enableListeners()
        enableCommands()
    }

    private fun enableSerializers() {
        ConfigurationSerialization.registerClass(UUIDList::class.java)
        ConfigurationSerialization.registerClass(SerializableBinaryList::class.java)
        ConfigurationSerialization.registerClass(ClaimChunk::class.java)
        ConfigurationSerialization.registerClass(Claim::class.java)
    }

    private fun enableListeners() {
        server.pluginManager.registerEvents(tabCompleteListener, this)
        server.pluginManager.registerEvents(actionListener, this)
    }

    private fun enableCommands() {
        getCommand("claim")!!.setExecutor(ClaimCommand(claimManager))
        getCommand("unclaim")!!.setExecutor(UnClaimCommand(claimManager))
        getCommand("invite")!!.setExecutor(InviteCommand(claimManager))
        getCommand("uninvite")!!.setExecutor(UnInviteCommand(claimManager))
        getCommand("claims")!!.setExecutor(ListClaimsCommand(claimManager))
        getCommand("claimowner")!!.setExecutor(ClaimOwnerCommand(claimManager))
        getCommand("showclaim")!!.setExecutor(ShowClaimCommand(claimManager, this))
        getCommand("claimoption")!!.setExecutor(
            ClaimOptionCommand(claimManager).also { tabCompleteListener.registerTabCompleter("claimoption", it.completionProcessor) }
        )
    }

    override fun onDisable() {
        disableListeners()
        claimManager.onDisable()
        freeCamManager.onDisable()
        persistentData.saveData()
        disableSerializers()
        super.onDisable()
    }

    private fun disableSerializers() {
        ConfigurationSerialization.unregisterClass(Claim::class.java)
        ConfigurationSerialization.unregisterClass(ClaimChunk::class.java)
        ConfigurationSerialization.unregisterClass(SerializableBinaryList::class.java)
        ConfigurationSerialization.unregisterClass(UUIDList::class.java)
    }

    private fun disableListeners() {
        HandlerList.unregisterAll(actionListener)
        tabCompleteListener.unRegisterAll()
        HandlerList.unregisterAll(tabCompleteListener)
    }
}