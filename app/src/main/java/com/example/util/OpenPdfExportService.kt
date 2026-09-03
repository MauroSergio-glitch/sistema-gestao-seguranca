package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.SafetyOccurrence
import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.ColumnText
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OpenPDF Export Service for FOCO NA PREVENÇÃO HSE/SST Reports.
 * Generates structured, corporate-branded, executive PDF reports using OpenPDF.
 */
object OpenPdfExportService {

    // Executive Corporate Palette (HSSE / SST Standard)
    private val COLOR_PRIMARY = Color(15, 32, 39)          // Deep Navy Slate #0F2027
    private val COLOR_NAVY_DARK = Color(24, 43, 73)        // Corporate Navy #182B49
    private val COLOR_ACCENT = Color(22, 101, 52)          // Executive Safety Green #166534
    private val COLOR_LIGHT_BG = Color(248, 250, 252)      // Off-white Soft Gray #F8FAFC
    private val COLOR_ROW_ALT = Color(241, 245, 249)       // Card Header Neutral #F1F5F9
    private val COLOR_BOX_DETAIL_BG = Color(245, 247, 250) // Detail Box Soft Gray #F5F7FA
    private val COLOR_BOX_ACTION_BG = Color(240, 253, 244) // Action Box Light Emerald #F0FDF4
    private val COLOR_TEXT_DARK = Color(30, 41, 59)        // Slate Dark #1E293B
    private val COLOR_TEXT_MUTED = Color(100, 116, 139)    // Slate Muted #64748B
    private val COLOR_BORDER = Color(226, 232, 240)        // Soft Slate Border #E2E8F0
    private val COLOR_BORDER_ACTION = Color(187, 247, 208) // Light green action border #BBF7D0
    private val COLOR_WHITE = Color.WHITE

    // Standardized 4-Level Risk Color Indicators (Green, Yellow, Orange, Red)
    data class RiskColorScheme(
        val bg: Color,
        val border: Color,
        val text: Color,
        val label: String
    )

    val RISK_LOW = RiskColorScheme(
        bg = Color(240, 253, 244),      // #F0FDF4
        border = Color(34, 197, 94),     // #22C55E
        text = Color(21, 128, 61),       // #15803D
        label = "BAIXO — VERDE"
    )

    val RISK_MEDIUM = RiskColorScheme(
        bg = Color(255, 251, 235),     // #FFFBEB
        border = Color(245, 158, 11),   // #F59E0B
        text = Color(180, 83, 9),       // #B45309
        label = "MÉDIO — AMARELO"
    )

    val RISK_HIGH = RiskColorScheme(
        bg = Color(255, 247, 237),      // #FFF7ED
        border = Color(249, 115, 22),   // #F97316
        text = Color(194, 65, 12),      // #C2410C
        label = "ALTO — LARANJA"
    )

    val RISK_CRITICAL = RiskColorScheme(
        bg = Color(254, 242, 242),      // #FEF2F2
        border = Color(239, 68, 68),    // #EF4444
        text = Color(185, 28, 28),      // #B91C1C
        label = "CRÍTICO — VERMELHO"
    )

    fun getRiskScheme(level: SstManagementEngine.RiskLevel): RiskColorScheme {
        return when (level) {
            SstManagementEngine.RiskLevel.CRITICAL -> RISK_CRITICAL
            SstManagementEngine.RiskLevel.HIGH -> RISK_HIGH
            SstManagementEngine.RiskLevel.MEDIUM -> RISK_MEDIUM
            SstManagementEngine.RiskLevel.LOW -> RISK_LOW
        }
    }

    fun getRiskScheme(occurrence: SafetyOccurrence): RiskColorScheme {
        return getRiskScheme(SstManagementEngine.getEffectiveRiskLevel(occurrence))
    }

    fun getRiskScheme(riskStr: String): RiskColorScheme {
        return getRiskScheme(SstManagementEngine.getRiskLevelFromString(riskStr))
    }

