package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.SafetyOccurrence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Core SST Management Engine for Risk Matrix, Recurrence Detection,
 * Deadline Tracking, and 6M Root Cause Analysis.
 */
object SstManagementEngine {

    // 4-Level Standardized Risk Scheme
    enum class RiskLevel(
        val label: String,
        val shortLabel: String,
        val fullLabel: String,
        val priority: String,
        val colorCodeHex: String,
        val lightBgHex: String,
        val borderColorHex: String,
        val textColorHex: String
    ) {
        LOW(
            label = "Baixo",
            shortLabel = "BAIXO",
            fullLabel = "BAIXO — VERDE",
            priority = "Programada",
            colorCodeHex = "#22C55E",
            lightBgHex = "#F0FDF4",
            borderColorHex = "#22C55E",
            textColorHex = "#15803D"
        ),
        MEDIUM(
            label = "Médio",
            shortLabel = "MÉDIO",
            fullLabel = "MÉDIO — AMARELO",
            priority = "Prioridade normal",
            colorCodeHex = "#F59E0B",
            lightBgHex = "#FFFBEB",
            borderColorHex = "#F59E0B",
            textColorHex = "#B45309"
        ),
        HIGH(
            label = "Alto",
            shortLabel = "ALTO",
            fullLabel = "ALTO — LARANJA",
            priority = "Prioridade alta",
            colorCodeHex = "#F97316",
            lightBgHex = "#FFF7ED",
            borderColorHex = "#F97316",
            textColorHex = "#C2410C"
        ),
        CRITICAL(
            label = "Crítico",
            shortLabel = "CRÍTICO",
            fullLabel = "CRÍTICO — VERMELHO",
            priority = "Tratativa imediata",
            colorCodeHex = "#EF4444",
            lightBgHex = "#FEF2F2",
            borderColorHex = "#EF4444",
            textColorHex = "#B91C1C"
        );

        val composeColor: Color get() = Color(android.graphics.Color.parseColor(colorCodeHex))
        val composeLightBg: Color get() = Color(android.graphics.Color.parseColor(lightBgHex))
        val composeBorderColor: Color get() = Color(android.graphics.Color.parseColor(borderColorHex))
        val composeTextColor: Color get() = Color(android.graphics.Color.parseColor(textColorHex))

        val emoji: String get() = when (this) {
            LOW -> "🟢"
            MEDIUM -> "🟡"
            HIGH -> "🟠"
            CRITICAL -> "🔴"
        }

        val namePt: String get() = label
    }

    data class RiskAssessment(
        val score: Int,
        val level: RiskLevel,
        val riskLabel: String,
        val priority: String,
        val description: String
    )

