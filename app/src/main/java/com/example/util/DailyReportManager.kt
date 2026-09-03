package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.model.SafetyOccurrence
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingPreviousDayReport(
    val previousDate: String,
    val count: Int
)

object DailyReportManager {

    private const val PREFS_NAME = "sst_daily_report_prefs"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getReportsDir(context: Context): File {
        val dir = File(context.filesDir, "relatorios_diarios")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPhotosDir(context: Context): File {
        val dir = File(getReportsDir(context), "fotos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Records an occurrence inside the internal daily text log file,
     * inserting each new report below the previous ones without erasing earlier entries.
     */
    fun appendOccurrenceToDailyFile(context: Context, occurrence: SafetyOccurrence) {
        try {
            val reportsDir = getReportsDir(context)
            val dateSanitized = occurrence.data.replace("/", "-")
            val dailyFile = File(reportsDir, "relatorio_diario_$dateSanitized.txt")

            // Copy photo to internal app folder if available
            var localPhotoPath: String? = null
            if (!occurrence.fotoUri.isNullOrBlank()) {
                localPhotoPath = copyPhotoToInternalFolder(context, occurrence)
            }

            val entryText = buildString {
                appendLine("==================================================")
                appendLine("REGISTRO DE OCORRÊNCIA SST - [${occurrence.hora}]")
                appendLine("--------------------------------------------------")
                appendLine("Data/Hora: ${occurrence.data} às ${occurrence.hora}")
                appendLine("Colaborador: ${occurrence.nomeColaborador.ifBlank { "Não informado" }} (RE: ${occurrence.registro.ifBlank { "-" }})")
                appendLine("Setor: ${occurrence.setor.ifBlank { "-" }} | Local: ${occurrence.local.ifBlank { "Não informado" }}")
                appendLine("Classificação: ${occurrence.classificacao} | Risco: ${occurrence.risco.uppercase()}")
                appendLine("Tipo: ${occurrence.ocorrencia} | Causa: ${occurrence.causa} | Clima: ${occurrence.clima}")
                appendLine("RELATO E DETALHES:")
                appendLine(occurrence.relatoDetalhes.ifBlank { "(Nenhum relato informado)" })
                appendLine("AÇÃO TOMADA:")
                appendLine(occurrence.acaoTomada.ifBlank { "(Nenhuma ação informada)" })
                if (localPhotoPath != null) {
                    appendLine("ANEXO FOTO INTERNA: $localPhotoPath")
                } else if (!occurrence.fotoUri.isNullOrBlank()) {
                    appendLine("ANEXO FOTO URI: ${occurrence.fotoUri}")
                } else {
                    appendLine("ANEXO FOTO: Nenhuma foto anexada")
                }
                appendLine("==================================================")
                appendLine()
            }

            FileOutputStream(dailyFile, true).use { out ->
                out.write(entryText.toByteArray(Charsets.UTF_8))
            }

            // Update last active date to today
            getPrefs(context).edit().putString(KEY_LAST_ACTIVE_DATE, occurrence.data).apply()

            Log.d("DailyReportManager", "Ocorrência salva com sucesso no arquivo diário acumulativo: ${dailyFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("DailyReportManager", "Erro ao salvar ocorrência em arquivo interno: ${e.message}", e)
        }
    }

    private fun copyPhotoToInternalFolder(context: Context, occurrence: SafetyOccurrence): String? {
        val uriStr = occurrence.fotoUri ?: return null
        return try {
            val photosDir = getPhotosDir(context)
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val destFile = File(photosDir, "foto_${occurrence.data.replace("/", "")}_${occurrence.hora.replace(":", "")}_$timestamp.jpg")

            val uri = Uri.parse(uriStr)
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
            }

            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists()) destFile.absolutePath else null
        } catch (e: Exception) {
            Log.e("DailyReportManager", "Erro ao copiar foto para pasta de arquivos interna: ${e.message}")
            null
        }
    }

    /**
     * Checks if a previous day date exists with saved occurrences that were not yet exported/cleared.
     */
    fun checkPreviousDayPendingReport(context: Context, occurrences: List<SafetyOccurrence>): PendingPreviousDayReport? {
        val prefs = getPrefs(context)
        val lastActiveDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null) ?: return null
        val today = getTodayDate()

        if (lastActiveDate != today) {
            val previousDayOccurrences = occurrences.filter { it.data == lastActiveDate }
            if (previousDayOccurrences.isNotEmpty()) {
                return PendingPreviousDayReport(
                    previousDate = lastActiveDate,
                    count = previousDayOccurrences.size
                )
            } else {
                // No occurrences from previous date, update last active date to today quietly
                prefs.edit().putString(KEY_LAST_ACTIVE_DATE, today).apply()
            }
        }
        return null
    }

    /**
     * Clears internal log files and photos for the specified date or clears all daily folders
     * to prepare a fresh internal workspace for the new day.
     */
    fun clearDailyFolderAndAdvanceDate(context: Context, targetDate: String) {
        try {
            val reportsDir = getReportsDir(context)
            val dateSanitized = targetDate.replace("/", "-")
            val dailyTxt = File(reportsDir, "relatorio_diario_$dateSanitized.txt")

            if (dailyTxt.exists()) {
                dailyTxt.delete()
            }

            // Update last active date to today
            getPrefs(context).edit().putString(KEY_LAST_ACTIVE_DATE, getTodayDate()).apply()

            Log.d("DailyReportManager", "Pasta e arquivos do dia $targetDate zerados para novo dia ${getTodayDate()}")
        } catch (e: Exception) {
            Log.e("DailyReportManager", "Erro ao zerar pasta do dia anterior: ${e.message}")
        }
    }
}