    /**
     * Generates a structured OpenPDF report file for the given occurrences and date.
     */
    fun exportReport(
        context: Context,
        dateStr: String,
        occurrences: List<SafetyOccurrence>,
        outputFile: File
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sortedOccurrences = occurrences.sortedWith(Comparator { o1, o2 ->
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

        val emitTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val document = Document(PageSize.A4, 32f, 32f, 36f, 36f)
        val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
        val pageEvent = HeaderFooterPageEvent(dateStr)
        writer.pageEvent = pageEvent

        document.open()

        // 1. INSTITUTIONAL BRANDING HEADER
        val headerTable = buildBrandingHeaderTable(context, dateStr, emitTime, sortedOccurrences.size)
        document.add(headerTable)

        // Accent Divider Line (Navy + Emerald)
        val lineTable = PdfPTable(1).apply { widthPercentage = 100f }
        val lineCell = PdfPCell().apply {
            border = Rectangle.TOP
            borderColor = COLOR_NAVY_DARK
            borderWidth = 2f
            setFixedHeight(4f)
        }
        lineTable.addCell(lineCell)
        document.add(lineTable)
        document.add(Paragraph(" ").apply { leading = 4f })

        // 2. EXECUTIVE KPI DASHBOARD (5 METRIC CARDS)
        val kpiTable = buildKpiDashboardTable(sortedOccurrences)
        document.add(kpiTable)
        document.add(Paragraph(" ").apply { leading = 8f })

        // 3. SECTION 1: DETAILED OCCURRENCES
        val sec1Banner = PdfPTable(1).apply { widthPercentage = 100f }
        val bannerPhrase = Phrase("1. REGISTROS TÉCNICOS DETALHADOS DAS OCORRÊNCIAS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_WHITE))
        val bannerCell = PdfPCell(bannerPhrase).apply {
            backgroundColor = COLOR_NAVY_DARK
            border = Rectangle.NO_BORDER
            paddingTop = 4f
            paddingBottom = 4f
            paddingLeft = 6f
        }
        sec1Banner.addCell(bannerCell)
        document.add(sec1Banner)
        document.add(Paragraph(" ").apply { leading = 6f })

        if (sortedOccurrences.isEmpty()) {
            val emptyTable = PdfPTable(1).apply { widthPercentage = 100f }
            val emptyPhrase = Phrase("Nenhuma ocorrência registrada para o período selecionado.", FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_MUTED))
            val emptyCell = PdfPCell(emptyPhrase).apply {
                backgroundColor = COLOR_LIGHT_BG
                borderColor = COLOR_BORDER
                borderWidth = 0.8f
                horizontalAlignment = Element.ALIGN_CENTER
                setPadding(14f)
            }
            emptyTable.addCell(emptyCell)
            document.add(emptyTable)
            document.add(Paragraph(" ").apply { leading = 10f })
        } else {
            for ((index, item) in sortedOccurrences.withIndex()) {
                val incidentCard = buildStandaloneIncidentCard(context, item, index + 1)
                document.add(incidentCard)
                document.add(Paragraph(" ").apply { leading = 6f })
            }
        }

        // 4. SECTION 2: FORMAL SIGNATURES & COMPLIANCE CLOSURE (NR-01 / NR-12)
        val signaturesTable = buildFormalSignaturesBlock(emitTime)
        document.add(signaturesTable)

        document.close()
    }

    private fun buildBrandingHeaderTable(
        context: Context,
        dateStr: String,
        emitTime: String,
        totalCount: Int
    ): PdfPTable {
        val table = PdfPTable(floatArrayOf(12f, 58f, 30f)).apply { widthPercentage = 100f }

        // Logo Cell
        val logoImg = getLogoImage(context)
        val logoCell = if (logoImg != null) {
            PdfPCell(logoImg, true).apply {
                backgroundColor = COLOR_LIGHT_BG
                borderColor = COLOR_BORDER
                borderWidth = 0.8f
                setPadding(3f)
                horizontalAlignment = Element.ALIGN_CENTER
                verticalAlignment = Element.ALIGN_MIDDLE
            }
        } else {
            PdfPCell(Phrase("FOCO NA PREVENÇÃO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, COLOR_NAVY_DARK))).apply {
                backgroundColor = COLOR_LIGHT_BG
                borderColor = COLOR_BORDER
                borderWidth = 0.8f
                horizontalAlignment = Element.ALIGN_CENTER
                verticalAlignment = Element.ALIGN_MIDDLE
            }
        }
        table.addCell(logoCell)

        // Title and Subtitle Cell
        val titlePhrase = Phrase().apply {
            add(Chunk("RELATÓRIO TÉCNICO DE INVESTIGAÇÃO DE OCORRÊNCIAS SST\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_NAVY_DARK)))
            add(Chunk("SISTEMA DE GESTÃO EM SEGURANÇA E SAÚDE OCUPACIONAL\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_ACCENT)))
            add(Chunk("Registro Oficial de Quase-Acidentes, Atos Inseguros e Condições de Risco (NR-01 / NR-12)", FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_TEXT_MUTED)))
        }
        val titleCell = PdfPCell(titlePhrase).apply {
            border = Rectangle.NO_BORDER
            paddingLeft = 8f
            paddingRight = 4f
            verticalAlignment = Element.ALIGN_MIDDLE
        }
        table.addCell(titleCell)

        // Metadata Panel Cell
        val metaPhrase = Phrase().apply {
            add(Chunk("EMISSÃO:  ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("$emitTime\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, COLOR_NAVY_DARK)))

            add(Chunk("DATA BASE: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("$dateStr\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, COLOR_NAVY_DARK)))

            add(Chunk("VOLUME:    ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("$totalCount registro(s)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, COLOR_ACCENT)))
        }
        val metaCell = PdfPCell(metaPhrase).apply {
            backgroundColor = COLOR_LIGHT_BG
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            setPadding(5f)
            verticalAlignment = Element.ALIGN_MIDDLE
        }
        table.addCell(metaCell)

        return table
    }

    private fun buildKpiDashboardTable(occurrences: List<SafetyOccurrence>): PdfPTable {
        val criticalCount = occurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.CRITICAL }
        val highCount = occurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.HIGH }
        val medCount = occurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.MEDIUM }
        val lowCount = occurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.LOW }

