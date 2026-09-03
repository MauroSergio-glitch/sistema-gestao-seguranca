package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.SafetyOccurrence
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages CSV / Excel Spreadsheet generation, export, and PDF/CSV backup storage.
 */
object SpreadsheetReportManager {

    fun getSpreadsheetDir(context: Context): File {
        val dir = File(context.filesDir, "relatorios_diarios/planilha")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, "relatorios_diarios/backup")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getSpreadsheetFile(context: Context): File {
        return File(getSpreadsheetDir(context), "Planilha_Ocorrencias_SST.csv")
    }

    const val CSV_HEADER = "ID;Data;Hora;Matrícula/RE;Colaborador;Setor;Local;Grau de Risco;Tipo Ocorrência;Classificação;Causa Principal;Causa Secundária / Fator Contribuinte;Condição do Clima;Relato e Detalhes;Ação Imediata / Tomada;Status da Ocorrência;Ação Preventiva Definitiva;Responsável pela Resolução;Setor Responsável;Prazo / Data Limite de Tratativa;Foto Anexa\n"

    /**
     * Resolves the user-facing status of an occurrence (Pendente, Em Tratativa, Eficaz, Concluído, etc.)
     */
    fun resolveStatus(occurrence: SafetyOccurrence): String {
        val status = occurrence.statusAcao.trim()
        val avaliacao = occurrence.avaliacaoEficacia.trim()
        return when {
            status.equals("Eficaz", ignoreCase = true) || avaliacao.equals("Eficaz", ignoreCase = true) -> "Eficaz"
            status.equals("Concluído", ignoreCase = true) || status.equals("Concluido", ignoreCase = true) -> "Concluído"
            status.equals("Em Tratativa", ignoreCase = true) -> "Em Tratativa"
            status.equals("Aguardando Validação", ignoreCase = true) || status.equals("Aguardando Validacao", ignoreCase = true) -> "Aguardando Validação"
            status.equals("Atrasado", ignoreCase = true) -> "Atrasado"
            status.equals("Cancelado", ignoreCase = true) -> "Cancelado"
            status.isNotBlank() -> status
            else -> "Pendente"
        }
    }

    /**
     * Builds a single formatted CSV row adhering to Excel and Google Sheets standards.
     */
    fun buildCsvRow(occurrence: SafetyOccurrence): String {
        val status = resolveStatus(occurrence)
        val acaoDefinitiva = if (occurrence.acaoPreventiva.isNotBlank()) {
            occurrence.acaoPreventiva
        } else if (occurrence.descricaoSolucao.isNotBlank()) {
            occurrence.descricaoSolucao
        } else {
            ""
        }
        val responsavel = occurrence.responsavelAcao
        val setorResp = occurrence.setorResponsavel.ifBlank { occurrence.setor }
        val prazo = occurrence.prazoAcao
        val causaSec = occurrence.causaSecundaria

        return buildString {
            append("${occurrence.id};")
            append("${sanitizeCsv(occurrence.data)};")
            append("${sanitizeCsv(occurrence.hora)};")
            append("${sanitizeCsv(occurrence.registro)};")
            append("${sanitizeCsv(occurrence.nomeColaborador)};")
            append("${sanitizeCsv(occurrence.setor)};")
            append("${sanitizeCsv(occurrence.local)};")
            append("${sanitizeCsv(occurrence.risco)};")
            append("${sanitizeCsv(occurrence.ocorrencia)};")
            append("${sanitizeCsv(occurrence.classificacao)};")
            append("${sanitizeCsv(occurrence.causa)};")
            append("${sanitizeCsv(causaSec)};")
            append("${sanitizeCsv(occurrence.clima)};")
            append("${sanitizeCsv(occurrence.relatoDetalhes)};")
            append("${sanitizeCsv(occurrence.acaoTomada)};")
            append("${sanitizeCsv(status)};")
            append("${sanitizeCsv(acaoDefinitiva)};")
            append("${sanitizeCsv(responsavel)};")
            append("${sanitizeCsv(setorResp)};")
            append("${sanitizeCsv(prazo)};")
            append("${if (!occurrence.fotoUri.isNullOrBlank()) "Sim" else "Não"}\n")
        }
    }

