package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import com.example.R
import java.io.File
import java.io.FileOutputStream

enum class CoverType {
    PRESET,
    CUSTOM
}

data class CoverPreset(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val resId: Int
)

data class CoverConfig(
    val type: CoverType = CoverType.PRESET,
    @DrawableRes val presetResId: Int = R.drawable.img_sst_cover_banner_1786993083781,
    val customFilePath: String? = null
)

object CoverImageManager {

    private const val PREFS_NAME = "sst_cover_prefs"
    private const val KEY_COVER_TYPE = "cover_type"
    private const val KEY_PRESET_RES_ID = "preset_res_id"
    private const val KEY_CUSTOM_FILE_PATH = "custom_file_path"

    val DEFAULT_PRESET_RES_ID = R.drawable.img_sst_cover_banner_1786993083781

    val AVAILABLE_PRESETS = listOf(
        CoverPreset(
            id = "preset_sst_banner_official",
            title = "Padrão SST / Relato de Segurança",
            description = "Banner oficial com profissionais em conformidade com as normas SST.",
            resId = R.drawable.img_sst_cover_banner_1786993083781
        ),
        CoverPreset(
            id = "preset_foco_prevencao_logo",
            title = "Identidade Foco na Prevenção",
            description = "Logotipo institucional de Segurança do Trabalho e Prevenção Ativa.",
            resId = R.drawable.img_foco_prevencao_logo_1787517691154
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCoverConfig(context: Context): CoverConfig {
        val prefs = getPrefs(context)
        val typeStr = prefs.getString(KEY_COVER_TYPE, CoverType.PRESET.name) ?: CoverType.PRESET.name
        val type = try {
            CoverType.valueOf(typeStr)
        } catch (e: Exception) {
            CoverType.PRESET
        }
        val presetResId = prefs.getInt(KEY_PRESET_RES_ID, DEFAULT_PRESET_RES_ID)
        val customFilePath = prefs.getString(KEY_CUSTOM_FILE_PATH, null)

        // Validate custom file exists
        if (type == CoverType.CUSTOM) {
            if (customFilePath.isNullOrBlank() || !File(customFilePath).exists()) {
                return CoverConfig(type = CoverType.PRESET, presetResId = DEFAULT_PRESET_RES_ID)
            }
        }

        return CoverConfig(
            type = type,
            presetResId = if (presetResId != 0) presetResId else DEFAULT_PRESET_RES_ID,
            customFilePath = customFilePath
        )
    }

    fun savePresetCover(context: Context, @DrawableRes resId: Int): CoverConfig {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_COVER_TYPE, CoverType.PRESET.name)
            .putInt(KEY_PRESET_RES_ID, resId)
            .apply()

        return CoverConfig(type = CoverType.PRESET, presetResId = resId)
    }

    fun saveCustomCoverFromUri(context: Context, uri: Uri): CoverConfig? {
        return try {
            val coverDir = File(context.filesDir, "app_cover")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
            }
            val destinationFile = File(coverDir, "persistent_cover.jpg")

            val uriStr = uri.toString()
            val isLocalFile = uriStr.startsWith("/") || uri.scheme == "file" || uri.scheme == null
            val localPath = if (uri.scheme == "file") uri.path ?: uriStr else uriStr
            val localFile = if (isLocalFile) File(localPath) else null

            val inputStream = if (localFile != null && localFile.exists()) {
                localFile.inputStream()
            } else {
                try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    null
                }
            } ?: return null

            inputStream.use { input ->
                FileOutputStream(destinationFile, false).use { output ->
                    input.copyTo(output)
                }
            }

            val prefs = getPrefs(context)
            prefs.edit()
                .putString(KEY_COVER_TYPE, CoverType.CUSTOM.name)
                .putString(KEY_CUSTOM_FILE_PATH, destinationFile.absolutePath)
                .apply()

            CoverConfig(
                type = CoverType.CUSTOM,
                presetResId = DEFAULT_PRESET_RES_ID,
                customFilePath = destinationFile.absolutePath
            )
        } catch (e: Exception) {
            Log.e("CoverImageManager", "Erro ao salvar imagem de capa customizada: ${e.message}")
            null
        }
    }

    fun resetToDefault(context: Context): CoverConfig {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
        return CoverConfig(
            type = CoverType.PRESET,
            presetResId = DEFAULT_PRESET_RES_ID,
            customFilePath = null
        )
    }
}
