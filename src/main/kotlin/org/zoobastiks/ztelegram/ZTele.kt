package org.zoobastiks.ztelegram

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.zoobastiks.ztelegram.bot.TBot
import org.zoobastiks.ztelegram.cmd.TCmds
import org.zoobastiks.ztelegram.conf.TConf
import org.zoobastiks.ztelegram.lis.TLis
import org.zoobastiks.ztelegram.mgr.PMgr
import org.zoobastiks.ztelegram.game.GameManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ZTele : JavaPlugin() {
    companion object {
        lateinit var instance: ZTele
            private set
        lateinit var conf: TConf
            private set
        lateinit var bot: TBot
            private set
        lateinit var mgr: PMgr
            private set
        lateinit var game: GameManager
            private set
        private var reconnectScheduler: ScheduledExecutorService? = null
        private val reconnectDelays = arrayOf(30L, 60L, 300L, 600L, 1800L)
        private var reconnectAttempt = 0
    }

    private var reconnectTask: Int = -1

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        conf = TConf(this)
        mgr = PMgr(this)
        game = GameManager(this)
        
        registerCommands()
        registerListeners()
        startBot()
        
        // Отправка сообщения о запуске сервера после полной загрузки сервера и миров
        server.scheduler.runTaskLater(this, Runnable {
            bot.sendServerStartMessage()
        }, 100L) // 5 секунд (100 тиков) после запуска сервера
        
        logger.info("§a${description.name} v${description.version} enabled!")
    }

    override fun onDisable() {
        bot.sendServerStopMessage()
        bot.stop()
        stopReconnectScheduler()
        game.cancelAllGames()
        logger.info("§c${description.name} v${description.version} disabled!")
    }
    
    fun reload() {
        reloadConfig()
        conf.reload()
        mgr.reload()
        
        // Останавливаем бот и ждем немного для корректного завершения сессии
        bot.stop()
        
        // Добавляем небольшую задержку перед повторным запуском, чтобы сессия успела корректно закрыться
        server.scheduler.runTaskLater(this, Runnable {
            // Попытка остановить бота еще раз перед запуском для гарантии
            try {
                bot.stop()
            } catch (e: Exception) {
                // Игнорируем ошибки
            }
            
            // Запускаем сборщик мусора для освобождения ресурсов
            System.gc()
            
            // Запускаем новый экземпляр бота
            server.scheduler.runTaskLater(this, Runnable {
                startBot()
            }, 40L) // Еще 2 секунды ожидания
        }, 60L) // 3 секунды ожидания
    }
    
    private fun registerCommands() {
        val cmdExecutor = TCmds(this)
        val telegramCmd = getCommand("telegram")
        if (telegramCmd != null) {
            telegramCmd.setExecutor(cmdExecutor)
            telegramCmd.tabCompleter = cmdExecutor
        }
    }
    
    private fun registerListeners() {
        server.pluginManager.registerEvents(TLis(this), this)
    }
    
    private fun startBot() {
        stopReconnectScheduler()
        reconnectAttempt = 0
        
        try {
            bot = TBot(this)
            bot.start()
        } catch (e: Exception) {
            logger.severe("Failed to start Telegram bot: ${e.message}")
            scheduleReconnect()
        }
    }
    
    fun scheduleReconnect() {
        stopReconnectScheduler()
        
        reconnectAttempt++
        val delaySeconds = if (reconnectAttempt > 5) 60 else 30
        
        logger.info("Scheduling bot reconnect attempt in $delaySeconds seconds...")
        
        reconnectTask = server.scheduler.scheduleSyncDelayedTask(this, Runnable {
            logger.info("Attempting to reconnect Telegram bot (attempt $reconnectAttempt)...")
            startBot()
        }, delaySeconds * 20L)
    }
    
    private fun stopReconnectScheduler() {
        if (reconnectTask != -1) {
            server.scheduler.cancelTask(reconnectTask)
            reconnectTask = -1
        }
    }
    
    // Добавляем метод для перезагрузки конфигурации игры
    fun reloadGame() {
        game.reload()
        logger.info("Game configuration reloaded!")
    }

    // Метод для доступа к боту из других классов
    fun getBot(): TBot {
        return bot
    }
} 