    /**
     * Appends or updates an occurrence in the master CSV spreadsheet file.
     */
    fun appendToSpreadsheet(context: Context, occurrence: SafetyOccurrence) {
        try {
            val file = getSpreadsheetFile(context)
            val isNewFile = !file.exists() || file.length() == 0L

            FileOutputStream(file, true).use { out ->
                if (isNewFile) {
                    // Write UTF-8 BOM so Excel opens PT-BR accented characters correctly
                    out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    out.write(CSV_HEADER.toByteArray(Charsets.UTF_8))
                }

                val row = buildCsvRow(occurrence)
                out.write(row.toByteArray(Charsets.UTF_8))
            }
            Log.d("SpreadsheetReportManager", "Ocorrência #${occurrence.id} adicionada à planilha CSV: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("SpreadsheetReportManager", "Erro ao salvar na planilha CSV: ${e.message}", e)
        }
    }

    fun sortSequentiallyByDate(occurrences: List<SafetyOccurrence>): List<SafetyOccurrence> {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return occurrences.sortedWith(Comparator { o1, o2 ->
            try {
                val d1 = sdf.parse("${o1.data} ${o1.hora}")
                val d2 = sdf.parse("${o2.data} ${o2.hora}")
                if (d1 != null && d2 != null) {
                    d1.compareTo(d2)
                } else {
                    o1.timestamp.compareTo(o2.timestamp)
                }
            } catch (e: Exception) {
                o1.timestamp.compareTo(o2.timestamp)
            }
        })
    }

    /**
     * Re-generates the entire CSV spreadsheet file from all occurrences in database, ordered sequentially by date.
     */
    fun syncFullSpreadsheet(context: Context, occurrences: List<SafetyOccurrence>): File {
        val sortedList = sortSequentiallyByDate(occurrences)
        val file = getSpreadsheetFile(context)
        try {
            FileOutputStream(file, false).use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(CSV_HEADER.toByteArray(Charsets.UTF_8))

                for (item in sortedList) {
                    val row = buildCsvRow(item)
                    out.write(row.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            Log.e("SpreadsheetReportManager", "Erro ao gerar planilha completa: ${e.message}", e)
        }
        return file
    }

    /**
     * Executes automatic backup of PDF and CSV Spreadsheet into dedicated backup folder.
     */
    fun createFullBackup(context: Context, occurrences: List<SafetyOccurrence>, pdfFile: File? = null): File {
        val sortedList = sortSequentiallyByDate(occurrences)
        val backupDir = getBackupDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupCsvFile = File(backupDir, "Backup_Planilha_SST_$timestamp.csv")

        // 1. Generate Backup CSV
        try {
            FileOutputStream(backupCsvFile, false).use { out ->
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(CSV_HEADER.toByteArray(Charsets.UTF_8))

                for (item in sortedList) {
                    val row = buildCsvRow(item)
                    out.write(row.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            Log.e("SpreadsheetReportManager", "Erro ao criar backup CSV: ${e.message}")
        }

        // 2. If PDF file provided, copy to backup folder
        if (pdfFile != null && pdfFile.exists()) {
            try {
                val pdfBackupFile = File(backupDir, "Backup_Relatorio_PDF_SST_$timestamp.pdf")
                pdfFile.inputStream().use { input ->
                    pdfBackupFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e("SpreadsheetReportManager", "Erro ao copiar backup PDF: ${e.message}")
            }
        }

        return backupCsvFile
    }

    fun getShareableCsvUri(context: Context, csvFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
    }

    private fun sanitizeCsv(value: String): String {
        val clean = value.replace(";", ",").replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
