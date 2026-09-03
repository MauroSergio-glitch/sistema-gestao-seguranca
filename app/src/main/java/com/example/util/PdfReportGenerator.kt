package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.SafetyOccurrence
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multinational Corporate HSE/SST Executive PDF Generator.
 * Built using native Android graphics and PdfDocument (100% Android runtime compatible).
 * Renders standalone, color-coded incident cards (Green, Yellow, Orange, Red),
 * institutional FOCO NA PREVENÇÃO branding, KPI summary dashboard, and formal validation signatures (NR-01 / NR-12).
 */
object PdfReportGenerator {

    // Executive Corporate Palette (HSSE / SST Standard)
    private val COLOR_PRIMARY = Color.rgb(15, 32, 39)          // Deep Navy Slate #0F2027
    private val COLOR_NAVY_DARK = Color.rgb(24, 43, 73)        // Corporate Navy #182B49
    private val COLOR_ACCENT = Color.rgb(22, 101, 52)          // Executive Safety Green #166534
    private val COLOR_SECONDARY = Color.rgb(46, 125, 50)       // Medium Safety Green #2E7D32
    private val COLOR_LIGHT_BG = Color.rgb(248, 250, 252)      // Off-white Soft Gray #F8FAFC
    private val COLOR_ROW_ALT = Color.rgb(241, 245, 249)       // Card Header Neutral #F1F5F9
    private val COLOR_BOX_DETAIL_BG = Color.rgb(245, 247, 250) // Detail Box Soft Gray #F5F7FA
    private val COLOR_BOX_ACTION_BG = Color.rgb(240, 253, 244) // Action Box Light Emerald #F0FDF4
    private val COLOR_TEXT_DARK = Color.rgb(30, 41, 59)        // Slate Dark #1E293B
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)    // Slate Muted #64748B
    private val COLOR_BORDER = Color.rgb(226, 232, 240)        // Soft Slate Border #E2E8F0
    private val COLOR_BORDER_ACTION = Color.rgb(187, 247, 208) // Light green action border #BBF7D0
    private val COLOR_WHITE = Color.WHITE

    // Standardized 4-Level Risk Color Indicators (Green, Yellow, Orange, Red)
    data class RiskColorScheme(
        val bg: Int,
        val border: Int,
        val text: Int,
        val label: String
    )

    // 1. Green (Low / Safe)
    val RISK_LOW = RiskColorScheme(
        bg = Color.rgb(240, 253, 244),      // #F0FDF4
        border = Color.rgb(34, 197, 94),     // #22C55E
        text = Color.rgb(21, 128, 61),       // #15803D
        label = "BAIXO — VERDE"
    )

    // 2. Yellow / Amber (Medium)
    val RISK_MEDIUM = RiskColorScheme(
        bg = Color.rgb(255, 251, 235),     // #FFFBEB
        border = Color.rgb(245, 158, 11),   // #F59E0B
        text = Color.rgb(180, 83, 9),       // #B45309
        label = "MÉDIO — AMARELO"
    )

    // 3. Orange (High)
    val RISK_HIGH = RiskColorScheme(
        bg = Color.rgb(255, 247, 237),      // #FFF7ED
        border = Color.rgb(249, 115, 22),   // #F97316
        text = Color.rgb(194, 65, 12),      // #C2410C
        label = "ALTO — LARANJA"
    )

    // 4. Red (Critical)
    val RISK_CRITICAL = RiskColorScheme(
        bg = Color.rgb(254, 242, 242),      // #FEF2F2
        border = Color.rgb(239, 68, 68),    // #EF4444
        text = Color.rgb(185, 28, 28),      // #B91C1C
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

    // Standard A4 Dimensions in points (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MARGIN_TOP = 42f
    private const val MARGIN_BOTTOM = 796f
    private const val CONTENT_WIDTH = 523f

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
     * Main PDF Generation Entry Point.
     * Generates both a timestamped audit copy and the canonical 'Relatorio_SST_Atualizado.pdf'
     * while preserving all existing historical files intact.
     */
    fun generatePdfReport(
        context: Context,
        dateStr: String,
        occurrences: List<SafetyOccurrence>
    ): File {
        val sortedOccurrences = sortSequentiallyByDate(occurrences)

        val reportsDir = File(context.filesDir, "relatorios_diarios/pdf")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val emitTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val dateSanitized = dateStr.replace("/", "-")
        val timestampedFile = File(reportsDir, "Relatorio_SST_${dateSanitized}_$timestamp.pdf")
        val updatedCanonicalFile = File(reportsDir, "Relatorio_SST_Atualizado.pdf")

        try {
            OpenPdfExportService.exportReport(context, dateStr, sortedOccurrences, timestampedFile)
        } catch (e: Throwable) {
            try {
                renderNativePdfDocument(context, dateStr, emitTime, sortedOccurrences, timestampedFile)
            } catch (t: Throwable) {
                writeFallbackPdfFile(timestampedFile, dateStr, sortedOccurrences)
            }
        }

        // Copy to canonical 'Relatorio_SST_Atualizado.pdf' preserving original timestamped files
        try {
            if (timestampedFile.exists() && timestampedFile.length() > 0) {
                timestampedFile.copyTo(updatedCanonicalFile, overwrite = true)
            }
        } catch (ignored: Exception) {}

        return if (updatedCanonicalFile.exists() && updatedCanonicalFile.length() > 0) {
            updatedCanonicalFile
        } else {
            timestampedFile
        }
    }

    /**
     * Explicit helper to generate the updated report file with the requested name.
     */
    fun generateUpdatedPdfReport(
        context: Context,
        dateStr: String,
        occurrences: List<SafetyOccurrence>
    ): File = generatePdfReport(context, dateStr, occurrences)

    private fun renderNativePdfDocument(
        context: Context,
        dateStr: String,
        emitTime: String,
        sortedOccurrences: List<SafetyOccurrence>,
        pdfFile: File
    ) {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        var currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var currentY = MARGIN_TOP

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            val linePaint = Paint().apply {
                color = COLOR_ACCENT
                strokeWidth = 1.2f
                style = Paint.Style.STROKE
            }
            // Top Header Line
            c.drawLine(MARGIN_LEFT, 32f, MARGIN_RIGHT, 32f, linePaint)

            val headerTitlePaint = Paint().apply {
                color = COLOR_NAVY_DARK
                textSize = 7.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            c.drawText("FOCO NA PREVENÇÃO SST / GESTÃO DE SEGURANÇA E SAÚDE OCUPACIONAL — RELATÓRIO TÉCNICO", MARGIN_LEFT, 26f, headerTitlePaint)

            val headerDatePaint = Paint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 7.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            c.drawText("Data Base: $dateStr", MARGIN_RIGHT, 26f, headerDatePaint)

            // Bottom Footer Line
            linePaint.color = COLOR_BORDER
            linePaint.strokeWidth = 0.8f
            c.drawLine(MARGIN_LEFT, 804f, MARGIN_RIGHT, 804f, linePaint)

            val footerTextPaint = Paint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 7f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            c.drawText("Documento Técnico de SST — Conforme Diretrizes da NR-01 / NR-12 — Confidencial e Auditável", MARGIN_LEFT, 816f, footerTextPaint)

            val pageNumPaint = Paint().apply {
                color = COLOR_NAVY_DARK
                textSize = 7f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            c.drawText("Página $pNum", MARGIN_RIGHT, 816f, pageNumPaint)
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > MARGIN_BOTTOM) {
                drawHeaderAndFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)

                pageNumber++
                currentPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                currentY = MARGIN_TOP + 12f
            }
        }

        // ==========================================
        // 1. HEADER BRANDING & LOGO
        // ==========================================
        val logoBitmap = loadCompanyLogoBitmap(context)
        val headerHeight = 56f
        ensureSpace(headerHeight)

        // Draw Logo Box
        if (logoBitmap != null) {
            val logoRect = RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + 50f, currentY + 50f)
            val logoBgPaint = Paint().apply {
                color = COLOR_LIGHT_BG
                style = Paint.Style.FILL
            }
            val logoBorderPaint = Paint().apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawRoundRect(logoRect, 6f, 6f, logoBgPaint)
            canvas.drawRoundRect(logoRect, 6f, 6f, logoBorderPaint)

            val innerLogoRect = RectF(MARGIN_LEFT + 4f, currentY + 4f, MARGIN_LEFT + 46f, currentY + 46f)
            canvas.drawBitmap(logoBitmap, null, innerLogoRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Draw Title & Subtitle
        val titleX = if (logoBitmap != null) MARGIN_LEFT + 58f else MARGIN_LEFT
        val titlePaint = Paint().apply {
            color = COLOR_NAVY_DARK
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("RELATÓRIO TÉCNICO DE INVESTIGAÇÃO DE OCORRÊNCIAS SST", titleX, currentY + 16f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = COLOR_ACCENT
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("SISTEMA DE GESTÃO EM SEGURANÇA E SAÚDE OCUPACIONAL", titleX, currentY + 29f, subtitlePaint)

        val metaSubPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Registro Oficial de Quase-Acidentes, Atos Inseguros e Condições de Risco (NR-01 / NR-12)", titleX, currentY + 41f, metaSubPaint)

        // Metadata box on right
        val metaCardWidth = 142f
        val metaCardLeft = MARGIN_RIGHT - metaCardWidth
        val metaCardRect = RectF(metaCardLeft, currentY, MARGIN_RIGHT, currentY + 50f)
        val metaBgPaint = Paint().apply {
            color = COLOR_LIGHT_BG
            style = Paint.Style.FILL
        }
        val metaBorderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(metaCardRect, 4f, 4f, metaBgPaint)
        canvas.drawRoundRect(metaCardRect, 4f, 4f, metaBorderPaint)

        val metaLabelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val metaValPaint = Paint().apply {
            color = COLOR_NAVY_DARK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("EMISSÃO:", metaCardLeft + 8f, currentY + 14f, metaLabelPaint)
        canvas.drawText(emitTime, metaCardLeft + 52f, currentY + 14f, metaValPaint)

        canvas.drawText("DATA BASE:", metaCardLeft + 8f, currentY + 27f, metaLabelPaint)
        canvas.drawText(dateStr, metaCardLeft + 52f, currentY + 27f, metaValPaint)

        canvas.drawText("VOLUME:", metaCardLeft + 8f, currentY + 40f, metaLabelPaint)
        metaValPaint.color = COLOR_ACCENT
        canvas.drawText("${sortedOccurrences.size} registro(s)", metaCardLeft + 52f, currentY + 40f, metaValPaint)

        currentY += 58f

        // Horizontal Divider Accent (Navy + Green Accent Line)
        val divPaint = Paint().apply {
            color = COLOR_NAVY_DARK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY, divPaint)
        divPaint.color = COLOR_ACCENT
        divPaint.strokeWidth = 1f
        canvas.drawLine(MARGIN_LEFT, currentY + 2.5f, MARGIN_RIGHT, currentY + 2.5f, divPaint)
        currentY += 12f

        // ==========================================
        // 2. EXECUTIVE DASHBOARD / KPI SUMMARY (5 CARDS)
        // ==========================================
        val criticalCount = sortedOccurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.CRITICAL }
        val highCount = sortedOccurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.HIGH }
        val medCount = sortedOccurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.MEDIUM }
        val lowCount = sortedOccurrences.count { SstManagementEngine.getEffectiveRiskLevel(it) == SstManagementEngine.RiskLevel.LOW }

        val kpiHeight = 42f
        ensureSpace(kpiHeight)

        val kpiSpacing = 6f
        val kpiWidth = (CONTENT_WIDTH - (kpiSpacing * 4)) / 5f

        val kpis = listOf(
            KpiItem("TOTAL RELATOS", "${sortedOccurrences.size}", COLOR_LIGHT_BG, COLOR_NAVY_DARK, COLOR_BORDER),
            KpiItem("CRÍTICO (VERMELHO)", "$criticalCount", RISK_CRITICAL.bg, RISK_CRITICAL.text, RISK_CRITICAL.border),
            KpiItem("ALTO (LARANJA)", "$highCount", RISK_HIGH.bg, RISK_HIGH.text, RISK_HIGH.border),
            KpiItem("MÉDIO (AMARELO)", "$medCount", RISK_MEDIUM.bg, RISK_MEDIUM.text, RISK_MEDIUM.border),
            KpiItem("BAIXO (VERDE)", "$lowCount", RISK_LOW.bg, RISK_LOW.text, RISK_LOW.border)
        )

        var kpiX = MARGIN_LEFT
        for (kpi in kpis) {
            val kpiRect = RectF(kpiX, currentY, kpiX + kpiWidth, currentY + kpiHeight)
            val kpiBg = Paint().apply { color = kpi.bgColor; style = Paint.Style.FILL }
            val kpiBorder = Paint().apply { color = kpi.borderColor; style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(kpiRect, 4f, 4f, kpiBg)
            canvas.drawRoundRect(kpiRect, 4f, 4f, kpiBorder)

            val kpiTitlePaint = Paint().apply {
                color = kpi.textColor
                textSize = 5.8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(kpi.label, kpiX + (kpiWidth / 2f), currentY + 13f, kpiTitlePaint)

            val kpiValPaint = Paint().apply {
                color = kpi.textColor
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(kpi.value, kpiX + (kpiWidth / 2f), currentY + 33f, kpiValPaint)

            kpiX += kpiWidth + kpiSpacing
        }

        currentY += kpiHeight + 14f

        // ==========================================
        // 3. SECTION 1: DETAILED OCCURRENCES
        // ==========================================
        ensureSpace(24f)
        val sectionBannerPaint = Paint().apply { color = COLOR_NAVY_DARK; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 19f), 3f, 3f, sectionBannerPaint)

        val sectionTextPaint = Paint().apply {
            color = COLOR_WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("1. REGISTROS TÉCNICOS DETALHADOS DAS OCORRÊNCIAS", MARGIN_LEFT + 10f, currentY + 13f, sectionTextPaint)
        currentY += 26f

        if (sortedOccurrences.isEmpty()) {
            ensureSpace(38f)
            val emptyRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 38f)
            val emptyBg = Paint().apply { color = COLOR_LIGHT_BG; style = Paint.Style.FILL }
            val emptyBorder = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
            canvas.drawRoundRect(emptyRect, 4f, 4f, emptyBg)
            canvas.drawRoundRect(emptyRect, 4f, 4f, emptyBorder)

            val emptyTextPaint = Paint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Nenhuma ocorrência registrada para o período selecionado.", MARGIN_LEFT + (CONTENT_WIDTH / 2f), currentY + 22f, emptyTextPaint)
            currentY += 48f
        } else {
            for ((index, item) in sortedOccurrences.withIndex()) {
                currentY = drawOccurrenceCard(context, canvas, currentY, index + 1, item) { needed ->
                    ensureSpace(needed)
                }
            }
        }

        // ==========================================
        // 4. SECTION 2: FORMAL SIGNATURES & VALIDATION
        // ==========================================
        val signaturesBlockHeight = 150f
        ensureSpace(signaturesBlockHeight)

        val sec2BannerPaint = Paint().apply { color = COLOR_ACCENT; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 19f), 3f, 3f, sec2BannerPaint)
        canvas.drawText("2. TERMO DE ENCERRAMENTO E VALIDAÇÃO TÉCNICA FORMAL", MARGIN_LEFT + 10f, currentY + 13f, sectionTextPaint)
        currentY += 25f

        // Compliance Declaration text box
        val declBoxRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 28f)
        val declBgPaint = Paint().apply { color = COLOR_LIGHT_BG; style = Paint.Style.FILL }
        val declBorderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        canvas.drawRoundRect(declBoxRect, 3f, 3f, declBgPaint)
        canvas.drawRoundRect(declBoxRect, 3f, 3f, declBorderPaint)

        val declTextPaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(
            "Certificamos para os devidos fins de conformidade com as Normas Regulamentadoras (NR-01/NR-12) que as ocorrências e",
            MARGIN_LEFT + 8f,
            currentY + 11f,
            declTextPaint
        )
        canvas.drawText(
            "ações corretivas foram apuradas tecnicamente, tendo sido direcionadas aos responsáveis para imediato cumprimento.",
            MARGIN_LEFT + 8f,
            currentY + 21f,
            declTextPaint
        )
        currentY += 36f

        // Dual Signature Boxes
        val colGap = 16f
        val boxWidth = (CONTENT_WIDTH - colGap) / 2f
        val boxHeight = 78f

        // Box 1: Responsável Técnico SST / TST
        val box1Left = MARGIN_LEFT
        val box1Rect = RectF(box1Left, currentY, box1Left + boxWidth, currentY + boxHeight)
        val sigBoxBg = Paint().apply { color = COLOR_WHITE; style = Paint.Style.FILL }
        val sigBoxBorder = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        canvas.drawRoundRect(box1Rect, 4f, 4f, sigBoxBg)
        canvas.drawRoundRect(box1Rect, 4f, 4f, sigBoxBorder)

        // Accent top strip for box 1
        val stripPaint = Paint().apply { color = COLOR_ACCENT; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(box1Left, currentY, box1Left + boxWidth, currentY + 3f), 4f, 4f, stripPaint)

        val sigLinePaint = Paint().apply {
            color = COLOR_TEXT_DARK
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val sigLineY1 = currentY + 32f
        canvas.drawLine(box1Left + 16f, sigLineY1, box1Left + boxWidth - 16f, sigLineY1, sigLinePaint)

        val sigRolePaint = Paint().apply {
            color = COLOR_NAVY_DARK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("RESPONSÁVEL TÉCNICO DE SEGURANÇA (SST)", box1Left + (boxWidth / 2f), currentY + 43f, sigRolePaint)

        val sigFieldPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 6.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        canvas.drawText("Nome: _____________________________________", box1Left + 12f, currentY + 56f, sigFieldPaint)
        canvas.drawText("Reg. Profissional / MTE: _________  Data: ___/___/______", box1Left + 12f, currentY + 68f, sigFieldPaint)

        // Box 2: Supervisor / Gestor da Área
        val box2Left = MARGIN_LEFT + boxWidth + colGap
        val box2Rect = RectF(box2Left, currentY, box2Left + boxWidth, currentY + boxHeight)
        canvas.drawRoundRect(box2Rect, 4f, 4f, sigBoxBg)
        canvas.drawRoundRect(box2Rect, 4f, 4f, sigBoxBorder)

        // Accent top strip for box 2
        stripPaint.color = COLOR_NAVY_DARK
        canvas.drawRoundRect(RectF(box2Left, currentY, box2Left + boxWidth, currentY + 3f), 4f, 4f, stripPaint)

        canvas.drawLine(box2Left + 16f, sigLineY1, box2Left + boxWidth - 16f, sigLineY1, sigLinePaint)
        canvas.drawText("SUPERVISOR / GESTOR DA ÁREA ENVOLVIDA", box2Left + (boxWidth / 2f), currentY + 43f, sigRolePaint)
        canvas.drawText("Nome: _____________________________________", box2Left + 12f, currentY + 56f, sigFieldPaint)
        canvas.drawText("Cargo / Setor: _________________  Data: ___/___/______", box2Left + 12f, currentY + 68f, sigFieldPaint)

        currentY += boxHeight + 10f

        // Bottom generation stamp
        val stampPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 6.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "Documento gerado e validado eletronicamente através do aplicativo oficial Foco na Prevenção SST em $emitTime",
            MARGIN_LEFT + (CONTENT_WIDTH / 2f),
            currentY + 6f,
            stampPaint
        )

        // Draw final page header & footer
        drawHeaderAndFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        // Write to output file
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()
    }

    private fun writeFallbackPdfFile(file: File, dateStr: String, occurrences: List<SafetyOccurrence>) {
        val content = buildString {
            append("BT\n")
            append("/F1 12 Tf\n")
            append("40 800 Td\n")
            append("(FOCO NA PREVENÇÃO SST - RELATORIO TECNICO DE INVESTIGACAO - $dateStr) Tj\n")
            append("/F1 9 Tf\n")
            append("0 -20 Td\n")
            append("(Total de Ocorrencias: ${occurrences.size} | Conforme Diretrizes da NR-01 / NR-12) Tj\n")
            occurrences.take(15).forEach { occ ->
                append("0 -18 Td\n")
                val sanitizedDesc = occ.ocorrencia.replace("(", "").replace(")", "")
                val sanitizedLocal = occ.local.replace("(", "").replace(")", "")
                val sanitizedEmp = occ.nomeColaborador.ifBlank { "N/A" }.replace("(", "").replace(")", "")
                append("(${occ.data} ${occ.hora} - $sanitizedLocal: $sanitizedDesc [Risco: ${occ.risco}] - Colab: $sanitizedEmp) Tj\n")
            }
            append("0 -30 Td\n")
            append("(--------------------------------------------------------------------------------) Tj\n")
            append("0 -15 Td\n")
            append("(ASSINATURAS FORMAIS DE VALIDACAO SST:) Tj\n")
            append("0 -20 Td\n")
            append("(1. Responsavel Tecnico SST: _______________________ Reg. MTE: __________) Tj\n")
            append("0 -20 Td\n")
            append("(2. Supervisor / Gestor de Area: ___________________ Data: ___/___/______) Tj\n")
            append("ET\n")
        }

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj <</Type /Catalog /Pages 2 0 R>> endobj\n")
        sb.append("2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1>> endobj\n")
        sb.append("3 0 obj <</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources <</Font <</F1 4 0 R>>>> /Contents 5 0 R>> endobj\n")
        sb.append("4 0 obj <</Type /Font /Subtype /Type1 /BaseFont /Helvetica>> endobj\n")
        sb.append("5 0 obj <</Length ${content.length}>> stream\n")
        sb.append(content)
        sb.append("endstream\nendobj\n")
        sb.append("xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000230 00000 n \n0000000297 00000 n \n")
        sb.append("trailer <</Size 6 /Root 1 0 R>>\nstartxref\n")
        sb.append("450\n%%EOF\n")

        file.writeText(sb.toString(), Charsets.ISO_8859_1)
    }

    private data class KpiItem(
        val label: String,
        val value: String,
        val bgColor: Int,
        val textColor: Int,
        val borderColor: Int
    )

    private fun drawOccurrenceCard(
        context: Context,
        canvas: Canvas,
        startY: Float,
        itemNum: Int,
        item: SafetyOccurrence,
        checkSpace: (Float) -> Unit
    ): Float {
        var y = startY

        val effectiveLevel = SstManagementEngine.getEffectiveRiskLevel(item)
        val riskScheme = getRiskScheme(effectiveLevel)
        val riskBg = riskScheme.bg
        val riskBorder = riskScheme.border
        val riskText = riskScheme.text

        val textPaint = TextPaint().apply {
            color = COLOR_TEXT_DARK
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val innerBoxWidth = (CONTENT_WIDTH - 24f).toInt()
        val relatoLayout = createStaticLayout(item.relatoDetalhes.ifBlank { "Sem relato ou circunstâncias detalhadas informadas." }, textPaint, innerBoxWidth)
        val acaoLayout = createStaticLayout(item.acaoTomada.ifBlank { "Nenhuma ação imediata ou plano de contenção registrado." }, textPaint, innerBoxWidth)

        val photoBitmap = if (!item.fotoUri.isNullOrBlank()) {
            loadPhotoBitmap(context, Uri.parse(item.fotoUri))
        } else null

        val photoHeight = if (photoBitmap != null) 92f else 0f
        val cardEstimatedHeight = 138f + relatoLayout.height + acaoLayout.height + photoHeight

        // Guarantees continuous block integrity without awkward breaks across page boundaries
        checkSpace(cardEstimatedHeight)

        // Outer Card Box
        val cardRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + cardEstimatedHeight)
        val cardBgPaint = Paint().apply { color = COLOR_WHITE; style = Paint.Style.FILL }
        val cardBorderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        canvas.drawRoundRect(cardRect, 5f, 5f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 5f, 5f, cardBorderPaint)

        // Left severity stripe
        val stripePaint = Paint().apply { color = riskBorder; style = Paint.Style.FILL }
        val stripeRect = RectF(MARGIN_LEFT, y, MARGIN_LEFT + 4f, y + cardEstimatedHeight)
        canvas.drawRoundRect(stripeRect, 4f, 4f, stripePaint)

        // 1. CARD HEADER STRIP
        val headerRect = RectF(MARGIN_LEFT + 4f, y, MARGIN_RIGHT, y + 24f)
        val headerBg = Paint().apply { color = COLOR_ROW_ALT; style = Paint.Style.FILL }
        canvas.drawRect(headerRect, headerBg)

        val cardTitlePaint = Paint().apply {
            color = COLOR_NAVY_DARK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerTitle = "#${String.format(Locale.getDefault(), "%02d", itemNum)} | ${item.data} às ${item.hora} — ${item.ocorrencia.ifBlank { "Relato SST" }}"
        canvas.drawText(headerTitle, MARGIN_LEFT + 12f, y + 16f, cardTitlePaint)

        // Risk badge on right
        val riskBadgeWidth = 96f
        val riskBadgeRect = RectF(MARGIN_RIGHT - riskBadgeWidth - 6f, y + 4f, MARGIN_RIGHT - 6f, y + 20f)
        val rBg = Paint().apply { color = riskBg; style = Paint.Style.FILL }
        val rBorder = Paint().apply { color = riskBorder; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        canvas.drawRoundRect(riskBadgeRect, 3f, 3f, rBg)
        canvas.drawRoundRect(riskBadgeRect, 3f, 3f, rBorder)

        val rTextPaint = Paint().apply {
            color = riskText
            textSize = 6.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("RISCO: ${effectiveLevel.fullLabel}", riskBadgeRect.centerX(), y + 15f, rTextPaint)

        y += 28f

        // 2. TWO-COLUMN STRUCTURED GRID
        val labelPaint = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val col1 = MARGIN_LEFT + 12f
        val col2 = MARGIN_LEFT + 265f

        // Row 1: Colaborador e Setor
        val empText = if (item.nomeColaborador.isNotBlank()) "${item.nomeColaborador} (RE: ${item.registro.ifBlank { "N/A" }})" else "Não identificado / Geral"
        canvas.drawText("Colaborador:", col1, y + 9f, labelPaint)
        canvas.drawText(empText, col1 + 58f, y + 9f, valPaint)

        canvas.drawText("Setor / Área:", col2, y + 9f, labelPaint)
        canvas.drawText(item.setor.ifBlank { "Não informado" }, col2 + 54f, y + 9f, valPaint)
        y += 14f

        // Row 2: Local e Classificação
        canvas.drawText("Local Específico:", col1, y + 9f, labelPaint)
        canvas.drawText(item.local.ifBlank { "Não informado" }, col1 + 68f, y + 9f, valPaint)

        canvas.drawText("Classificação:", col2, y + 9f, labelPaint)
        canvas.drawText(item.classificacao.ifBlank { "SST" }, col2 + 58f, y + 9f, valPaint)
        y += 14f

        // Row 3: Causa e Clima
        canvas.drawText("Causa / Fator:", col1, y + 9f, labelPaint)
        canvas.drawText(item.causa.ifBlank { "A apurar" }, col1 + 62f, y + 9f, valPaint)

        canvas.drawText("Condição Clima:", col2, y + 9f, labelPaint)
        canvas.drawText(item.clima.ifBlank { "Normal" }, col2 + 66f, y + 9f, valPaint)
        y += 17f

        // 3. NARRATIVE / DETALHES BOX
        val boxWidth = CONTENT_WIDTH - 20f
        val relatoBoxHeight = relatoLayout.height + 18f
        val relatoBoxRect = RectF(MARGIN_LEFT + 10f, y, MARGIN_LEFT + 10f + boxWidth, y + relatoBoxHeight)
        val boxBgPaint = Paint().apply { color = COLOR_BOX_DETAIL_BG; style = Paint.Style.FILL }
        val boxBorderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.6f }
        canvas.drawRoundRect(relatoBoxRect, 3f, 3f, boxBgPaint)
        canvas.drawRoundRect(relatoBoxRect, 3f, 3f, boxBorderPaint)

        val subHeaderPaint = Paint().apply {
            color = COLOR_NAVY_DARK
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("DESCRIÇÃO CIRCUNSTANCIADA DO FATO / RELATO:", MARGIN_LEFT + 14f, y + 9f, subHeaderPaint)

        canvas.save()
        canvas.translate(MARGIN_LEFT + 14f, y + 13f)
        relatoLayout.draw(canvas)
        canvas.restore()
        y += relatoBoxHeight + 6f

        // 4. ACTION / MEDIDA ADOTADA BOX
        val acaoBoxHeight = acaoLayout.height + 18f
        val acaoBoxRect = RectF(MARGIN_LEFT + 10f, y, MARGIN_LEFT + 10f + boxWidth, y + acaoBoxHeight)
        boxBgPaint.color = COLOR_BOX_ACTION_BG
        boxBorderPaint.color = COLOR_BORDER_ACTION
        canvas.drawRoundRect(acaoBoxRect, 3f, 3f, boxBgPaint)
        canvas.drawRoundRect(acaoBoxRect, 3f, 3f, boxBorderPaint)

        // Left accent bar for action box
        val actStripe = Paint().apply { color = COLOR_ACCENT; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(MARGIN_LEFT + 10f, y, MARGIN_LEFT + 12.5f, y + acaoBoxHeight), 2f, 2f, actStripe)

        subHeaderPaint.color = COLOR_ACCENT
        canvas.drawText("PLANO DE AÇÃO IMEDIATA E CONTENÇÃO PREVENTIVA ADOTADA:", MARGIN_LEFT + 16f, y + 9f, subHeaderPaint)

        canvas.save()
        canvas.translate(MARGIN_LEFT + 16f, y + 13f)
        acaoLayout.draw(canvas)
        canvas.restore()
        y += acaoBoxHeight + 6f

        // 5. PHOTO IF ATTACHED
        if (photoBitmap != null) {
            val photoBoxRect = RectF(MARGIN_LEFT + 10f, y, MARGIN_LEFT + 10f + boxWidth, y + 84f)
            boxBgPaint.color = COLOR_LIGHT_BG
            boxBorderPaint.color = COLOR_BORDER
            canvas.drawRoundRect(photoBoxRect, 3f, 3f, boxBgPaint)
            canvas.drawRoundRect(photoBoxRect, 3f, 3f, boxBorderPaint)

            subHeaderPaint.color = COLOR_TEXT_MUTED
            canvas.drawText("EVIDÊNCIA FOTOGRÁFICA / REGISTRO VISUAL ANEXADO:", MARGIN_LEFT + 14f, y + 10f, subHeaderPaint)

            val photoW = 110f
            val photoH = 64f
            val photoRect = RectF(MARGIN_LEFT + 14f, y + 14f, MARGIN_LEFT + 14f + photoW, y + 14f + photoH)
            canvas.drawBitmap(photoBitmap, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

            val photoCaptionPaint = Paint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 6.8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("Registro de campo capturado no ato da inspeção SST.", MARGIN_LEFT + 134f, y + 36f, photoCaptionPaint)
            canvas.drawText("Evidência associada à Ocorrência #${String.format(Locale.getDefault(), "%02d", itemNum)}", MARGIN_LEFT + 134f, y + 48f, photoCaptionPaint)

            y += 88f
        }

        return y + 6f
    }

    private fun createStaticLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
        }
    }

    private fun loadCompanyLogoBitmap(context: Context): Bitmap? {
        return try {
            var resId = context.resources.getIdentifier("img_foco_prevencao_logo_1787517691154", "drawable", context.packageName)
            if (resId == 0) {
                resId = R.drawable.img_foco_prevencao_logo_1787517691154
            }
            if (resId != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.resources, resId)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.setTargetSampleSize(2)
                    }
                } else {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = false
                    }
                    BitmapFactory.decodeResource(context.resources, resId, options)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadPhotoBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val uriStr = uri.toString()
            val isLocalFile = uriStr.startsWith("/") || uri.scheme == "file" || uri.scheme == null
            val localPath = if (uri.scheme == "file") uri.path ?: uriStr else uriStr
            val localFile = if (isLocalFile) File(localPath) else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = if (localFile != null && localFile.exists()) {
                    ImageDecoder.createSource(localFile)
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val reqWidth = 480
                    val reqHeight = 360
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
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
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

                val reqWidth = 480
                val reqHeight = 360
                var inSampleSize = 1
                if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                    val halfHeight: Int = boundsOptions.outHeight / 2
                    val halfWidth: Int = boundsOptions.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                openStream()?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getShareablePdfUri(context: Context, pdfFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }
}
