package org.zoobastiks.ztelegram.lis

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.zoobastiks.ztelegram.ZTele
import org.zoobastiks.ztelegram.bot.TBot
import org.bukkit.Bukkit

class TLis(private val plugin: ZTele) : Listener {
    private val bot: TBot
        get() = ZTele.bot

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerName = event.player.name
        bot.sendPlayerJoinMessage(playerName)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerName = event.player.name
        bot.sendPlayerQuitMessage(playerName)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val deathMessage = event.deathMessage ?: ""
        bot.sendPlayerDeathMessage(player.name, deathMessage)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        try {
            val playerName = event.player.name
            val message = PlainTextComponentSerializer.plainText().serialize(event.message())
            bot.sendPlayerChatMessage(playerName, message)
        } catch (e: Exception) {
            plugin.logger.warning("Error processing AsyncChatEvent: ${e.message}")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val playerName = event.player.name
        val message = event.message
        bot.sendPlayerChatMessage(playerName, message)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerChatLowestPriority(event: AsyncPlayerChatEvent) {
        if (!event.isCancelled) {
            val playerName = event.player.name
            val message = event.message
            
            if (Bukkit.getPluginManager().getPlugin("AdvancedChat") != null) {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    bot.sendPlayerChatMessage(playerName, message)
                }, 1L)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        val playerName = event.player.name
        val command = event.message.substring(1) // Remove the leading '/'
        
        // Log command to Telegram
        bot.sendPlayerCommandMessage(playerName, command)
    }
} 