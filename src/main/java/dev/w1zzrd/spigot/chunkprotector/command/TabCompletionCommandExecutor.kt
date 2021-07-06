package dev.w1zzrd.spigot.chunkprotector.command

import dev.w1zzrd.spigot.chunkprotector.listener.CompletionProcessor
import org.bukkit.command.CommandExecutor

abstract class TabCompletionCommandExecutor: CommandExecutor {
    abstract val completionProcessor: CompletionProcessor
}