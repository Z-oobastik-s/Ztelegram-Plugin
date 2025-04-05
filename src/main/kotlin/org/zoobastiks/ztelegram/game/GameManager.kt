package org.zoobastiks.ztelegram.game

import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.zoobastiks.ztelegram.ZTele
import java.io.File
import java.util.*
import kotlin.random.Random

class GameManager(private val plugin: ZTele) {
    private val gameFile = File(plugin.dataFolder, "game.yml")
    private lateinit var gameConfig: FileConfiguration
    
    // Настройки игры
    var enabled: Boolean = true
    var gameCommandEnabled: Boolean = true
    var rewardAmount: Double = 5.0
    var rewardCommands: List<String> = listOf("eco give %player% 5")
    
    // Сообщения
    var gameStartMessage: String = "🎮 Игра \"Угадай слово\" началась!\n🎯 Угадайте слово: %word%\n⏱️ У вас есть %time% секунд!"
    var gameWinMessage: String = "🎉 Поздравляем! Вы угадали слово: %word%\n🎁 Вы получили %reward% монет!"
    var gameLoseMessage: String = "😢 Время вышло! Правильное слово: %word%\n🔄 Используйте /game для новой игры."
    var gameAlreadyPlayingMessage: String = "❌ Вы уже играете! Сначала закончите текущую игру."
    var gameNotRegisteredMessage: String = "❌ Вы не зарегистрированы! Сначала зарегистрируйтесь в канале регистрации."
    var gamePlayerNotFoundMessage: String = "❌ Игрок %player% не найден! Укажите правильный никнейм."
    
    // Настройки времени
    var gameTimeSeconds: Int = 60
    
    // Слова для игры
    private var wordsList: List<WordPair> = listOf()
    
    // Активные игры: telegramUsername -> GameSession
    private val activeGames = mutableMapOf<String, GameSession>()
    
    // Класс для хранения пары слов (оригинал и с пропусками)
    data class WordPair(val original: String, val masked: String)
    
    // Класс для хранения информации об активной игре
    data class GameSession(
        val playerName: String,
        val wordPair: WordPair,
        val startTime: Long,
        var taskId: Int = -1
    )
    
    init {
        if (!gameFile.exists()) {
            plugin.saveResource("game.yml", false)
        }
        
        loadConfig()
    }
    
    fun reload() {
        loadConfig()
    }
    
