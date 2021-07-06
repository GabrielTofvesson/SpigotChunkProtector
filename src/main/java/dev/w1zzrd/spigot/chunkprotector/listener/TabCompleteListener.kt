package dev.w1zzrd.spigot.chunkprotector.listener

import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.TabCompleteEvent

typealias CompletionProcessor = (CommandSender, List<String>) -> Collection<String>
private val SPLITTER = Regex(" +")

class TabCompleteListener: Listener {

    private val tabCompleters = HashMap<String, CompletionProcessor>()

    @EventHandler
    fun onTabComplete(event: TabCompleteEvent) {
        if (!event.isCancelled) {
            val split = event.buffer.split(SPLITTER)
            event.completions.addAll((tabCompleters[split[0]] ?: return).invoke(event.sender, if(split.size > 1) split.subList(1, split.size) else emptyList()))
        }
    }

    fun registerTabCompleter(command: String, processor: CompletionProcessor) {
        tabCompleters[command] = processor
    }

    fun unRegisterTabCompleter(command: String) {
        tabCompleters.remove(command)
    }

    fun unRegisterAll() {
        tabCompleters.clear()
    }
}