package org.zoobastiks.ztelegram.mgr

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.zoobastiks.ztelegram.ZTele
import org.zoobastiks.ztelegram.conf.TConf
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PMgr(private val plugin: ZTele) {
    private val conf: TConf
        get() = ZTele.conf
    
    private val playersConfig: YamlConfiguration
        get() = conf.getPlayersConfig()
    
    private val hiddenPlayers = mutableSetOf<String>()
    private val registeredPlayers = mutableMapOf<String, PlayerData>()
    
    init {
        loadPlayers()
    }
    
    fun reload() {
        hiddenPlayers.clear()
        registeredPlayers.clear()
        loadPlayers()
    }
    
    private fun loadPlayers() {
        val config = ZTele.conf.getPlayersConfig()
        
        // Load hidden players
        val hiddenList = config.getStringList("hidden-players")
        hiddenPlayers.addAll(hiddenList)
        
        // Load registered players
        val playersSection = config.getConfigurationSection("players")
        if (playersSection != null) {
            for (playerName in playersSection.getKeys(false)) {
                val playerSection = playersSection.getConfigurationSection(playerName)
                if (playerSection != null) {
                    val telegramId = playerSection.getString("telegram-id") ?: continue
                    val registered = playerSection.getString("registered")
                    val gender = playerSection.getString("gender")
                    
                    registeredPlayers[playerName] = PlayerData(telegramId, registered, gender)
                }
            }
        }
    }
    
    fun isPlayerHidden(name: String): Boolean {
        val hiddenPlayers = playersConfig.getStringList("hidden-players")
        return hiddenPlayers.contains(name.lowercase())
    }
    
    fun addHiddenPlayer(name: String) {
        val hiddenPlayers = playersConfig.getStringList("hidden-players").toMutableList()
        
        hiddenPlayers.add(name.lowercase())
        playersConfig.set("hidden-players", hiddenPlayers)
        conf.savePlayersConfig()
    }
    
    fun removeHiddenPlayer(name: String) {
        val hiddenPlayers = playersConfig.getStringList("hidden-players").toMutableList()
        
        hiddenPlayers.remove(name.lowercase())
        playersConfig.set("hidden-players", hiddenPlayers)
        conf.savePlayersConfig()
    }
    
    // Метод для получения списка скрытых игроков
    fun getHiddenPlayers(): List<String> {
        return playersConfig.getStringList("hidden-players")
    }
    
    fun isPlayerRegistered(playerName: String): Boolean {
        return registeredPlayers.containsKey(playerName.lowercase())
    }
    
    fun registerPlayer(playerName: String, telegramId: String): Boolean {
        val name = playerName.lowercase()
        if (registeredPlayers.containsKey(name)) {
            return false
        }
        
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val registered = LocalDateTime.now().format(dateFormat)
        
        registeredPlayers[name] = PlayerData(telegramId, registered, null)
        savePlayers()
        return true
    }
    
    fun unregisterPlayer(playerName: String): Boolean {
        val name = playerName.lowercase()
        if (!registeredPlayers.containsKey(name)) {
            return false
        }
        
        registeredPlayers.remove(name)
        savePlayers()
        return true
    }
    
    fun getPlayerData(playerName: String): PlayerData? {
        return registeredPlayers[playerName.lowercase()]
    }
    
    fun getPlayerByTelegramId(telegramId: String): String? {
        for ((player, data) in registeredPlayers) {
            if (data.telegramId == telegramId) {
                return player
            }
        }
        return null
    }
    
    fun setPlayerGender(playerName: String, gender: String): Boolean {
        val name = playerName.lowercase()
        val playerData = registeredPlayers[name] ?: return false
        
        registeredPlayers[name] = playerData.copy(gender = gender)
        savePlayers()
        return true
    }
    
    private fun savePlayers() {
        val config = ZTele.conf.getPlayersConfig()
        
        // Save hidden players
        config.set("hidden-players", hiddenPlayers.toList())
        
        // Save registered players
        config.set("players", null) // Clear existing players
        
        for ((playerName, data) in registeredPlayers) {
            val path = "players.$playerName"
            config.set("$path.telegram-id", data.telegramId)
            config.set("$path.registered", data.registered)
            config.set("$path.gender", data.gender)
        }
        
        ZTele.conf.savePlayersConfig()
    }
    
    data class PlayerData(
        val telegramId: String,
        val registered: String? = null,
        val gender: String? = null
    )
} 