    /**
     * Calculates risk level automatically from Probability (1..4) and Severity (1..4).
     * Score = Probabilidade x Severidade
     * 1..3   -> BAIXO (Verde)
     * 4..6   -> MÉDIO (Amarelo)
     * 8..9   -> ALTO (Laranja)
     * 10..16 -> CRÍTICO (Vermelho)
     */
    fun calculateRiskMatrix(probabilidade: Int, severidade: Int): RiskAssessment {
        val p = probabilidade.coerceIn(1, 4)
        val s = severidade.coerceIn(1, 4)
        val score = p * s

        val level = when {
            score <= 3 -> RiskLevel.LOW
            score <= 6 -> RiskLevel.MEDIUM
            score <= 9 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

        val riskLabel = when (level) {
            RiskLevel.LOW -> "Baixo (Verde)"
            RiskLevel.MEDIUM -> "Médio (Amarelo)"
            RiskLevel.HIGH -> "Alto (Laranja)"
            RiskLevel.CRITICAL -> "Crítico (Vermelho)"
        }

        val description = "Matriz de Risco: P($p) × S($s) = Score $score -> ${level.fullLabel}"

        return RiskAssessment(
            score = score,
            level = level,
            riskLabel = riskLabel,
            priority = level.priority,
            description = description
        )
    }

    /**
     * Determines the RiskLevel enum from any risk string representation.
     */
    fun getRiskLevelFromString(riskStr: String): RiskLevel {
        val normalized = riskStr.lowercase(Locale.getDefault())
        return when {
            normalized.contains("crítico") || normalized.contains("critico") || normalized.contains("vermelho") -> RiskLevel.CRITICAL
            normalized.contains("alto") || normalized.contains("laranja") -> RiskLevel.HIGH
            normalized.contains("médio") || normalized.contains("medio") || normalized.contains("amarelo") -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * Single source of truth for resolving the effective RiskLevel of a SafetyOccurrence.
     * Considers explicit Probability and Severity (P x S), falling back to the risk string.
     */
    fun getEffectiveRiskLevel(occurrence: SafetyOccurrence): RiskLevel {
        if (occurrence.probabilidade in 1..4 && occurrence.severidade in 1..4) {
            val score = occurrence.probabilidade * occurrence.severidade
            return when {
                score <= 3 -> RiskLevel.LOW
                score <= 6 -> RiskLevel.MEDIUM
                score <= 9 -> RiskLevel.HIGH
                else -> RiskLevel.CRITICAL
            }
        }
        return getRiskLevelFromString(occurrence.risco)
    }

    /**
     * Checks if an occurrence represents an active alert in the Central de Alertas.
     * An occurrence is an active alert if:
     * 1. It has Critical or High risk level, OR
     * 2. It is an open / pending safety event (not yet finalized as Concluído/Eficaz with conclusion date).
     */
    fun isAlertActive(occurrence: SafetyOccurrence): Boolean {
        val level = getEffectiveRiskLevel(occurrence)
        val isCriticalOrHigh = level == RiskLevel.CRITICAL || level == RiskLevel.HIGH
        val isFinalized = (occurrence.statusAcao.equals("Concluído", true) || occurrence.statusAcao.equals("Eficaz", true)) &&
                occurrence.dataConclusao.isNotBlank()

        return isCriticalOrHigh || !isFinalized
    }

    /**
     * Counts total active alerts in the system for real-time badge and notification synchronization.
     */
    fun countActiveAlerts(occurrences: List<SafetyOccurrence>): Int {
        return occurrences.count { isAlertActive(it) }
    }

    /**
     * Returns the effective risk score (1..16) of an occurrence.
     */
    fun getEffectiveScore(occurrence: SafetyOccurrence): Int {
        if (occurrence.probabilidade in 1..4 && occurrence.severidade in 1..4) {
            return occurrence.probabilidade * occurrence.severidade
        }
        return when (getRiskLevelFromString(occurrence.risco)) {
            RiskLevel.LOW -> 2
            RiskLevel.MEDIUM -> 4
            RiskLevel.HIGH -> 8
            RiskLevel.CRITICAL -> 12
        }
    }

    // 6M Root Cause Methodology Standard Categories
    val CAUSE_CATEGORIES_6M = listOf(
        "Mão de Obra / Fator Humano",
        "Método / Procedimentos",
        "Máquinas / Equipamentos",
        "Material / EPI / Ferramentas",
        "Meio Ambiente",
        "Gestão / Organização"
    )

    val CAUSE_SUGGESTIONS_BY_CATEGORY = mapOf(
        "Mão de Obra / Fator Humano" to listOf(
            "Falta de Atenção / Distração",
            "Falta de Treinamento / Capacitação",
            "Não Utilização de EPI",
            "Comportamento de Risco / Imprudência",
            "Pressa / Fadiga Operacional",
            "Excesso de Autoconfiança"
        ),
        "Método / Procedimentos" to listOf(
            "Procedimento Operacional Inexistente",
            "Procedimento Incorreto ou Desatualizado",
            "Não Cumprimento de APR / Permissão de Trabalho",
            "Falta de Bloqueio LOTO (Desenergização)",
            "Armazenamento / Empilhamento Inadequado",
            "Trabalho em Altura sem Ancoragem"
        ),
        "Máquinas / Equipamentos" to listOf(
            "Falha Mecânica / Rompimento",
            "Falta de Proteção Coletiva (NR-12)",
            "Sensor / Intertravamento Inoperante",
            "Vazamento Hidráulico / Pneumático",
            "Desgaste Prematuro de Componente",
            "Equipamento sem Manutenção Preventiva"
        ),
        "Material / EPI / Ferramentas" to listOf(
            "EPI Danificado ou Inadequado ao Risco",
            "Ferramenta Improvisada / Inadequada",
            "Produto Químico sem FISPQ / Rotulagem",
            "Material Instável / Peso Excessivo",
            "Falta de Dispositivo de Içamento Certificado"
        ),
        "Meio Ambiente" to listOf(
            "Piso Escorregadio / Óleo ou Água",
            "Iluminação Inadequada / Lâmpada Queimada",
            "Ruído Excessivo / Área Ruidosa",
            "Calor / Frio Extremo",
            "Ventilação Insuficiente / Poeiras",
            "Chuva / Piso Molhado Externo"
        ),
        "Gestão / Organização" to listOf(
            "Falta de Planejamento da Atividade",
            "Falha de Comunicação entre Turnos",
            "Fiscalização / Auditoria Insuficiente",
            "Pressão por Produtividade",
            "Demora no Fornecimento de Recursos EHS"
        )
    )

    data class DeadlineInfo(
        val diasDesdeAbertura: Long,
        val diasRestantes: Long,
        val diasEmAtraso: Long,
        val isAtrasado: Boolean,
        val statusDisplay: String,
        val statusBadgeLabel: String
    ) {
        val label: String get() = statusBadgeLabel
        val color: Color get() = if (isAtrasado) Color(0xFFB91C1C) else Color(0xFF15803D)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Calculates deadline information and checks whether the action is overdue.
     */
    fun calculateDeadlineInfo(
        dataAberturaStr: String,
        prazoAcaoStr: String,
        statusAcao: String
    ): DeadlineInfo {
        val today = Date()
        val todayMillis = today.time

        val aberturaDate = parseDateOrNull(dataAberturaStr) ?: today
        val prazoDate = parseDateOrNull(prazoAcaoStr)

        val diasDesdeAbertura = TimeUnit.MILLISECONDS.toDays(todayMillis - aberturaDate.time).coerceAtLeast(0)

        val isFinalizado = statusAcao.equals("Concluído", ignoreCase = true) ||
                statusAcao.equals("Eficaz", ignoreCase = true) ||
                statusAcao.equals("Cancelado", ignoreCase = true)

        var diasRestantes: Long = 0
        var diasEmAtraso: Long = 0
        var isAtrasado = false

        if (prazoDate != null) {
            val diff = prazoDate.time - todayMillis
            val diffDays = TimeUnit.MILLISECONDS.toDays(diff)
            if (diffDays < 0 && !isFinalizado) {
                isAtrasado = true
                diasEmAtraso = kotlin.math.abs(diffDays)
                diasRestantes = 0
            } else {
                diasRestantes = diffDays.coerceAtLeast(0)
                diasEmAtraso = 0
            }
        }

        val statusDisplay = if (isAtrasado) "Atrasado" else statusAcao
        val statusBadgeLabel = when {
            isAtrasado -> "🔴 AÇÃO ATRASADA ($diasEmAtraso d)"
            statusAcao.equals("Eficaz", true) -> "✓ EFICAZ"
            statusAcao.equals("Concluído", true) -> "✓ CONCLUÍDO"
            statusAcao.equals("Aguardando Validação", true) -> "⏳ AGUARDANDO VALIDAÇÃO"
            statusAcao.equals("Em Tratativa", true) -> "⚙ EM TRATATIVA"
            else -> "⏳ PENDENTE"
        }

        return DeadlineInfo(
            diasDesdeAbertura = diasDesdeAbertura,
            diasRestantes = diasRestantes,
            diasEmAtraso = diasEmAtraso,
            isAtrasado = isAtrasado,
            statusDisplay = statusDisplay,
            statusBadgeLabel = statusBadgeLabel
        )
    }

    private fun parseDateOrNull(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            dateFormat.parse(dateStr.trim())
        } catch (e: Exception) {
            null
        }
    }

    data class RecurrenceResult(
        val isRecurring: Boolean,
        val matchingCount: Int,
        val previousOccurrences: List<SafetyOccurrence>,
        val reason: String
    )

    /**
     * Identifies whether an occurrence is recurrent based on similarities with previous records:
     * - Same Local or Setor AND (matching perigo, causa, or ocorrência)
     */
    fun checkRecurrence(
        current: SafetyOccurrence,
        allOccurrences: List<SafetyOccurrence>
    ): RecurrenceResult {
        val previous = allOccurrences.filter { it.id != current.id && it.id > 0 }
        if (previous.isEmpty()) {
            return RecurrenceResult(false, 0, emptyList(), "")
        }

        val normalizedLocal = current.local.trim().lowercase(Locale.getDefault())
        val normalizedSetor = current.setor.trim().lowercase(Locale.getDefault())
        val normalizedPerigo = current.perigo.trim().lowercase(Locale.getDefault())
        val normalizedCausa = current.causa.trim().lowercase(Locale.getDefault())
        val normalizedOcorrencia = current.ocorrencia.trim().lowercase(Locale.getDefault())

        val matches = previous.filter { prev ->
            val pLocal = prev.local.trim().lowercase(Locale.getDefault())
            val pSetor = prev.setor.trim().lowercase(Locale.getDefault())
            val pPerigo = prev.perigo.trim().lowercase(Locale.getDefault())
            val pCausa = prev.causa.trim().lowercase(Locale.getDefault())
            val pOcorrencia = prev.ocorrencia.trim().lowercase(Locale.getDefault())

            val samePlace = (normalizedLocal.isNotBlank() && pLocal == normalizedLocal) ||
                    (normalizedSetor.isNotBlank() && pSetor == normalizedSetor)

            val sameIssue = (normalizedCausa.isNotBlank() && pCausa == normalizedCausa) ||
                    (normalizedPerigo.isNotBlank() && pPerigo.isNotBlank() && (pPerigo.contains(normalizedPerigo) || normalizedPerigo.contains(pPerigo))) ||
                    (normalizedOcorrencia.isNotBlank() && pOcorrencia == normalizedOcorrencia)

            samePlace && sameIssue
        }

        val isRec = matches.isNotEmpty()
        val reason = if (isRec) {
            "Identificada reincidência de ${matches.size} ocorrência(s) anterior(es) no mesmo local/setor ('${current.local.ifBlank { current.setor }}') com a mesma causa/perigo."
        } else {
            ""
        }

        return RecurrenceResult(
            isRecurring = isRec,
            matchingCount = matches.size,
            previousOccurrences = matches,
            reason = reason
        )
    }

    /**
     * Parses the occurrence date and time into epoch milliseconds for accurate chronology.
     */
    fun parseOccurrenceDateTimeMillis(occurrence: SafetyOccurrence): Long {
        val dateStr = occurrence.data.trim()
        val timeStr = occurrence.hora.trim().let { if (it.isBlank()) "00:00" else it }

        val formatsWithTime = listOf(
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm"
        )
        val formatsDateOnly = listOf(
            "dd/MM/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy"
        )

        for (format in formatsWithTime) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = true
                val parsed = sdf.parse("$dateStr $timeStr")
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) { }
        }

        for (format in formatsDateOnly) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = true
                val parsed = sdf.parse(dateStr)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) { }
        }

        return if (occurrence.timestamp > 0) occurrence.timestamp else occurrence.id
    }

    /**
     * Sorts occurrences from newest to oldest (descending order) based on the occurrence ID number.
     */
    fun sortOccurrencesByIdDescending(occurrences: List<SafetyOccurrence>): List<SafetyOccurrence> {
        return occurrences.sortedByDescending { it.id }
    }

    /**
     * Sorts occurrences from newest to oldest (descending order) based on date and time.
     */
    fun sortOccurrencesByDateTimeDescending(occurrences: List<SafetyOccurrence>): List<SafetyOccurrence> {
        return occurrences.sortedWith(
            compareByDescending<SafetyOccurrence> { parseOccurrenceDateTimeMillis(it) }
                .thenByDescending { it.id }
        )
    }
}