        val table = PdfPTable(5).apply { widthPercentage = 100f }

        data class KpiCard(val title: String, val count: Int, val bg: Color, val border: Color, val text: Color)

        val cards = listOf(
            KpiCard("TOTAL DE RELATOS", occurrences.size, COLOR_LIGHT_BG, COLOR_NAVY_DARK, COLOR_NAVY_DARK),
            KpiCard("CRÍTICO (VERMELHO)", criticalCount, RISK_CRITICAL.bg, RISK_CRITICAL.border, RISK_CRITICAL.text),
            KpiCard("ALTO (LARANJA)", highCount, RISK_HIGH.bg, RISK_HIGH.border, RISK_HIGH.text),
            KpiCard("MÉDIO (AMARELO)", medCount, RISK_MEDIUM.bg, RISK_MEDIUM.border, RISK_MEDIUM.text),
            KpiCard("BAIXO (VERDE)", lowCount, RISK_LOW.bg, RISK_LOW.border, RISK_LOW.text)
        )

        for (card in cards) {
            val kpiPhrase = Phrase().apply {
                add(Chunk("${card.title}\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.8f, card.text)))
                add(Chunk("${card.count}", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, card.text)))
            }
            val cell = PdfPCell(kpiPhrase).apply {
                backgroundColor = card.bg
                borderColor = card.border
                borderWidth = 0.8f
                horizontalAlignment = Element.ALIGN_CENTER
                verticalAlignment = Element.ALIGN_MIDDLE
                paddingTop = 4f
                paddingBottom = 4f
            }
            table.addCell(cell)
        }

        return table
    }

