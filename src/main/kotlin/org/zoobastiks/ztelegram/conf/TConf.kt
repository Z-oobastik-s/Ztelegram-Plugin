package org.zoobastiks.ztelegram.conf

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.zoobastiks.ztelegram.ZTele
import java.io.File

class TConf(private val plugin: ZTele) {
    companion object {
        lateinit var botToken: String
            private set
    }
    
    // Channel IDs
    var mainChannelId: String = "-1002111043217"
    var consoleChannelId: String = "-1002656200279"
    var registerChannelId: String = "-1002611802353"
    
    // Main channel settings
    var mainChannelEnabled: Boolean = true
    var mainChannelChatEnabled: Boolean = true
    var formatTelegramToMinecraft: String = "&b[Telegram] &f%player%: &7%message%"
    var formatMinecraftToTelegram: String = "📤 **%player%**: %message%"
    
    // Server events
    var serverStartEnabled: Boolean = true
    var serverStopEnabled: Boolean = true
    var serverStartMessage: String = "🟢 Server started"
    var serverStopMessage: String = "🔴 Server stopped"
    
    // Player events
    var playerJoinEnabled: Boolean = true
    var playerQuitEnabled: Boolean = true
    var playerDeathEnabled: Boolean = true
    var playerChatEnabled: Boolean = true
    var playerJoinMessage: String = "🟢 %player% joined the server"
    var playerQuitMessage: String = "🔴 %player% left the server"
    var playerDeathMessage: String = "💀 %player% %death_message%"
    
    // Telegram commands
    var enabledOnlineCommand: Boolean = true
    var enabledTpsCommand: Boolean = true
    var enabledRestartCommand: Boolean = true
    var enabledGenderCommand: Boolean = true
    var enabledPlayerCommand: Boolean = true
    
    // Telegram command responses
    var onlineCommandResponse: String = "Online: %online%/%max%\nPlayers: %players%"
    var tpsCommandResponse: String = "Server TPS: %tps%"
    var restartCommandResponse: String = "⚠️ Server is restarting..."
    var genderCommandUsage: String = "Usage: /gender [man/girl]"
    var genderCommandNoPlayer: String = "You need to register your nickname first!"
    var genderCommandResponse: String = "Gender for %player% set to %gender%"
    var playerCommandUsage: String = "Usage: /player <nickname>"
    var playerCommandNoPlayer: String = "Player %player% not found"
    var playerCommandResponse: String = "Player: %player%\nOnline: %online%\nHealth: %health%\nGender: %gender%\nRegistered: %registered%\nFirst played: %first_played%\nDeaths: %deaths%\nLevel: %level%\nBalance: %balance%\nCoordinates: %coords%"
    
    // Новая команда для вывода списка команд
    var enabledCommandsListCommand: Boolean = true
    var commandsListResponse: String = """
        <gradient:#0052CC:#45B6FE>Доступные команды:</gradient>
        
        <gradient:#4CAF50:#8BC34A>• /online, /онлайн</gradient> - показать список игроков онлайн
        <gradient:#4CAF50:#8BC34A>• /tps, /тпс</gradient> - показать TPS сервера
        <gradient:#4CAF50:#8BC34A>• /restart, /рестарт</gradient> - перезапустить сервер
        <gradient:#4CAF50:#8BC34A>• /gender [man/girl], /пол [м/ж]</gradient> - установить свой пол
        <gradient:#4CAF50:#8BC34A>• /player [nickname], /ник [никнейм]</gradient> - информация об игроке
        <gradient:#4CAF50:#8BC34A>• /cmd, /команды</gradient> - показать список всех команд
        <gradient:#4CAF50:#8BC34A>• /game [nickname], /игра [никнейм]</gradient> - сыграть в игру "Угадай слово"
        
        <gradient:#FF9800:#FFEB3B>Команды доступны только в следующих каналах:</gradient>
        • Основной канал: все команды
        • Канал для регистрации: только имя пользователя
        • Консольный канал: любые серверные команды
    """
    
    // Gender translations
    var genderTranslations: Map<String, String> = mapOf(
        "man" to "Мужчина",
        "girl" to "Женщина"
    )
    
    // Status translations
    var statusTranslations: Map<String, String> = mapOf(
        "online" to "Онлайн",
        "offline" to "Оффлайн",
        "not_set" to "Не указано",
        "not_registered" to "Не зарегистрирован",
        "never" to "Никогда"
    )
    
    // Console channel settings
    var consoleChannelEnabled: Boolean = true
    var playerCommandLogEnabled: Boolean = true
    var playerCommandLogFormat: String = "[%time%] %player% executed: %command%"
    var consoleCommandFeedbackEnabled: Boolean = true
    var consoleCommandFeedback: String = "✅ Command executed: %command%"
    var consoleCommandError: String = "❌ Command failed: %command%\nError: %error%"
    
