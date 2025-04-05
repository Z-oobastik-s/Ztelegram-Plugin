package org.zoobastiks.ztelegram.cmd

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.zoobastiks.ztelegram.GradientUtils
import org.zoobastiks.ztelegram.ZTele
import org.zoobastiks.ztelegram.conf.TConf
import org.zoobastiks.ztelegram.mgr.PMgr

class TCmds(private val plugin: ZTele) : CommandExecutor, TabCompleter {
    private val conf: TConf
        get() = ZTele.conf
    private val mgr: PMgr
        get() = ZTele.mgr
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            handleTelegramCommand(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("ztelegram.admin")) {
                    sender.sendMessage("${ZTele.conf.pluginPrefix} §cYou don't have permission to do that!")
                    return true
                }
                
                if (args.size > 1 && args[1].lowercase() == "game") {
                    // Перезагрузка только конфигурации игры
                    plugin.reloadGame()
                    sender.sendMessage("${ZTele.conf.pluginPrefix} §aGame configuration reloaded!")
                    return true
                }
                
                sender.sendMessage("${ZTele.conf.pluginPrefix} §eReloading plugin...")
                plugin.reload()
                sender.sendMessage("${ZTele.conf.pluginPrefix} §aPlugin reloaded!")
                return true
            }
            "help" -> {
                showHelpMenu(sender)
                return true
            }
            "unregister" -> return handleUnregisterCommand(sender, args)
            "addplayer" -> return handleAddPlayerCommand(sender, args)
            "removeplayer" -> return handleRemovePlayerCommand(sender, args)
            "addchannel" -> return handleAddChannelCommand(sender, args)
            "hidden" -> return handleHiddenListCommand(sender, args)
            else -> {
                sender.sendMessage("${conf.pluginPrefix} §cUnknown command: §f${args[0]}")
                sender.sendMessage("${conf.pluginPrefix} §7Use §f/telegram help §7for a list of commands")
                return false
            }
        }
        
        handleTelegramCommand(sender)
        return true
    }
    
    private fun handleTelegramCommand(sender: CommandSender): Boolean {
        // Отправляем компонент с ссылкой на телеграм
        val component = GradientUtils.parseMixedFormat(conf.telegramCommandMessage)
        
        if (sender is Player) {
            sender.sendMessage(component)
            
            // Отправляем компонент с кликабельной ссылкой
            val clickableText = Component.text()
                .append(Component.text("» "))
                .append(Component.text("Нажмите сюда, чтобы открыть")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(conf.telegramLink))
                    .hoverEvent(HoverEvent.showText(Component.text("Открыть Telegram канал"))))
                .build()
            
            sender.sendMessage(clickableText)
        } else {
            sender.sendMessage(PlainTextComponentSerializer.plainText().serialize(component))
            sender.sendMessage("» ${conf.telegramLink}")
        }
        
        return true
    }
    
    private fun handleUnregisterCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("ztelegram.unregister")) {
            sender.sendMessage("${conf.pluginPrefix} §cYou don't have permission to use this command!")
            return false
        }
        
        if (args.size < 2) {
            sender.sendMessage("${conf.pluginPrefix} §cUsage: /telegram unregister <nickname>")
            return false
        }
        
        val playerName = args[1]
        
        if (!mgr.isPlayerRegistered(playerName)) {
            sender.sendMessage("${conf.pluginPrefix} §cPlayer §f$playerName §cis not registered!")
            return false
        }
        
        mgr.unregisterPlayer(playerName)
        sender.sendMessage("${conf.pluginPrefix} §aPlayer §f$playerName §ahas been unregistered!")
        
        return true
    }
    
    private fun handleAddPlayerCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("ztelegram.hideshow")) {
            sender.sendMessage("${conf.pluginPrefix} §cYou don't have permission to use this command!")
            return false
        }
        
        if (sender !is Player && args.size < 2) {
            sender.sendMessage("${conf.pluginPrefix} §cUsage: /telegram addplayer <nickname>")
            return false
        }
        
        val playerName = if (args.size >= 2) args[1] else (sender as Player).name
        
        if (mgr.isPlayerHidden(playerName)) {
            sender.sendMessage("${conf.pluginPrefix} §cPlayer §f$playerName §cis already hidden!")
            return false
        }
        
        mgr.addHiddenPlayer(playerName)
        sender.sendMessage("${conf.pluginPrefix} §aPlayer §f$playerName §ahas been hidden from Telegram messages!")
        
        return true
    }
    
    private fun handleRemovePlayerCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("ztelegram.hideshow")) {
            sender.sendMessage("${conf.pluginPrefix} §cYou don't have permission to use this command!")
            return false
        }
        
        if (sender !is Player && args.size < 2) {
            sender.sendMessage("${conf.pluginPrefix} §cUsage: /telegram removeplayer <nickname>")
            return false
        }
        
        val playerName = if (args.size >= 2) args[1] else (sender as Player).name
        
        if (!mgr.isPlayerHidden(playerName)) {
            sender.sendMessage("${conf.pluginPrefix} §cPlayer §f$playerName §cis not hidden!")
            return false
        }
        
        mgr.removeHiddenPlayer(playerName)
        sender.sendMessage("${conf.pluginPrefix} §aPlayer §f$playerName §ais now visible in Telegram messages!")
        
        return true
    }
    
    private fun handleAddChannelCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("ztelegram.admin")) {
            sender.sendMessage("${conf.pluginPrefix} §cYou don't have permission to use this command!")
            return false
        }
        
        if (args.size < 3) {
            sender.sendMessage("${conf.pluginPrefix} §cUsage: /telegram addchannel <1|2|3> <channelId>")
            return false
        }
        
        val channelNumber = args[1].toIntOrNull()
        if (channelNumber == null || channelNumber < 1 || channelNumber > 3) {
            sender.sendMessage("${conf.pluginPrefix} §cInvalid channel number: §f${args[1]}")
            return false
        }
        
        val channelId = args[2]
        
        val config = plugin.config
        when (channelNumber) {
            1 -> {
                config.set("channels.main", channelId)
                conf.mainChannelId = channelId
                sender.sendMessage("${conf.pluginPrefix} §aMain channel ID updated to §f$channelId")
            }
            2 -> {
                config.set("channels.console", channelId)
                conf.consoleChannelId = channelId
                sender.sendMessage("${conf.pluginPrefix} §aConsole channel ID updated to §f$channelId")
            }
            3 -> {
                config.set("channels.register", channelId)
                conf.registerChannelId = channelId
                sender.sendMessage("${conf.pluginPrefix} §aRegister channel ID updated to §f$channelId")
            }
        }
        
        plugin.saveConfig()
        return true
    }
    
    private fun showHelpMenu(sender: CommandSender) {
        val prefix = conf.pluginPrefix
        
        sender.sendMessage("$prefix §6✦✦✦✦✦✦✦ §e§lZTelegram Commands §6✦✦✦✦✦✦✦")
        sender.sendMessage("$prefix §r")
        
        // Базовые команды для всех
        sender.sendMessage("$prefix §e/telegram §7- §fПоказать ссылку на Telegram канал")
        
        // Команды для игроков
        if (sender.hasPermission("ztelegram.hideshow")) {
            sender.sendMessage("$prefix §e/telegram addplayer [nickname] §7- §fСкрыть игрока из сообщений в Telegram")
            sender.sendMessage("$prefix §e/telegram removeplayer [nickname] §7- §fПоказывать игрока в сообщениях Telegram")
        }
        
        // Команды для модераторов
        if (sender.hasPermission("ztelegram.unregister")) {
            sender.sendMessage("$prefix §e/telegram unregister <nickname> §7- §fУдалить регистрацию игрока")
        }
        
        // Команды для админов
        if (sender.hasPermission("ztelegram.admin")) {
            sender.sendMessage("$prefix §e/telegram reload §7- §fПерезагрузить весь плагин")
            sender.sendMessage("$prefix §e/telegram reload game §7- §fПерезагрузить только конфигурацию игры")
            sender.sendMessage("$prefix §e/telegram addchannel <1|2|3> <channelId> §7- §fНастроить ID канала")
            sender.sendMessage("$prefix §e/telegram hidden §7- §fПоказать список скрытых игроков")
        }
        
        sender.sendMessage("$prefix §r")
        sender.sendMessage("$prefix §6✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦✦")
    }
    
    private fun handleHiddenListCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("ztelegram.admin")) {
            sender.sendMessage("${conf.pluginPrefix} §cYou don't have permission to use this command!")
            return false
        }
        
        val hiddenPlayers = mgr.getHiddenPlayers()
        
        if (hiddenPlayers.isEmpty()) {
            sender.sendMessage("${conf.pluginPrefix} §eThere are no hidden players.")
            return true
        }
        
        sender.sendMessage("${conf.pluginPrefix} §6Hidden players (${hiddenPlayers.size}):")
        for (player in hiddenPlayers) {
            sender.sendMessage("${conf.pluginPrefix} §f- §e$player")
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        if (command.name.equals("telegram", ignoreCase = true)) {
            if (args.size == 1) {
                val completions = mutableListOf<String>()
                
                // Базовые команды для всех
                completions.add("help")
                
                // Добавляем команды в зависимости от прав
                if (sender.hasPermission("ztelegram.admin")) completions.add("reload")
                if (sender.hasPermission("ztelegram.unregister")) completions.add("unregister")
                if (sender.hasPermission("ztelegram.hideshow")) {
                    completions.add("addplayer")
                    completions.add("removeplayer")
                }
                if (sender.hasPermission("ztelegram.admin")) {
                    completions.add("addchannel")
                    completions.add("hidden")
                }
                
                return completions.filter { it.startsWith(args[0], ignoreCase = true) }
            } else if (args.size == 2) {
                when (args[0].lowercase()) {
                    "reload" -> {
                        if (sender.hasPermission("ztelegram.admin")) {
                            return listOf("game").filter { it.startsWith(args[1], ignoreCase = true) }
                        }
                    }
                    "addchannel" -> {
                        if (sender.hasPermission("ztelegram.admin")) {
                            return listOf("1", "2", "3").filter { it.startsWith(args[1], ignoreCase = true) }
                        }
                    }
                    "unregister", "addplayer", "removeplayer" -> {
                        // Дополнение именами игроков
                        val playerNames = Bukkit.getOnlinePlayers().map { it.name }
                        return playerNames.filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                }
            }
        }
        
        return emptyList()
    }
} 