    /**
     * Builds a standalone, color-coded block for each incident (with keepTogether = true).
     */
    private fun buildStandaloneIncidentCard(
        context: Context,
        item: SafetyOccurrence,
        itemNum: Int
    ): PdfPTable {
        val effectiveLevel = SstManagementEngine.getEffectiveRiskLevel(item)
        val risk = getRiskScheme(effectiveLevel)

        val cardTable = PdfPTable(1).apply {
            widthPercentage = 100f
            keepTogether = true
            isSplitRows = false
            isSplitLate = true
        }

        val innerTable = PdfPTable(1).apply { widthPercentage = 100f }

        // 1. Header Bar with ID, Date, Event, and Risk Badge
        val headerTable = PdfPTable(floatArrayOf(76f, 24f)).apply { widthPercentage = 100f }

        val headerPhrase = Phrase().apply {
            add(Chunk("#${String.format(Locale.getDefault(), "%02d", itemNum)} | ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, COLOR_NAVY_DARK)))
            add(Chunk("${item.data} às ${item.hora} — ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_DARK)))
            add(Chunk(item.ocorrencia.ifBlank { "Relato SST" }, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_NAVY_DARK)))
        }
        val leftHeaderCell = PdfPCell(headerPhrase).apply {
            backgroundColor = COLOR_ROW_ALT
            border = Rectangle.NO_BORDER
            paddingTop = 5f
            paddingBottom = 5f
            paddingLeft = 6f
            verticalAlignment = Element.ALIGN_MIDDLE
        }
        headerTable.addCell(leftHeaderCell)

        val badgePhrase = Phrase("RISCO: ${risk.label}", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, risk.text))
        val badgeCell = PdfPCell(badgePhrase).apply {
            backgroundColor = risk.bg
            borderColor = risk.border
            borderWidth = 0.8f
            horizontalAlignment = Element.ALIGN_CENTER
            verticalAlignment = Element.ALIGN_MIDDLE
            paddingTop = 3f
            paddingBottom = 3f
        }
        val badgeWrapperTable = PdfPTable(1).apply {
            widthPercentage = 95f
            addCell(badgeCell)
        }

        val rightHeaderCell = PdfPCell(badgeWrapperTable).apply {
            backgroundColor = COLOR_ROW_ALT
            border = Rectangle.NO_BORDER
            paddingRight = 6f
            verticalAlignment = Element.ALIGN_MIDDLE
            horizontalAlignment = Element.ALIGN_RIGHT
        }
        headerTable.addCell(rightHeaderCell)

        innerTable.addCell(PdfPCell(headerTable).apply {
            border = Rectangle.BOTTOM
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            setPadding(0f)
        })

        // 2. Attributes Two-Column Grid
        val attrTable = PdfPTable(floatArrayOf(50f, 50f)).apply { widthPercentage = 100f }

        val col1Phrase = Phrase().apply {
            add(Chunk("Colaborador: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            val empText = if (item.nomeColaborador.isNotBlank()) "${item.nomeColaborador} (RE: ${item.registro.ifBlank { "N/A" }})" else "Não identificado / Geral"
            add(Chunk("$empText\n", FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))

            add(Chunk("Local Específico: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("${item.local.ifBlank { "Não informado" }}\n", FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))

            add(Chunk("Perigo Identificado: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("${item.perigo.ifBlank { "Não especificado" }}\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_NAVY_DARK)))

            add(Chunk("Causa Raiz (6M): ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            val causaFull = if (item.categoriaCausa.isNotBlank()) "${item.categoriaCausa} — ${item.causa}" else item.causa.ifBlank { "A apurar" }
            add(Chunk(causaFull, FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))
        }
        val col1Cell = PdfPCell(col1Phrase).apply {
            border = Rectangle.NO_BORDER
            paddingTop = 5f
            paddingBottom = 5f
            paddingLeft = 6f
            paddingRight = 3f
        }
        attrTable.addCell(col1Cell)

        val col2Phrase = Phrase().apply {
            add(Chunk("Setor / Área: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("${item.setor.ifBlank { "Não informado" }}\n", FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))

            add(Chunk("Classificação SST: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            add(Chunk("${item.classificacao.ifBlank { "SST" }}\n", FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))

            add(Chunk("Matriz Risco (P×S): ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            val p = if (item.probabilidade in 1..4) item.probabilidade else 2
            val s = if (item.severidade in 1..4) item.severidade else 2
            val score = SstManagementEngine.getEffectiveScore(item)
            val prio = item.prioridade.ifBlank { effectiveLevel.priority }
            add(Chunk("Prob. $p × Sev. $s = Score $score ($prio)\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, risk.text)))

            add(Chunk("Status da Ação: ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_TEXT_MUTED)))
            val respText = if (item.responsavelAcao.isNotBlank()) " | Resp: ${item.responsavelAcao}" else ""
            val prazoText = if (item.prazoAcao.isNotBlank()) " | Prazo: ${item.prazoAcao}" else ""
            add(Chunk("${item.statusAcao}$respText$prazoText", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_NAVY_DARK)))
        }
        val col2Cell = PdfPCell(col2Phrase).apply {
            border = Rectangle.NO_BORDER
            paddingTop = 5f
            paddingBottom = 5f
            paddingLeft = 3f
            paddingRight = 6f
        }
        attrTable.addCell(col2Cell)

        innerTable.addCell(PdfPCell(attrTable).apply {
            border = Rectangle.NO_BORDER
            setPadding(0f)
        })

        // 3. Circumstantial Description Box
        val descPhrase = Phrase().apply {
            add(Chunk("DESCRIÇÃO CIRCUNSTANCIADA DO FATO / RELATO:\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_NAVY_DARK)))
            add(Chunk(item.relatoDetalhes.ifBlank { "Sem relato ou circunstâncias detalhadas informadas." }, FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))
        }
        val descCell = PdfPCell(descPhrase).apply {
            backgroundColor = COLOR_BOX_DETAIL_BG
            borderColor = COLOR_BORDER
            borderWidth = 0.6f
            setPadding(5f)
        }
        val descWrapper = PdfPTable(1).apply {
            widthPercentage = 100f
            addCell(descCell)
        }
        innerTable.addCell(PdfPCell(descWrapper).apply {
            border = Rectangle.NO_BORDER
            paddingLeft = 6f
            paddingRight = 6f
            paddingBottom = 4f
        })

        // 4. Immediate Action & Preventive Plan Box (Emerald Accent)
        val actionTable = PdfPTable(floatArrayOf(2f, 98f)).apply { widthPercentage = 100f }

        val stripeCell = PdfPCell().apply {
            backgroundColor = COLOR_ACCENT
            border = Rectangle.NO_BORDER
        }
        actionTable.addCell(stripeCell)

        val actionPhrase = Phrase().apply {
            add(Chunk("PLANO DE AÇÃO IMEDIATA, CONTENÇÃO E AÇÃO PREVENTIVA DEFINITIVA:\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_ACCENT)))
            val acaoImediata = item.acaoTomada.ifBlank { "Nenhuma ação imediata registrada." }
            add(Chunk("Ação Imediata: $acaoImediata\n", FontFactory.getFont(FontFactory.HELVETICA, 7.2f, COLOR_TEXT_DARK)))
            if (item.acaoPreventiva.isNotBlank()) {
                add(Chunk("Ação Preventiva: ${item.acaoPreventiva}\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_NAVY_DARK)))
            }
            if (item.descricaoSolucao.isNotBlank() || item.avaliacaoEficacia != "Pendente") {
                val solText = if (item.descricaoSolucao.isNotBlank()) "Solução: ${item.descricaoSolucao} | " else ""
                add(Chunk("$solText Eficácia: ${item.avaliacaoEficacia}", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6.8f, COLOR_TEXT_MUTED)))
            }
        }
        val actionContentCell = PdfPCell(actionPhrase).apply {
            backgroundColor = COLOR_BOX_ACTION_BG
            borderColor = COLOR_BORDER_ACTION
            borderWidth = 0.6f
            border = Rectangle.TOP or Rectangle.BOTTOM or Rectangle.RIGHT
            setPadding(5f)
        }
        actionTable.addCell(actionContentCell)

        innerTable.addCell(PdfPCell(actionTable).apply {
            border = Rectangle.NO_BORDER
            paddingLeft = 6f
            paddingRight = 6f
            paddingBottom = 5f
        })

        // 5. Photographic Evidence (Antes / Depois)
        val photoAntesImg = getOccurrencePhotoImage(context, item.fotoUri)
        val photoDepoisImg = getOccurrencePhotoImage(context, item.fotoDepoisUri)

        if (photoAntesImg != null || photoDepoisImg != null) {
            val photoGrid = PdfPTable(if (photoAntesImg != null && photoDepoisImg != null) 2 else 1).apply {
                widthPercentage = 100f
            }

            if (photoAntesImg != null) {
                val pTable1 = PdfPTable(floatArrayOf(35f, 65f)).apply { widthPercentage = 100f }
                val c1 = PdfPCell(photoAntesImg, true).apply {
                    border = Rectangle.BOX
                    borderColor = COLOR_BORDER
                    borderWidth = 0.5f
                    setPadding(2f)
                }
                val cap1 = Phrase().apply {
                    add(Chunk("EVIDÊNCIA (ANTES)\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, COLOR_NAVY_DARK)))
                    add(Chunk("Registro de campo da condição/ato no ato da constatação.", FontFactory.getFont(FontFactory.HELVETICA, 5.8f, COLOR_TEXT_MUTED)))
                }
                pTable1.addCell(c1)
                pTable1.addCell(PdfPCell(cap1).apply { border = Rectangle.NO_BORDER; paddingLeft = 4f })
                photoGrid.addCell(PdfPCell(pTable1).apply { border = Rectangle.NO_BORDER; paddingRight = 4f })
            }

            if (photoDepoisImg != null) {
                val pTable2 = PdfPTable(floatArrayOf(35f, 65f)).apply { widthPercentage = 100f }
                val c2 = PdfPCell(photoDepoisImg, true).apply {
                    border = Rectangle.BOX
                    borderColor = COLOR_BORDER_ACTION
                    borderWidth = 0.5f
                    setPadding(2f)
                }
                val cap2 = Phrase().apply {
                    add(Chunk("EVIDÊNCIA (DEPOIS / RESOLVIDO)\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, COLOR_ACCENT)))
                    add(Chunk("Comprovação da ação corretiva/preventiva concluída.", FontFactory.getFont(FontFactory.HELVETICA, 5.8f, COLOR_TEXT_MUTED)))
                }
                pTable2.addCell(c2)
                pTable2.addCell(PdfPCell(cap2).apply { border = Rectangle.NO_BORDER; paddingLeft = 4f })
                photoGrid.addCell(PdfPCell(pTable2).apply { border = Rectangle.NO_BORDER; paddingLeft = 4f })
            }

            innerTable.addCell(PdfPCell(photoGrid).apply {
                border = Rectangle.NO_BORDER
                paddingLeft = 6f
                paddingRight = 6f
                paddingBottom = 5f
            })
        }

        // Outer cell with colored left border according to risk severity
        val outerCell = PdfPCell(innerTable).apply {
            backgroundColor = COLOR_WHITE
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            border = Rectangle.BOX
            borderWidthLeft = 3.5f
            borderColorLeft = risk.border
            setPadding(0f)
        }

        cardTable.addCell(outerCell)
        return cardTable
    }

    private fun buildFormalSignaturesBlock(emitTime: String): PdfPTable {
        val containerTable = PdfPTable(1).apply {
            widthPercentage = 100f
            keepTogether = true
        }

        // Title Banner
        val titlePhrase = Phrase("2. TERMO DE ENCERRAMENTO E VALIDAÇÃO TÉCNICA FORMAL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_WHITE))
        val titleCell = PdfPCell(titlePhrase).apply {
            backgroundColor = COLOR_ACCENT
            border = Rectangle.NO_BORDER
            paddingTop = 4f
            paddingBottom = 4f
            paddingLeft = 6f
        }
        containerTable.addCell(titleCell)

        // Compliance Declaration Statement
        val declPhrase = Phrase(
            "Certificamos para os devidos fins de conformidade com as Normas Regulamentadoras (NR-01/NR-12) que as ocorrências e ações corretivas foram apuradas tecnicamente, tendo sido direcionadas aos responsáveis para imediato cumprimento.",
            FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_TEXT_DARK)
        )
        val declCell = PdfPCell(declPhrase).apply {
            backgroundColor = COLOR_LIGHT_BG
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            setPadding(5f)
        }
        containerTable.addCell(declCell)

        // Spacer
        val spacerCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            setFixedHeight(6f)
        }
        containerTable.addCell(spacerCell)

        // Dual Signature Boxes
        val sigGrid = PdfPTable(floatArrayOf(48.5f, 3f, 48.5f)).apply { widthPercentage = 100f }

        // Box 1: Responsável Técnico SST / TST
        val box1Table = PdfPTable(1).apply { widthPercentage = 100f }
        val strip1 = PdfPCell().apply {
            backgroundColor = COLOR_ACCENT
            border = Rectangle.NO_BORDER
            setFixedHeight(2.5f)
        }
        box1Table.addCell(strip1)

        val content1Phrase = Phrase().apply {
            add(Chunk("\n_____________________________________\n", FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_DARK)))
            add(Chunk("RESPONSÁVEL TÉCNICO DE SEGURANÇA (SST)\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_NAVY_DARK)))
            add(Chunk("Nome: _____________________________________\n", FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)))
            add(Chunk("Reg. Profissional / MTE: _________  Data: ___/___/______", FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)))
        }
        val content1Cell = PdfPCell(content1Phrase).apply {
            backgroundColor = COLOR_WHITE
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_CENTER
            paddingBottom = 6f
            paddingLeft = 4f
            paddingRight = 4f
        }
        box1Table.addCell(content1Cell)

        val sigBox1Cell = PdfPCell(box1Table).apply {
            backgroundColor = COLOR_WHITE
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            setPadding(0f)
        }
        sigGrid.addCell(sigBox1Cell)

        // Column gap
        sigGrid.addCell(PdfPCell().apply { border = Rectangle.NO_BORDER })

        // Box 2: Supervisor / Gestor da Área Envolvida
        val box2Table = PdfPTable(1).apply { widthPercentage = 100f }
        val strip2 = PdfPCell().apply {
            backgroundColor = COLOR_NAVY_DARK
            border = Rectangle.NO_BORDER
            setFixedHeight(2.5f)
        }
        box2Table.addCell(strip2)

        val content2Phrase = Phrase().apply {
            add(Chunk("\n_____________________________________\n", FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_DARK)))
            add(Chunk("SUPERVISOR / GESTOR DA ÁREA ENVOLVIDA\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_NAVY_DARK)))
            add(Chunk("Nome: _____________________________________\n", FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)))
            add(Chunk("Cargo / Setor: _________________  Data: ___/___/______", FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)))
        }
        val content2Cell = PdfPCell(content2Phrase).apply {
            backgroundColor = COLOR_WHITE
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_CENTER
            paddingBottom = 6f
            paddingLeft = 4f
            paddingRight = 4f
        }
        box2Table.addCell(content2Cell)

        val sigBox2Cell = PdfPCell(box2Table).apply {
            backgroundColor = COLOR_WHITE
            borderColor = COLOR_BORDER
            borderWidth = 0.8f
            setPadding(0f)
        }
        sigGrid.addCell(sigBox2Cell)

        containerTable.addCell(PdfPCell(sigGrid).apply {
            border = Rectangle.NO_BORDER
            setPadding(0f)
        })

        // Electronic Validation Footer Note
        val stampPhrase = Phrase(
            "Documento gerado e validado eletronicamente através do aplicativo oficial Foco na Prevenção SST em $emitTime",
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 6f, COLOR_TEXT_MUTED)
        )
        val stampCell = PdfPCell(stampPhrase).apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_CENTER
            paddingTop = 6f
        }
        containerTable.addCell(stampCell)

        return containerTable
    }

    private class HeaderFooterPageEvent(
        private val dateStr: String
    ) : PdfPageEventHelper() {
        private val fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, COLOR_NAVY_DARK)
        private val fontHeaderDate = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, COLOR_TEXT_MUTED)
        private val fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)
        private val fontFooterPage = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_NAVY_DARK)

        override fun onEndPage(writer: PdfWriter, document: Document) {
            val cb = writer.directContent
            val pageNum = writer.pageNumber

            // Header line & text on subsequent pages
            if (pageNum > 1) {
                cb.setColorStroke(COLOR_ACCENT)
                cb.setLineWidth(1f)
                cb.moveTo(document.left(), document.top() + 10f)
                cb.lineTo(document.right(), document.top() + 10f)
                cb.stroke()

                ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_LEFT,
                    Phrase("FOCO NA PREVENÇÃO SST / GESTÃO DE SEGURANÇA E SAÚDE OCUPACIONAL — RELATÓRIO TÉCNICO", fontHeader),
                    document.left(),
                    document.top() + 14f,
                    0f
                )
                ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_RIGHT,
                    Phrase("Data Base: $dateStr", fontHeaderDate),
                    document.right(),
                    document.top() + 14f,
                    0f
                )
            }

            // Standard Footer on all pages
            cb.setColorStroke(COLOR_BORDER)
            cb.setLineWidth(0.8f)
            cb.moveTo(document.left(), document.bottom() - 6f)
            cb.lineTo(document.right(), document.bottom() - 6f)
            cb.stroke()

            ColumnText.showTextAligned(
                cb,
                Element.ALIGN_LEFT,
                Phrase("Documento Técnico de SST — Conforme Diretrizes da NR-01 / NR-12 — Confidencial e Auditável", fontFooter),
                document.left(),
                document.bottom() - 16f,
                0f
            )
            ColumnText.showTextAligned(
                cb,
                Element.ALIGN_RIGHT,
                Phrase("Página $pageNum", fontFooterPage),
                document.right(),
                document.bottom() - 16f,
                0f
            )
        }
    }

    private fun getLogoImage(context: Context): com.lowagie.text.Image? {
        return try {
            var resId = context.resources.getIdentifier("img_foco_prevencao_logo_1787517691154", "drawable", context.packageName)
            if (resId == 0) {
                resId = R.drawable.img_foco_prevencao_logo_1787517691154
            }
            if (resId != 0) {
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.resources, resId)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.setTargetSampleSize(2)
                    }
                } else {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = false
                    }
                    BitmapFactory.decodeResource(context.resources, resId, options)
                } ?: return null

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val img = com.lowagie.text.Image.getInstance(stream.toByteArray())
                img.scaleToFit(48f, 48f)
                img
            } else null
        } catch (e: Throwable) {
            null
        }
    }

    private fun getOccurrencePhotoImage(context: Context, uriString: String?): com.lowagie.text.Image? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val isLocalFile = uriString.startsWith("/") || uri.scheme == "file" || uri.scheme == null
            val localPath = if (uri.scheme == "file") uri.path ?: uriString else uriString
            val localFile = if (isLocalFile) File(localPath) else null

            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = if (localFile != null && localFile.exists()) {
                    android.graphics.ImageDecoder.createSource(localFile)
                } else {
                    android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                }
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    val reqWidth = 400
                    val reqHeight = 300
                    var sampleSize = 1
                    val w = info.size.width
                    val h = info.size.height
                    if (h > reqHeight || w > reqWidth) {
                        val halfH = h / 2
                        val halfW = w / 2
                        while ((halfH / sampleSize) >= reqHeight && (halfW / sampleSize) >= reqWidth) {
                            sampleSize *= 2
                        }
                    }
                    decoder.setTargetSampleSize(sampleSize)
                }
            } else {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val openStream = {
                    if (localFile != null && localFile.exists()) {
                        localFile.inputStream()
                    } else {
                        try { context.contentResolver.openInputStream(uri) } catch (e: Exception) { null }
                    }
                }
                openStream()?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, boundsOptions)
                } ?: return null

                val reqWidth = 400
                val reqHeight = 300
                var inSampleSize = 1
                if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                    val halfHeight = boundsOptions.outHeight / 2
                    val halfWidth = boundsOptions.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false
                }
                openStream()?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                } ?: return null
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val img = com.lowagie.text.Image.getInstance(stream.toByteArray())
            img.scaleToFit(130f, 85f)
            img
        } catch (e: Throwable) {
            null
        }
    }
}