    // Register channel settings
    var registerChannelEnabled: Boolean = true
    var registerInvalidUsername: String = "❌ Invalid username: %player%"
    var registerAlreadyRegistered: String = "❌ Player %player% is already registered"
    var registerPlayerOffline: String = "❌ Player %player% is not online"
    var registerSuccess: String = "✅ Successfully registered player %player%"
    var registerSuccessInGame: String = "§a✅ Your account has been linked to Telegram!"
    var registerRewardCommands: List<String> = listOf("eco give %player% 50")
    
    // Plugin settings
    var pluginPrefix: String = "§b[ZTelegram]§r"
    var telegramLink: String = "https://t.me/ReZoobastik"
    var telegramCommandMessage: String = "<gradient:#FF0000:#A6EB0F>〔Телеграм〕</gradient> <hover:show_text:\"Кликни, чтобы открыть канал\"><gradient:#A6EB0F:#00FF00>Присоединяйтесь к нашему Telegram каналу!</gradient></hover>"
    
    // Files
    private val playersFile = File(plugin.dataFolder, "players.yml")
    private lateinit var playersConfig: YamlConfiguration
    
    init {
        plugin.saveDefaultConfig()
        if (!playersFile.exists()) {
            plugin.saveResource("players.yml", false)
        }
        
        playersConfig = YamlConfiguration.loadConfiguration(playersFile)
        loadConfig()
    }
    
    fun reload() {
        plugin.reloadConfig()
        playersConfig = YamlConfiguration.loadConfiguration(playersFile)
        loadConfig()
    }
    
    fun getPlayersConfig(): YamlConfiguration {
        return playersConfig
    }
    
    fun savePlayersConfig() {
        try {
            playersConfig.save(playersFile)
        } catch (e: Exception) {
            plugin.logger.severe("Could not save players.yml: ${e.message}")
        }
    }
    
    // Получение перевода для gender
    fun getGenderTranslation(gender: String): String {
        return genderTranslations[gender.lowercase()] ?: gender
    }
    
    // Получение перевода для статуса
    fun getStatusTranslation(status: String): String {
        val key = status.lowercase().replace(" ", "_")
        return statusTranslations[key] ?: status
    }
    
