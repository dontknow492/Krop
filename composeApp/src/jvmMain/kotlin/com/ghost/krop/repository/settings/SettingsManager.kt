package com.ghost.krop.repository.settings

import com.ghost.krop.utils.AppDirs
import io.github.aakira.napier.Napier
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class SettingsManager(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    // Make configurable for testing
    private val settingsFile: File by lazy {
        val folder = AppDirs.configDir

        if (!folder.exists()) {
            folder.mkdirs()
            Napier.i("Created config directory: ${folder.absolutePath}")
        }

        File(folder, "settings.json").also {
            Napier.i("Settings file: ${it.absolutePath}")
        }
    }

    /**
     * Save settings with validation
     * @return Result indicating success/failure
     */
    fun saveSettings(settings: AppSettings): Result<Unit> = runCatching {
        val cleanSettings = SettingsValidator.validateAll(settings)

        try {
            val jsonString = json.encodeToString(cleanSettings)
            settingsFile.writeText(jsonString)
            Napier.i("✅ Settings saved successfully (${settingsFile.length()} bytes)")
        } catch (e: IOException) {
            Napier.e("Failed to write settings file", e)
            throw e
        } catch (e: SerializationException) {
            Napier.e("Failed to serialize settings", e)
            throw e
        }
    }.onFailure { error ->
        Napier.e("❌ Failed to save settings", error)
    }

    /**
     * Save settings (blocking version for simple use)
     */
    fun saveSettingsBlocking(settings: AppSettings): Boolean =
        saveSettings(settings).isSuccess

    /**
     * Load settings with better error context
     */
    fun loadSettings(): AppSettings {
        return try {
            if (!settingsFile.exists()) {
                Napier.i("No settings file found, using defaults")
                return AppSettings()
            }

            val jsonString = settingsFile.readText()

            if (jsonString.isBlank()) {
                Napier.w("Settings file is empty, using defaults")
                return AppSettings()
            }

            val loadedSettings = json.decodeFromString<AppSettings>(jsonString)

            // Validate before returning
            val validatedSettings = SettingsValidator.validateAll(loadedSettings)

            // Check if validation changed anything
            if (validatedSettings != loadedSettings) {
                Napier.i("Settings validated and corrected")
                // Optionally save the corrected settings
                saveSettingsBlocking(validatedSettings)
            }

            Napier.i("✅ Settings loaded successfully (${jsonString.length} chars)")
            validatedSettings

        } catch (e: IOException) {
            Napier.e("Failed to read settings file", e)
            AppSettings()
        } catch (e: SerializationException) {
            Napier.e("Failed to parse settings JSON", e)
            // Backup corrupted file?
            backupCorruptedFile()
            AppSettings()
        } catch (e: Exception) {
            Napier.e("Unexpected error loading settings", e)
            AppSettings()
        }
    }

    /**
     * Reset settings to defaults
     */
    fun resetToDefaults(): AppSettings {
        val defaults = AppSettings()
        saveSettingsBlocking(defaults)
        Napier.i("🔄 Settings reset to defaults")
        return defaults
    }

    /**
     * Export settings to a custom location
     */
    fun exportSettings(targetFile: File): Result<Unit> = runCatching {
        if (settingsFile.exists()) {
            settingsFile.copyTo(targetFile, overwrite = true)
            Napier.i("📤 Settings exported to ${targetFile.absolutePath}")
        } else {
            // Create default settings file at target
            val defaults = AppSettings()
            val jsonString = json.encodeToString(defaults)
            targetFile.writeText(jsonString)
            Napier.i("📤 Default settings exported to ${targetFile.absolutePath}")
        }
    }

    /**
     * Import settings from a custom location
     */
    fun importSettings(sourceFile: File): Result<AppSettings> = runCatching {
        require(sourceFile.exists()) { "Source file does not exist" }
        require(sourceFile.canRead()) { "Cannot read source file" }

        val jsonString = sourceFile.readText()
        val importedSettings = json.decodeFromString<AppSettings>(jsonString)
        val validatedSettings = SettingsValidator.validateAll(importedSettings)

        // Save to main settings file
        saveSettingsBlocking(validatedSettings)

        Napier.i("📥 Settings imported from ${sourceFile.absolutePath}")
        validatedSettings
    }

    /**
     * Check if settings file exists
     */
    fun hasSettingsFile(): Boolean = settingsFile.exists()

    /**
     * Get settings file info
     */
    fun getSettingsFileInfo(): FileInfo? {
        return if (settingsFile.exists()) {
            FileInfo(
                path = settingsFile.absolutePath,
                size = settingsFile.length(),
                lastModified = settingsFile.lastModified()
            )
        } else null
    }

    /**
     * Delete settings file (use with caution!)
     */
    fun deleteSettingsFile(): Boolean {
        return if (settingsFile.exists()) {
            val deleted = settingsFile.delete()
            if (deleted) {
                Napier.w("🗑️ Settings file deleted")
            }
            deleted
        } else {
            true // Already doesn't exist
        }
    }

    // Helper to backup corrupted files
    private fun backupCorruptedFile() {
        try {
            if (settingsFile.exists()) {
                val backupFile = File(settingsFile.parent, "settings.json.corrupted.${System.currentTimeMillis()}")
                settingsFile.copyTo(backupFile, overwrite = true)
                Napier.i("💾 Corrupted settings backed up to ${backupFile.absolutePath}")
            }
        } catch (e: Exception) {
            Napier.e("Failed to backup corrupted file", e)
        }
    }

    // Data class for file info
    data class FileInfo(
        val path: String,
        val size: Long,
        val lastModified: Long
    )
}