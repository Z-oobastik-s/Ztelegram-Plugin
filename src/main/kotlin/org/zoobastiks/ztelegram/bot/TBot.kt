package org.zoobastiks.ztelegram.bot

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.zoobastiks.ztelegram.ZTele
import org.zoobastiks.ztelegram.conf.TConf
import org.zoobastiks.ztelegram.mgr.PMgr
import org.zoobastiks.ztelegram.ColorUtils
import org.zoobastiks.ztelegram.GradientUtils
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class TBot(private val plugin: ZTele) : TelegramLongPollingBot() {
    private val conf: TConf = ZTele.conf
    private val mgr: PMgr = ZTele.mgr
    private var botsApi: TelegramBotsApi? = null
    private val miniMessage = MiniMessage.miniMessage()
    private var botSession: DefaultBotSession? = null
    
    override fun getBotToken(): String {
        return TConf.botToken
    }
    
    fun start() {
        try {
            // Создаем экземпляр API
            botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            
            // Сначала очищаем предыдущие сессии с помощью clean запроса
            try {
                val clean = org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook()
                clean.dropPendingUpdates = true
                execute(clean)
            } catch (e: Exception) {
                // Игнорируем ошибки здесь
            }
            
            // Регистрируем бота с получением ссылки на сессию
            val session = botsApi!!.registerBot(this)
            
            // Сохраняем ссылку на сессию для последующего корректного закрытия
            if (session is DefaultBotSession) {
                botSession = session
            }
            
            plugin.logger.info("Telegram bot started successfully!")
        } catch (e: TelegramApiException) {
            plugin.logger.severe("Failed to start Telegram bot: ${e.message}")
            throw e
        }
    }
    
    fun stop() {
        try {
            // 1. Сначала пытаемся очистить обновления
            try {
                val clean = org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook()
                clean.dropPendingUpdates = true
                execute(clean)
            } catch (e: Exception) {
                // Игнорируем ошибки здесь
            }
            
            // 2. Закрываем сессию, если она существует
            if (botSession != null) {
                try {
                    botSession!!.stop()
                } catch (e: Exception) {
                    plugin.logger.warning("Error stopping bot session: ${e.message}")
                }
                botSession = null
            }
            
            // 3. Обнуляем ссылку на API
            botsApi = null
            
            // 4. Уничтожаем все активные потоки, связанные с DefaultBotSession
            val threadGroup = Thread.currentThread().threadGroup
            val threads = arrayOfNulls<Thread>(threadGroup.activeCount())
            threadGroup.enumerate(threads)
            
            for (thread in threads) {
                if (thread != null && thread.name.contains("DefaultBotSession")) {
                    try {
                        thread.interrupt()
                    } catch (e: Exception) {
                        // Игнорируем ошибки
                    }
                }
            }
            
            plugin.logger.info("Telegram bot stopped")
        } catch (e: Exception) {
            plugin.logger.severe("Error stopping Telegram bot: ${e.message}")
        }
    }
    
    override fun getBotUsername(): String {
        return "YourTelegramBot"
    }
    
    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage()) return
        
        val message = update.message
        if (!message.hasText()) return
        
        val chatId = message.chatId.toString()
        val text = message.text
        val username = message.from.userName ?: "Unknown"
        
        when (chatId) {
            conf.mainChannelId -> handleMainChannelMessage(text, username)
            conf.consoleChannelId -> handleConsoleChannelMessage(text, username)
            conf.registerChannelId -> handleRegisterChannelMessage(text, username)
        }
    }
    
    private fun handleMainChannelMessage(text: String, username: String) {
        if (!conf.mainChannelEnabled) return
        
        if (text.startsWith("/")) {
            handleMainChannelCommand(text, username)
            return
        }
        
        // Проверяем, не играет ли пользователь в игру
        if (ZTele.game.hasActiveGame(username)) {
            // Обрабатываем ответ на игру
            val (isCorrect, message) = ZTele.game.checkAnswer(username, text)
            
            // Отправляем ответ
            sendToMainChannel(message)
            
            // Если ответ правильный, не отправляем сообщение в чат
            if (isCorrect) return
        }
        
        if (conf.mainChannelChatEnabled) {
            val formattedMessage = conf.formatTelegramToMinecraft
                .replace("%player%", username)
                .replace("%message%", text)
                .replace("\\n", "\n")
            
            // Отправляем сообщение на сервер с поддержкой градиентов и цветов
            sendFormattedMessageToServer(formattedMessage)
        }
    }
    
    // Универсальный метод для отправки отформатированных сообщений на сервер
    private fun sendFormattedMessageToServer(message: String) {
        // Проверяем наличие MiniMessage форматирования
        if (message.contains("<") && message.contains(">")) {
            // Если есть MiniMessage теги (градиенты и др.)
            val component = GradientUtils.parseMixedFormat(message)
            Bukkit.getServer().sendMessage(component)
        } else {
            // Для обычных цветовых кодов
            val processedMessage = ColorUtils.translateColorCodes(message)
            Bukkit.getServer().broadcast(Component.text().append(
                LegacyComponentSerializer.legacySection().deserialize(processedMessage)
            ).build())
        }
    }
    
    // Метод для форматирования текста с заменой плейсхолдеров и обработкой переносов строк
    private fun formatMessage(template: String, replacements: Map<String, String>): String {
        var result = template
        
        // Заменяем плейсхолдеры
        for ((key, value) in replacements) {
            result = result.replace(key, value)
        }
        
        // Обрабатываем переносы строк
        return result.replace("\\n", "\n")
    }
    
    private fun handleMainChannelCommand(command: String, username: String) {
        // Создаем карту команд и их псевдонимов
        val commandAliases = mapOf(
            "online" to setOf("/online", "/онлайн"),
            "tps" to setOf("/tps", "/тпс"),
            "restart" to setOf("/restart", "/рестарт"),
            "gender" to setOf("/gender", "/пол"),
            "player" to setOf("/player", "/ник", "/игрок"),
            "commands" to setOf("/cmd", "/команды", "/commands", "/help", "/помощь"),
            "game" to setOf("/game", "/игра")
        )
        
        // Определяем, какая команда была введена
        val baseCommand = commandAliases.entries.find { entry ->
            entry.value.any { alias -> command.lowercase().startsWith(alias) }
        }?.key
        
        // Извлекаем аргументы из команды (часть после первого пробела)
        val arguments = if (command.contains(" ")) command.substring(command.indexOf(" ") + 1) else ""
        
        when (baseCommand) {
            "online" -> {
                if (!conf.enabledOnlineCommand) return
                
                val onlinePlayers = Bukkit.getOnlinePlayers().size
                val maxPlayers = Bukkit.getMaxPlayers()
                val playerList = Bukkit.getOnlinePlayers().joinToString(", ") { it.name }
                
                val response = formatMessage(conf.onlineCommandResponse, mapOf(
                    "%online%" to onlinePlayers.toString(),
                    "%max%" to maxPlayers.toString(),
                    "%players%" to playerList
                ))
                
                sendToMainChannel(response)
            }
            
            "tps" -> {
                if (!conf.enabledTpsCommand) return
                
                val tps = Bukkit.getTPS()
                val formattedTps = tps.joinToString(", ") { String.format("%.2f", it) }
                
                val response = formatMessage(conf.tpsCommandResponse, mapOf(
                    "%tps%" to formattedTps
                ))
                
                sendToMainChannel(response)
            }
            
            "restart" -> {
                if (!conf.enabledRestartCommand) return
                
                sendToMainChannel(conf.restartCommandResponse.replace("\\n", "\n"))
                
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart")
                })
            }
            
            "gender" -> {
                if (!conf.enabledGenderCommand) return
                
                if (arguments.isEmpty()) {
                    sendToMainChannel(conf.genderCommandUsage.replace("\\n", "\n"))
                    return
                }
                
                // Конвертируем русские варианты в английские
                val genderArg = when (arguments.lowercase()) {
                    "м", "муж", "мужской", "man", "male" -> "man"
                    "ж", "жен", "женский", "girl", "female" -> "girl"
                    else -> ""
                }
                
                if (genderArg.isEmpty()) {
                    sendToMainChannel(conf.genderCommandUsage.replace("\\n", "\n"))
                    return
                }
                
                val telegramId = username
                val player = mgr.getPlayerByTelegramId(telegramId)
                
                if (player == null) {
                    sendToMainChannel(conf.genderCommandNoPlayer.replace("\\n", "\n"))
                    return
                }
                
                mgr.setPlayerGender(player, genderArg)
                
                val response = formatMessage(conf.genderCommandResponse, mapOf(
                    "%player%" to player,
                    "%gender%" to genderArg
                ))
                
                sendToMainChannel(response)
            }
            
            "player" -> {
                if (!conf.enabledPlayerCommand) return
                
                if (arguments.isEmpty()) {
                    sendToMainChannel(conf.playerCommandUsage.replace("\\n", "\n"))
                    return
                }
                
                val playerName = arguments.split(" ")[0]
                val playerData = mgr.getPlayerData(playerName)
                
                // Проверяем, существует ли игрок в Minecraft, даже если не зарегистрирован
                val isOnline = Bukkit.getPlayerExact(playerName) != null
                val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                
                if (!offlinePlayer.hasPlayedBefore() && !isOnline) {
                    sendToMainChannel(formatMessage(conf.playerCommandNoPlayer, mapOf(
                        "%player%" to playerName
                    )))
                    return
                }
                
                val rawGender = playerData?.gender ?: "Not set"
                // Используем перевод для gender
                val gender = if (rawGender == "man" || rawGender == "girl") conf.getGenderTranslation(rawGender) else conf.getStatusTranslation("not_set")
                
                // Форматируем баланс с двумя знаками после запятой
                val rawBalance = getPlayerBalance(playerName)
                val balance = String.format("%.2f", rawBalance)
                
                val currentHealth = if (isOnline) Bukkit.getPlayerExact(playerName)?.health?.toInt() ?: 0 else 0
                val coords = if (isOnline) {
                    val loc = Bukkit.getPlayerExact(playerName)?.location
                    "X: ${loc?.blockX}, Y: ${loc?.blockY}, Z: ${loc?.blockZ}"
                } else conf.getStatusTranslation("offline_coords")
                
                // Переводим статусы для отображения
                val onlineStatus = if (isOnline) conf.getStatusTranslation("online") else conf.getStatusTranslation("offline")
                
                // Форматируем дату регистрации с корректным форматом
                val registeredDate = if (playerData?.registered != null) {
                    try {
                        // Парсим исходную дату
                        val originalFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        val date = originalFormat.parse(playerData.registered)
                        
                        // Устанавливаем часовой пояс МСК (+3)
                        originalFormat.timeZone = java.util.TimeZone.getTimeZone("Europe/Moscow")
                        
                        // Форматируем дату в нужный формат
                        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        dateFormat.timeZone = java.util.TimeZone.getTimeZone("Europe/Moscow")
                        dateFormat.format(date)
                    } catch (e: Exception) {
                        // В случае ошибки парсинга оставляем исходную дату
                        playerData.registered
                    }
                } else conf.getStatusTranslation("not_registered")
                
                // Добавляем новую информацию
                val firstPlayed = if (offlinePlayer.hasPlayedBefore()) {
                    val date = java.util.Date(offlinePlayer.firstPlayed)
                    val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                    dateFormat.format(date)
                } else conf.getStatusTranslation("never")
                
                val deaths = offlinePlayer.getStatistic(org.bukkit.Statistic.DEATHS)
                val level = if (isOnline) Bukkit.getPlayerExact(playerName)?.level ?: 0 else 0
                
                val response = formatMessage(conf.playerCommandResponse, mapOf(
                    "%player%" to playerName,
                    "%gender%" to gender,
                    "%balance%" to balance,
                    "%online%" to onlineStatus,
                    "%health%" to currentHealth.toString(),
                    "%registered%" to registeredDate,
                    "%coords%" to coords,
                    "%first_played%" to firstPlayed,
                    "%deaths%" to deaths.toString(),
                    "%level%" to level.toString()
                ))
                
                sendToMainChannel(response)
            }
            
            "commands" -> {
                if (!conf.enabledCommandsListCommand) return
                
                // Отправляем список доступных команд
                sendToMainChannel(conf.commandsListResponse.replace("\\n", "\n"))
            }
            
            "game" -> {
                // Получаем имя игрока для игры
                var playerName = ""
                
                // Проверяем аргументы
                if (arguments.isNotEmpty()) {
                    playerName = arguments.split(" ")[0]
                } else {
                    // Если аргументы не указаны, проверяем, есть ли привязанный игрок
                    playerName = mgr.getPlayerByTelegramId(username) ?: ""
                    
                    if (playerName.isEmpty()) {
                        sendToMainChannel(ZTele.game.gameNotRegisteredMessage)
                        return
                    }
                }
                
                // Запускаем игру
                val gameResponse = ZTele.game.startGame(username, playerName)
                sendToMainChannel(gameResponse)
            }
        }
    }
    
    private fun getPlayerBalance(playerName: String): Double {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return 0.0
        
        val rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy::class.java)
        val economy = rsp?.provider ?: return 0.0
        
        return economy.getBalance(Bukkit.getOfflinePlayer(playerName))
    }
    
    private fun handleConsoleChannelMessage(text: String, username: String) {
        if (!conf.consoleChannelEnabled) return
        
        Bukkit.getScheduler().runTask(plugin, Runnable {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), text)
                
                if (conf.consoleCommandFeedbackEnabled) {
                    val response = formatMessage(conf.consoleCommandFeedback, mapOf(
                        "%command%" to text,
                        "%user%" to username
                    ))
                    
                    sendToConsoleChannel(response)
                }
            } catch (e: Exception) {
                val errorMsg = formatMessage(conf.consoleCommandError, mapOf(
                    "%command%" to text,
                    "%error%" to (e.message ?: "Unknown error")
                ))
                
                sendToConsoleChannel(errorMsg)
            }
        })
    }
    
    private fun handleRegisterChannelMessage(text: String, username: String) {
        if (!conf.registerChannelEnabled) return
        
        val playerName = text.trim()
        if (!playerName.matches(Regex("^[a-zA-Z0-9_]{2,16}$"))) {
            sendToRegisterChannel(formatMessage(conf.registerInvalidUsername, mapOf(
                "%player%" to playerName
            )))
            return
        }
        
        if (mgr.isPlayerRegistered(playerName)) {
            sendToRegisterChannel(formatMessage(conf.registerAlreadyRegistered, mapOf(
                "%player%" to playerName
            )))
            return
        }
        
        val player = Bukkit.getPlayerExact(playerName)
        if (player == null) {
            sendToRegisterChannel(formatMessage(conf.registerPlayerOffline, mapOf(
                "%player%" to playerName
            )))
            return
        }
        
        mgr.registerPlayer(playerName, username)
        
        val telegramMsg = formatMessage(conf.registerSuccess, mapOf(
            "%player%" to playerName
        ))
        sendToRegisterChannel(telegramMsg)
        
        // Отправляем сообщение с поддержкой градиентов и цветов
        val inGameMessage = formatMessage(conf.registerSuccessInGame, mapOf(
            "%player%" to playerName
        ))
        
        // Отправляем игроку через универсальный форматировщик
        sendComponentToPlayer(player, inGameMessage)
        
        // Выполняем команды для выдачи наград
        Bukkit.getScheduler().runTask(plugin, Runnable {
            for (command in conf.registerRewardCommands) {
                try {
                    val cmd = command.replace("%player%", playerName)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to execute reward command for player $playerName: ${e.message}")
                }
            }
        })
    }
    
    // Отправляет отформатированное сообщение игроку
    private fun sendComponentToPlayer(player: org.bukkit.entity.Player, message: String) {
        if (message.contains("<") && message.contains(">")) {
            // Если есть MiniMessage теги (градиенты и др.)
            val component = GradientUtils.parseMixedFormat(message)
            player.sendMessage(component)
        } else {
            // Для обычных цветовых кодов
            player.sendMessage(ColorUtils.translateColorCodes(message))
        }
    }
    
    fun sendServerStartMessage() {
        if (!conf.mainChannelEnabled || !conf.serverStartEnabled) return
        
        sendToMainChannel(conf.serverStartMessage.replace("\\n", "\n"))
    }
    
    fun sendServerStopMessage() {
        if (!conf.mainChannelEnabled || !conf.serverStopEnabled) return
        
        sendToMainChannel(conf.serverStopMessage.replace("\\n", "\n"))
    }
    
    fun sendPlayerJoinMessage(playerName: String) {
        if (!conf.mainChannelEnabled || !conf.playerJoinEnabled || mgr.isPlayerHidden(playerName)) return
        
        val message = formatMessage(conf.playerJoinMessage, mapOf(
            "%player%" to playerName
        ))
        sendToMainChannel(message)
    }
    
    fun sendPlayerQuitMessage(playerName: String) {
        if (!conf.mainChannelEnabled || !conf.playerQuitEnabled || mgr.isPlayerHidden(playerName)) return
        
        val message = formatMessage(conf.playerQuitMessage, mapOf(
            "%player%" to playerName
        ))
        sendToMainChannel(message)
    }
    
    fun sendPlayerDeathMessage(playerName: String, deathMessage: String) {
        if (!conf.mainChannelEnabled || !conf.playerDeathEnabled || mgr.isPlayerHidden(playerName)) return
        
        // Заменяем имя игрока в сообщении о смерти на плейсхолдер
        // Это необходимо для корректного форматирования, так как имя игрока 
        // может встречаться в сообщении о смерти в разных падежах
        var processedDeathMessage = deathMessage
        
        // Пробуем убрать имя игрока из сообщения о смерти, если оно там есть
        if (processedDeathMessage.contains(playerName)) {
            processedDeathMessage = processedDeathMessage.replace(playerName, "")
        }
        
        // Если сообщение начинается с лишних символов (часто остаются после удаления имени)
        processedDeathMessage = processedDeathMessage.trimStart(' ', '.', ',', ':')
        
        val message = formatMessage(conf.playerDeathMessage, mapOf(
            "%player%" to playerName,
            "%death_message%" to processedDeathMessage
        ))
        
        sendToMainChannel(message)
    }
    
    fun sendPlayerChatMessage(playerName: String, chatMessage: String) {
        if (!conf.mainChannelEnabled || !conf.playerChatEnabled || mgr.isPlayerHidden(playerName)) return
        
        val message = formatMessage(conf.formatMinecraftToTelegram, mapOf(
            "%player%" to playerName,
            "%message%" to chatMessage
        ))
        
        sendToMainChannel(message)
    }
    
    fun sendPlayerCommandMessage(playerName: String, command: String) {
        if (!conf.consoleChannelEnabled || !conf.playerCommandLogEnabled || mgr.isPlayerHidden(playerName)) return
        
        // Используем часовой пояс МСК (+3) для времени
        val now = LocalDateTime.now(java.time.ZoneId.of("Europe/Moscow"))
        val timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        
        val message = formatMessage(conf.playerCommandLogFormat, mapOf(
            "%time%" to timestamp,
            "%player%" to playerName,
            "%command%" to command
        ))
        
        sendToConsoleChannel(message)
    }
    
    fun sendToMainChannel(message: String) {
        sendMessage(conf.mainChannelId, message)
    }
    
    fun sendToConsoleChannel(message: String) {
        sendMessage(conf.consoleChannelId, message)
    }
    
    fun sendToRegisterChannel(message: String) {
        sendMessage(conf.registerChannelId, message)
    }
    
    private fun sendMessage(chatId: String, message: String) {
        if (chatId.isEmpty() || message.isEmpty()) return
        
        try {
            val sendMessage = SendMessage(chatId, convertToHtml(message))
            sendMessage.parseMode = "HTML"
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            plugin.logger.warning("Failed to send message to Telegram: ${e.message}")
            plugin.scheduleReconnect()
        }
    }
    
    private fun convertToHtml(text: String): String {
        // Заменяем \n на настоящие переносы строк
        var processedText = text.replace("\\n", "\n")
        
        // Сохраняем кавычки для моноширинного шрифта
        val codeBlocks = mutableMapOf<String, String>()
        var codeCounter = 0
        
        // Сохраняем одиночные обратные кавычки для последующей обработки
        processedText = processedText.replace(Regex("`([^`]+)`")) { match ->
            val placeholder = "CODE_BLOCK_${codeCounter++}"
            codeBlocks[placeholder] = match.groupValues[1]
            placeholder
        }
        
        // Обрабатываем Markdown разметку и запоминаем отформатированные участки
        val formattedParts = mutableMapOf<String, String>()
        var counter = 0
        
        // Жирный текст - разные варианты
        processedText = processedText.replace(Regex("\\*\\*(.*?)\\*\\*|<b>(.*?)</b>|<strong>(.*?)</strong>")) { match -> 
            val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<b>$content</b>"
            placeholder
        }
        
        // Курсив - разные варианты
        processedText = processedText.replace(Regex("\\*(.*?)\\*|<i>(.*?)</i>|<em>(.*?)</em>")) { match -> 
            val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<i>$content</i>"
            placeholder
        }
        
        // Моноширинный шрифт (код) - другие варианты
        processedText = processedText.replace(Regex("<code>(.*?)</code>")) { match -> 
            val content = match.groupValues[1]
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<code>$content</code>"
            placeholder
        }
        
        // Зачеркнутый текст - разные варианты
        processedText = processedText.replace(Regex("~~(.*?)~~|<s>(.*?)</s>|<strike>(.*?)</strike>|<del>(.*?)</del>")) { match -> 
            val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<s>$content</s>"
            placeholder
        }
        
        // Подчеркнутый текст
        processedText = processedText.replace(Regex("<u>(.*?)</u>|__(.*?)__")) { match -> 
            val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<u>$content</u>"
            placeholder
        }
        
        // Многострочный код с указанием языка
        processedText = processedText.replace(Regex("```([a-zA-Z0-9+]+)?\n(.*?)```", RegexOption.DOT_MATCHES_ALL)) { match -> 
            val language = match.groupValues[1].takeIf { it.isNotEmpty() } ?: ""
            val code = match.groupValues[2]
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            
            if (language.isNotEmpty()) {
                formattedParts[placeholder] = "<pre language=\"$language\">$code</pre>"
            } else {
                formattedParts[placeholder] = "<pre>$code</pre>"
            }
            
            placeholder
        }
        
        // Проверяем наличие тега <pre> с атрибутом language
        processedText = processedText.replace(Regex("<pre language=\"([a-zA-Z0-9+]+)\">(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)) { match -> 
            val language = match.groupValues[1]
            val code = match.groupValues[2]
            val placeholder = "FORMAT_PLACEHOLDER_${counter++}"
            formattedParts[placeholder] = "<pre language=\"$language\">$code</pre>"
            placeholder
        }
        
        // Если текст содержит градиенты или теги MiniMessage
        if (processedText.contains("<") && processedText.contains(">")) {
            try {
                // Обрабатываем MiniMessage форматирование
                val component = GradientUtils.parseMixedFormat(processedText)
                processedText = PlainTextComponentSerializer.plainText().serialize(component)
            } catch (e: Exception) {
                plugin.logger.warning("Error parsing MiniMessage format: ${e.message}")
            }
        }
        
        // Для обычных цветовых кодов
        try {
            val component = LegacyComponentSerializer.legacySection().deserialize(
                processedText.replace("&", "§")
            )
            processedText = PlainTextComponentSerializer.plainText().serialize(component)
        } catch (e: Exception) {
            // Если произошла ошибка, просто убираем цветовые коды
            processedText = ColorUtils.stripColorCodes(processedText)
        }
        
        // Восстанавливаем блоки кода
        for ((placeholder, content) in codeBlocks) {
            processedText = processedText.replace(placeholder, content)
        }
        
        // Экранируем специальные символы HTML
        processedText = processedText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        
        // Восстанавливаем placeholders с форматированием
        for ((placeholder, htmlTag) in formattedParts) {
            processedText = processedText.replace(placeholder, htmlTag)
        }
        
        // Обрабатываем сохраненные блоки кода
        processedText = processedText.replace(Regex("CODE_BLOCK_(\\d+)")) { match ->
            val index = match.groupValues[1].toInt()
            val content = codeBlocks["CODE_BLOCK_$index"] ?: ""
            "<code>$content</code>"
        }
        
        return processedText
    }
} 