    private fun loadConfig() {
        val conf = plugin.config
        
        // Bot settings
        botToken = conf.getString("bot.token", "") ?: ""
        
        // Channel IDs
        mainChannelId = conf.getString("channels.main", "-1002111043217") ?: "-1002111043217"
        consoleChannelId = conf.getString("channels.console", "-1002656200279") ?: "-1002656200279"
        registerChannelId = conf.getString("channels.register", "-1002611802353") ?: "-1002611802353"
        
        // Main channel settings
        mainChannelEnabled = conf.getBoolean("main-channel.enabled", true)
        mainChannelChatEnabled = conf.getBoolean("main-channel.chat-enabled", true)
        formatTelegramToMinecraft = conf.getString("main-channel.format-telegram-to-minecraft", formatTelegramToMinecraft) ?: formatTelegramToMinecraft
        formatMinecraftToTelegram = conf.getString("main-channel.format-minecraft-to-telegram", formatMinecraftToTelegram) ?: formatMinecraftToTelegram
        
        // Server events
        serverStartEnabled = conf.getBoolean("events.server-start.enabled", true)
        serverStopEnabled = conf.getBoolean("events.server-stop.enabled", true)
        serverStartMessage = conf.getString("events.server-start.message", serverStartMessage) ?: serverStartMessage
        serverStopMessage = conf.getString("events.server-stop.message", serverStopMessage) ?: serverStopMessage
        
        // Player events
        playerJoinEnabled = conf.getBoolean("events.player-join.enabled", true)
        playerQuitEnabled = conf.getBoolean("events.player-quit.enabled", true)
        playerDeathEnabled = conf.getBoolean("events.player-death.enabled", true)
        playerChatEnabled = conf.getBoolean("events.player-chat.enabled", true)
        playerJoinMessage = conf.getString("events.player-join.message", playerJoinMessage) ?: playerJoinMessage
        playerQuitMessage = conf.getString("events.player-quit.message", playerQuitMessage) ?: playerQuitMessage
        playerDeathMessage = conf.getString("events.player-death.message", playerDeathMessage) ?: playerDeathMessage
        
        // Telegram commands
        enabledOnlineCommand = conf.getBoolean("commands.online.enabled", true)
        enabledTpsCommand = conf.getBoolean("commands.tps.enabled", true)
        enabledRestartCommand = conf.getBoolean("commands.restart.enabled", true)
        enabledGenderCommand = conf.getBoolean("commands.gender.enabled", true)
        enabledPlayerCommand = conf.getBoolean("commands.player.enabled", true)
        
        // Telegram command responses
        onlineCommandResponse = conf.getString("commands.online.response", onlineCommandResponse) ?: onlineCommandResponse
        tpsCommandResponse = conf.getString("commands.tps.response", tpsCommandResponse) ?: tpsCommandResponse
        restartCommandResponse = conf.getString("commands.restart.response", restartCommandResponse) ?: restartCommandResponse
        genderCommandUsage = conf.getString("commands.gender.usage", genderCommandUsage) ?: genderCommandUsage
        genderCommandNoPlayer = conf.getString("commands.gender.no-player", genderCommandNoPlayer) ?: genderCommandNoPlayer
        genderCommandResponse = conf.getString("commands.gender.response", genderCommandResponse) ?: genderCommandResponse
        playerCommandUsage = conf.getString("commands.player.usage", playerCommandUsage) ?: playerCommandUsage
        playerCommandNoPlayer = conf.getString("commands.player.no-player", playerCommandNoPlayer) ?: playerCommandNoPlayer
        playerCommandResponse = conf.getString("commands.player.response", playerCommandResponse) ?: playerCommandResponse
        
        // Добавляем загрузку новой команды списка команд
        enabledCommandsListCommand = conf.getBoolean("commands.cmd_list.enabled", true)
        commandsListResponse = conf.getString("commands.cmd_list.response", commandsListResponse) ?: commandsListResponse
        
        // Загружаем переводы для gender если они есть в конфиге
        val genderTranslationsSection = conf.getConfigurationSection("commands.gender.translations")
        if (genderTranslationsSection != null) {
            val translations = mutableMapOf<String, String>()
            for (key in genderTranslationsSection.getKeys(false)) {
                val translation = genderTranslationsSection.getString(key)
                if (translation != null) {
                    translations[key] = translation
                }
            }
            if (translations.isNotEmpty()) {
                genderTranslations = translations
            }
        }
        
        // Загружаем переводы для статусов если они есть в конфиге
        val statusTranslationsSection = conf.getConfigurationSection("commands.player.translations")
        if (statusTranslationsSection != null) {
            val translations = mutableMapOf<String, String>()
            for (key in statusTranslationsSection.getKeys(false)) {
                val translation = statusTranslationsSection.getString(key)
                if (translation != null) {
                    translations[key] = translation
                }
            }
            if (translations.isNotEmpty()) {
                statusTranslations = translations
            }
        }
        
        // Console channel settings
        consoleChannelEnabled = conf.getBoolean("console-channel.enabled", true)
        playerCommandLogEnabled = conf.getBoolean("console-channel.player-command-log.enabled", true)
        playerCommandLogFormat = conf.getString("console-channel.player-command-log.format", playerCommandLogFormat) ?: playerCommandLogFormat
        consoleCommandFeedbackEnabled = conf.getBoolean("console-channel.command-feedback.enabled", true)
        consoleCommandFeedback = conf.getString("console-channel.command-feedback.success", consoleCommandFeedback) ?: consoleCommandFeedback
        consoleCommandError = conf.getString("console-channel.command-feedback.error", consoleCommandError) ?: consoleCommandError
        
        // Register channel settings
        registerChannelEnabled = conf.getBoolean("register-channel.enabled", true)
        registerInvalidUsername = conf.getString("register-channel.invalid-username", registerInvalidUsername) ?: registerInvalidUsername
        registerAlreadyRegistered = conf.getString("register-channel.already-registered", registerAlreadyRegistered) ?: registerAlreadyRegistered
        registerPlayerOffline = conf.getString("register-channel.player-offline", registerPlayerOffline) ?: registerPlayerOffline
        registerSuccess = conf.getString("register-channel.success", registerSuccess) ?: registerSuccess
        registerSuccessInGame = conf.getString("register-channel.success-in-game", registerSuccessInGame) ?: registerSuccessInGame
        
        // Загружаем команды для наград вместо фиксированной суммы
        registerRewardCommands = conf.getStringList("register-channel.reward-commands")
        if (registerRewardCommands.isEmpty()) {
            registerRewardCommands = listOf("eco give %player% 50")
        }
        
        // Plugin settings
        pluginPrefix = conf.getString("plugin.prefix", pluginPrefix) ?: pluginPrefix
        telegramLink = conf.getString("plugin.telegram-link", telegramLink) ?: telegramLink
        telegramCommandMessage = conf.getString("plugin.telegram-command-message", telegramCommandMessage) ?: telegramCommandMessage
    }
} 