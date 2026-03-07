package com.ghost.krop.repository.settings

import com.ghost.krop.models.AppDirs
import kotlinx.serialization.json.Json
import java.io.File

class SettingsManager {

    // 🔐 INTERNAL SECRET KEY
    // This makes the obfuscation unique to your app.
    // Even if someone base64 decodes the string, they get garbage data without this.

    // 1. Create a pretty-printing JSON engine
    private val json = Json {
        prettyPrint = true          // Makes the file readable in Notepad
        ignoreUnknownKeys = true    // Prevents crashing if you add features later
        encodeDefaults = true       // Saves default values to the file
    }

    // 2. Locate the  AppData folder
    private val settingsFile: File by lazy {
        val folder = AppDirs.configDir

        if (!folder.exists()) folder.mkdirs()
        File(folder, "settings.json")
    }

    // 3. The "Save" function
    fun saveSettings(settings: AppSettings) {
        // Validate everything before it hits the disk
        val cleanSettings = SettingsValidator.validateAll(settings)

        try {
            val jsonString = json.encodeToString(cleanSettings)
            settingsFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 4. The "Load" function
    fun loadSettings(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                val loadedSettings = json.decodeFromString<AppSettings>(jsonString)

                // Validate the loaded data before returning it to the UI
                SettingsValidator.validateAll(loadedSettings)

            } else {
                AppSettings() // Return defaults if file doesn't exist
            }
        } catch (e: Exception) {
            // If the JSON is so broken it can't even be parsed,
            // we log it and return the safe factory defaults.
            println("Error loading settings: ${e.message}")
            AppSettings()
        }
    }
}