    private fun loadConfig() {
        try {
            gameConfig = YamlConfiguration.loadConfiguration(gameFile)
            
            // Загружаем основные настройки
            enabled = gameConfig.getBoolean("enabled", true)
            gameCommandEnabled = gameConfig.getBoolean("command.enabled", true)
            rewardAmount = gameConfig.getDouble("reward.amount", 5.0)
            rewardCommands = gameConfig.getStringList("reward.commands")
            if (rewardCommands.isEmpty()) {
                rewardCommands = listOf("eco give %player% 5")
            }
            
            // Загружаем сообщения
            gameStartMessage = gameConfig.getString("messages.game_start", gameStartMessage) ?: gameStartMessage
            gameWinMessage = gameConfig.getString("messages.game_win", gameWinMessage) ?: gameWinMessage
            gameLoseMessage = gameConfig.getString("messages.game_lose", gameLoseMessage) ?: gameLoseMessage
            gameAlreadyPlayingMessage = gameConfig.getString("messages.already_playing", gameAlreadyPlayingMessage) ?: gameAlreadyPlayingMessage
            gameNotRegisteredMessage = gameConfig.getString("messages.not_registered", gameNotRegisteredMessage) ?: gameNotRegisteredMessage
            gamePlayerNotFoundMessage = gameConfig.getString("messages.player_not_found", gamePlayerNotFoundMessage) ?: gamePlayerNotFoundMessage
            
            // Загружаем настройки времени
            gameTimeSeconds = gameConfig.getInt("settings.time_seconds", 60)
            
            // Загружаем слова для игры
            val wordsSection = gameConfig.getConfigurationSection("words")
            val wordsList = mutableListOf<WordPair>()
            
            if (wordsSection != null) {
                for (key in wordsSection.getKeys(false)) {
                    val originalWord = wordsSection.getString("$key.original")
                    val maskedWord = wordsSection.getString("$key.masked")
                    
                    if (originalWord != null && maskedWord != null) {
                        wordsList.add(WordPair(originalWord, maskedWord))
                    }
                }
            }
            
            if (wordsList.isEmpty()) {
                // Если нет слов в конфиге, добавляем стандартные
                val defaultWords = listOf(
                    WordPair("Буратино утопился", "Бу__тин_ _топ_лс_"),
                    WordPair("Колобок повесился", "К_ло_ок п_в_си_ся"),
                    WordPair("Красная шапочка", "Кр__ная ш_по_ка"),
                    WordPair("Серый волк", "С_р_й в_лк"),
                    WordPair("Minecraft сервер", "M_n_cr_ft с_рв_р"),
                    WordPair("Телеграм бот", "Т_л_гр_м б_т"),
                    WordPair("Золотой ключик", "З_л_т_й кл_ч_к"),
                    WordPair("Зеленый огр", "З_л_н_й о_р"),
                    WordPair("Подземелье дракона", "П_дз_м_ль_ др_к_на"),
                    WordPair("Волшебная палочка", "В_лш_бн_я п_л_чк_")
                )
                wordsList.addAll(defaultWords)
                
                // Сохраняем стандартные слова в конфиг
                saveDefaultWords(defaultWords)
            }
            
            this.wordsList = wordsList
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load game.yml: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun saveDefaultWords(words: List<WordPair>) {
        try {
            val wordsSection = gameConfig.createSection("words")
            
            for (i in words.indices) {
                val wordSection = wordsSection.createSection("word$i")
                wordSection.set("original", words[i].original)
                wordSection.set("masked", words[i].masked)
            }
            
            gameConfig.save(gameFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save default words to game.yml: ${e.message}")
        }
    }
    
    fun startGame(telegramUsername: String, playerName: String): String {
        if (!enabled) {
            return "❌ Игра временно отключена."
        }
        
        // Проверяем, не играет ли игрок уже
        if (activeGames.containsKey(telegramUsername)) {
            return gameAlreadyPlayingMessage
        }
        
        // Проверяем, существует ли игрок
        val player = Bukkit.getPlayerExact(playerName)
        if (player == null && !Bukkit.getOfflinePlayer(playerName).hasPlayedBefore()) {
            return gamePlayerNotFoundMessage.replace("%player%", playerName)
        }
        
        // Получаем случайное слово
        if (wordsList.isEmpty()) {
            return "❌ Список слов пуст. Обратитесь к администратору."
        }
        
        val randomWord = wordsList[Random.nextInt(wordsList.size)]
        
        // Создаем игровую сессию
        val gameSession = GameSession(
            playerName = playerName,
            wordPair = randomWord,
            startTime = System.currentTimeMillis()
        )
        
        // Запускаем таймер для завершения игры по таймауту
        val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            handleTimeout(telegramUsername)
        }, gameTimeSeconds * 20L).taskId
        
        gameSession.taskId = taskId
        
        // Сохраняем игровую сессию
        activeGames[telegramUsername] = gameSession
        
        // Возвращаем сообщение о начале игры
        return gameStartMessage
            .replace("%word%", randomWord.masked)
            .replace("%time%", gameTimeSeconds.toString())
            .replace("\\n", "\n")
    }
    
    fun checkAnswer(telegramUsername: String, answer: String): Pair<Boolean, String> {
        val gameSession = activeGames[telegramUsername] ?: return Pair(false, "❌ У вас нет активной игры. Напишите /game [nickname] для начала игры.")
        
        // Сравниваем ответ с правильным словом (игнорируя регистр)
        if (answer.trim().equals(gameSession.wordPair.original, ignoreCase = true)) {
            // Правильный ответ
            
            // Отменяем таймер
            if (gameSession.taskId != -1) {
                Bukkit.getScheduler().cancelTask(gameSession.taskId)
            }
            
            // Выдаем награду
            giveReward(gameSession.playerName)
            
            // Удаляем игровую сессию
            activeGames.remove(telegramUsername)
            
            // Возвращаем сообщение о победе
            return Pair(true, gameWinMessage
                .replace("%word%", gameSession.wordPair.original)
                .replace("%reward%", rewardAmount.toString())
                .replace("\\n", "\n"))
        }
        
        // Неправильный ответ
        return Pair(false, "❌ Неверный ответ! Попробуйте еще раз.\n🎯 Слово: ${gameSession.wordPair.masked}")
    }
    
    private fun handleTimeout(telegramUsername: String) {
        val gameSession = activeGames[telegramUsername] ?: return
        
        // Удаляем игровую сессию
        activeGames.remove(telegramUsername)
        
        // Отправляем сообщение о проигрыше
        val loseMessage = gameLoseMessage
            .replace("%word%", gameSession.wordPair.original)
            .replace("\\n", "\n")
        
        // Отправляем сообщение через бота
        plugin.getBot().sendToMainChannel(loseMessage)
    }
    
    private fun giveReward(playerName: String) {
        // Выполняем команды для выдачи наград
        Bukkit.getScheduler().runTask(plugin, Runnable {
            for (command in rewardCommands) {
                try {
                    val cmd = command.replace("%player%", playerName)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to execute reward command for player $playerName: ${e.message}")
                }
            }
        })
    }
    
    fun hasActiveGame(telegramUsername: String): Boolean {
        return activeGames.containsKey(telegramUsername)
    }
    
    fun getActiveGame(telegramUsername: String): GameSession? {
        return activeGames[telegramUsername]
    }
    
    fun cancelGame(telegramUsername: String): Boolean {
        val gameSession = activeGames[telegramUsername] ?: return false
        
        // Отменяем таймер
        if (gameSession.taskId != -1) {
            Bukkit.getScheduler().cancelTask(gameSession.taskId)
        }
        
        // Удаляем игровую сессию
        activeGames.remove(telegramUsername)
        
        return true
    }
    
    fun cancelAllGames() {
        // Отменяем все активные игры
        for (gameSession in activeGames.values) {
            if (gameSession.taskId != -1) {
                Bukkit.getScheduler().cancelTask(gameSession.taskId)
            }
        }
        
        // Очищаем список активных игр
        activeGames.clear()
    }
} 