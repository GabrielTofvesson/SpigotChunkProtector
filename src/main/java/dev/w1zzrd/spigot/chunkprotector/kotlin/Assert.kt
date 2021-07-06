package dev.w1zzrd.spigot.chunkprotector.kotlin

import dev.w1zzrd.spigot.wizcompat.command.CommandUtils.assertTrue
import org.bukkit.command.CommandSender
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T> assertNotNull(value: T?, errorMessage: String, sender: CommandSender): T? {
    contract { returnsNotNull() implies (value != null) }

    assertTrue(value != null, errorMessage, sender)

    return value
}