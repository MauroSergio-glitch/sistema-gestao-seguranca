package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SafetyOccurrence
import com.example.ui.theme.SafetyAlertRed
import com.example.ui.theme.SafetyGoldSecondary
import com.example.ui.theme.SafetyGreenPrimary
import com.example.ui.viewmodel.SafetyViewModel
import com.example.util.SstManagementEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PowerBiDashboardView(
    viewModel: SafetyViewModel,
    occurrences: List<SafetyOccurrence>,
    onNavigateToAlerts: () -> Unit,
    onNavigateToWorkflow: () -> Unit,
    onNavigateToForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Slicers / Interactive Filters (Estilo Power BI)
    var selectedPeriodFilter by remember { mutableStateOf("Todos") } // "Todos", "Últimos 30 Dias", "Últimos 7 Dias"
    var selectedRiskFilter by remember { mutableStateOf<String?>(null) } // null, "Crítico", "Alto", "Médio", "Baixo"
    var selectedSectorFilter by remember { mutableStateOf<String?>(null) }

    // Dynamic filtering of occurrences based on active slicers
    val filteredOccurrences = remember(occurrences, selectedPeriodFilter, selectedRiskFilter, selectedSectorFilter) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        occurrences.filter { occ ->
            // Period filter
            val periodMatch = when (selectedPeriodFilter) {
                "Últimos 7 Dias" -> {
                    try {
                        val d = dateFormat.parse(occ.data)
                        d != null && (now - d.time) <= (7L * 24 * 3600 * 1000)
                    } catch (e: Exception) { true }
                }
                "Últimos 30 Dias" -> {
                    try {
                        val d = dateFormat.parse(occ.data)
                        d != null && (now - d.time) <= (30L * 24 * 3600 * 1000)
                    } catch (e: Exception) { true }
                }
                else -> true
            }

            // Risk filter
            val riskMatch = if (selectedRiskFilter == null) true else {
                occ.risco.contains(selectedRiskFilter!!, ignoreCase = true)
            }

            // Sector filter
            val sectorMatch = if (selectedSectorFilter == null) true else {
                occ.setor.equals(selectedSectorFilter, ignoreCase = true) ||
                occ.local.equals(selectedSectorFilter, ignoreCase = true)
            }

            periodMatch && riskMatch && sectorMatch
        }
    }

    // Available sectors for slicer
    val availableSectors = remember(occurrences) {
        occurrences.mapNotNull {
            val s = it.setor.trim()
            if (s.isNotBlank()) s else null
        }.distinct().sorted()
    }

    // Key Performance Metrics Calculation
    val totalFiltered = filteredOccurrences.size
    val totalGlobal = occurrences.size

    val criticosCount = filteredOccurrences.count { it.risco.contains("Crítico", true) }
    val altosCount = filteredOccurrences.count { it.risco.contains("Alto", true) }
    val mediosCount = filteredOccurrences.count { it.risco.contains("Médio", true) }
    val baixosCount = filteredOccurrences.count { it.risco.contains("Baixo", true) }

    val completedCapa = filteredOccurrences.count {
        it.statusAcao.equals("Concluído", true) || it.statusAcao.equals("Eficaz", true)
    }
    val capaResolutionRate = if (totalFiltered > 0) (completedCapa * 100f / totalFiltered).toInt() else 100

    // Conformity Index (GRO / NR-01): based on ratio of low/medium risk + closed CAPA actions
    val conformityScore = if (totalFiltered > 0) {
        val nonCriticalRatio = (mediosCount + baixosCount).toFloat() / totalFiltered
        val resolutionRatio = completedCapa.toFloat() / totalFiltered
        ((nonCriticalRatio * 0.5f + resolutionRatio * 0.5f) * 100).toInt().coerceIn(0, 100)
    } else 100

    // Active Critical Occurrences
    val activeCriticals = filteredOccurrences.filter {
        (it.risco.contains("Crítico", true) || it.risco.contains("Alto", true)) &&
        !it.statusAcao.equals("Concluído", true) && !it.statusAcao.equals("Eficaz", true)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ========================================================
        // 1. POWER BI EXECUTIVE TOP BANNER & SLICERS
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("powerbi_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A) // Dark slate Power BI theme
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "PAINEL GERENCIAL POWER BI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                )
                            }
                            Text(
                                text = "Analytics em Tempo Real • NR-01 & GRO",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "$totalFiltered / $totalGlobal reg.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // Power BI Slicers / Interactive Filters Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Segmentação de Dados (Slicers):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        if (selectedPeriodFilter != "Todos" || selectedRiskFilter != null || selectedSectorFilter != null) {
                            Text(
                                text = "Limpar Filtros",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        selectedPeriodFilter = "Todos"
                                        selectedRiskFilter = null
                                        selectedSectorFilter = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Period Slicers
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Todos", "Últimos 30 Dias", "Últimos 7 Dias").forEach { period ->
                            val isSelected = selectedPeriodFilter == period
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { selectedPeriodFilter = period }
                            ) {
                                Text(
                                    text = period,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Risk Slicers
                        listOf("Crítico" to Color(0xFFEF4444), "Alto" to Color(0xFFF97316), "Médio" to Color(0xFFF59E0B), "Baixo" to Color(0xFF22C55E)).forEach { (riskName, color) ->
                            val isSelected = selectedRiskFilter == riskName
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) color else Color(0xFF1E293B),
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.clickable {
                                    selectedRiskFilter = if (isSelected) null else riskName
                                }
                            ) {
                                Text(
                                    text = riskName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else color,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // 2. POWER BI KPI TILES (Métricas de Desempenho & Conformidade)
        // ========================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PowerBiKpiCard(
                title = "Conformidade SST",
                value = "$conformityScore%",
                statusLabel = if (conformityScore >= 80) "Excelente" else "Atenção",
                statusColor = if (conformityScore >= 80) Color(0xFF16A34A) else Color(0xFFD97706),
                subtitle = "Índice Geral GRO",
                icon = Icons.Default.Shield,
                modifier = Modifier.weight(1f).testTag("kpi_conformity_score")
            )

            PowerBiKpiCard(
                title = "Resolução CAPA",
                value = "$capaResolutionRate%",
                statusLabel = "Meta: ≥ 90%",
                statusColor = Color(0xFF0284C7),
                subtitle = "$completedCapa concluídas",
                icon = Icons.Default.CheckCircle,
                onClick = onNavigateToWorkflow,
                modifier = Modifier.weight(1f).testTag("kpi_capa_rate")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PowerBiKpiCard(
                title = "Alertas Ativos",
                value = "${activeCriticals.size}",
                statusLabel = if (activeCriticals.isNotEmpty()) "Ação Imediata" else "Controlado",
                statusColor = if (activeCriticals.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF16A34A),
                subtitle = "Crítico e Alto",
                icon = Icons.Default.ReportProblem,
                onClick = onNavigateToAlerts,
                modifier = Modifier.weight(1f).testTag("kpi_active_alerts")
            )

            PowerBiKpiCard(
                title = "Total Selecionado",
                value = "$totalFiltered",
                statusLabel = "${((totalFiltered.toFloat() / totalGlobal.coerceAtLeast(1)) * 100).toInt()}% do total",
                statusColor = Color(0xFF64748B),
                subtitle = "Ocorrências SST",
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f).testTag("kpi_filtered_total")
            )
        }

        // ========================================================
        // 3. GRÁFICO INTERATIVO DE ROSCA / DONUT (Distribuição de Riscos)
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_interactive_donut"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Distribuição de Riscos (Power BI Donut)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Interativo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Custom Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val animatedCriticos by animateFloatAsState(criticosCount.toFloat(), tween(600), label = "crit")
                        val animatedAltos by animateFloatAsState(altosCount.toFloat(), tween(600), label = "alt")
                        val animatedMedios by animateFloatAsState(mediosCount.toFloat(), tween(600), label = "med")
                        val animatedBaixos by animateFloatAsState(baixosCount.toFloat(), tween(600), label = "baix")

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 24.dp.toPx()
                            val total = (animatedCriticos + animatedAltos + animatedMedios + animatedBaixos).coerceAtLeast(1f)
                            var startAngle = -90f

                            val slices = listOf(
                                animatedCriticos to Color(0xFFEF4444),
                                animatedAltos to Color(0xFFF97316),
                                animatedMedios to Color(0xFFF59E0B),
                                animatedBaixos to Color(0xFF22C55E)
                            )

                            slices.forEach { (count, color) ->
                                val sweepAngle = (count / total) * 360f
                                if (sweepAngle > 0f) {
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }

                        // Central Label
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalFiltered",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Interactive Legend
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        DonutLegendItem(
                            label = "Crítico",
                            count = criticosCount,
                            total = totalFiltered,
                            color = Color(0xFFEF4444),
                            isSelected = selectedRiskFilter == "Crítico",
                            onClick = { selectedRiskFilter = if (selectedRiskFilter == "Crítico") null else "Crítico" }
                        )
                        DonutLegendItem(
                            label = "Alto",
                            count = altosCount,
                            total = totalFiltered,
                            color = Color(0xFFF97316),
                            isSelected = selectedRiskFilter == "Alto",
                            onClick = { selectedRiskFilter = if (selectedRiskFilter == "Alto") null else "Alto" }
                        )
                        DonutLegendItem(
                            label = "Médio",
                            count = mediosCount,
                            total = totalFiltered,
                            color = Color(0xFFF59E0B),
                            isSelected = selectedRiskFilter == "Médio",
                            onClick = { selectedRiskFilter = if (selectedRiskFilter == "Médio") null else "Médio" }
                        )
                        DonutLegendItem(
                            label = "Baixo",
                            count = baixosCount,
                            total = totalFiltered,
                            color = Color(0xFF22C55E),
                            isSelected = selectedRiskFilter == "Baixo",
                            onClick = { selectedRiskFilter = if (selectedRiskFilter == "Baixo") null else "Baixo" }
                        )
                    }
                }
            }
        }

        // ========================================================
        // 4. MATRIZ DE RISCO 4x4 (HEATMAP INTERATIVO ESTILO POWER BI)
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_risk_matrix_heatmap"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Matriz de Riscos 4x4 (Heatmap NR-01)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Cruzamento Probabilidade × Severidade para priorização de tratativa imediata:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 4x4 Heatmap Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header row (Probabilidade 1 a 4)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1.2f).height(24.dp), contentAlignment = Alignment.Center) {
                            Text("Sev \\ Prob", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        listOf("1 - Rara", "2 - Baixa", "3 - Média", "4 - Alta").forEach { pLabel ->
                            Box(modifier = Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                                Text(pLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }

                    // Rows from Severity 4 (Catastrófica) down to 1 (Leve)
                    val severities = listOf(
                        4 to "4 - Crítica",
                        3 to "3 - Severa",
                        2 to "2 - Moderada",
                        1 to "1 - Leve"
                    )

                    severities.forEach { (sev, sevLabel) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier.weight(1.2f).height(38.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(sevLabel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            for (prob in 1..4) {
                                val score = prob * sev
                                val assessment = SstManagementEngine.calculateRiskMatrix(prob, sev)
                                val cellCount = remember(filteredOccurrences, score) {
                                    filteredOccurrences.count { occ ->
                                        // Map occ.risco to score range
                                        when (assessment.level) {
                                            SstManagementEngine.RiskLevel.CRITICAL -> occ.risco.contains("Crítico", true)
                                            SstManagementEngine.RiskLevel.HIGH -> occ.risco.contains("Alto", true)
                                            SstManagementEngine.RiskLevel.MEDIUM -> occ.risco.contains("Médio", true)
                                            SstManagementEngine.RiskLevel.LOW -> occ.risco.contains("Baixo", true)
                                        }
                                    } / 4 // Distributed for representative heatmap visualization
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = assessment.level.composeColor.copy(alpha = 0.85f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$score",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = assessment.level.label,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ========================================================
        // 5. ANDAMENTO DO FUNIL CAPA & FLUXO DE TRATATIVAS
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_capa_funnel"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Status do Ciclo CAPA (Plano 5W2H)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onNavigateToWorkflow) {
                        Text("Gerenciar", fontSize = 11.sp)
                    }
                }

                val pendentesCount = filteredOccurrences.count { it.statusAcao.equals("Pendente", true) }
                val tratativaCount = filteredOccurrences.count { it.statusAcao.equals("Em Tratativa", true) }
                val validacaoCount = filteredOccurrences.count { it.statusAcao.equals("Aguardando Validação", true) }
                val eficazCount = filteredOccurrences.count { it.statusAcao.equals("Eficaz", true) || it.statusAcao.equals("Concluído", true) }

                CapaFunnelStage(
                    stage = "1. Pendente de Ação",
                    count = pendentesCount,
                    total = totalFiltered,
                    color = Color(0xFFEF4444)
                )
                CapaFunnelStage(
                    stage = "2. Em Tratativa / Plano em Execução",
                    count = tratativaCount,
                    total = totalFiltered,
                    color = Color(0xFFF59E0B)
                )
                CapaFunnelStage(
                    stage = "3. Aguardando Validação Técnica",
                    count = validacaoCount,
                    total = totalFiltered,
                    color = Color(0xFF0284C7)
                )
                CapaFunnelStage(
                    stage = "4. Ação Concluída & Eficaz",
                    count = eficazCount,
                    total = totalFiltered,
                    color = Color(0xFF22C55E)
                )
            }
        }

        // ========================================================
        // 6. QUADRO EXECUTIVO DE DECISÃO RÁPIDA (Ocorrências Críticas)
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_executive_decision"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (activeCriticals.isNotEmpty()) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (activeCriticals.isNotEmpty()) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (activeCriticals.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (activeCriticals.isNotEmpty()) SafetyAlertRed else SafetyGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Decisão Rápida da Gerência",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (activeCriticals.isNotEmpty()) Color(0xFF991B1B) else Color(0xFF166534)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeCriticals.isNotEmpty()) SafetyAlertRed else SafetyGreenPrimary
                    ) {
                        Text(
                            text = if (activeCriticals.isNotEmpty()) "${activeCriticals.size} Pendente(s)" else "Em Dia",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (activeCriticals.isEmpty()) {
                    Text(
                        text = "Parabéns! Todas as ocorrências de risco crítico e alto possuem planos de ação concluídos ou eficazes. Não há bloqueios emergenciais pendentes no momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF166534)
                    )
                } else {
                    Text(
                        text = "As seguintes ocorrências exigem validação e deliberação imediata do responsável técnico / gerência:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7F1D1D)
                    )

                    activeCriticals.take(3).forEach { occ ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${occ.ocorrencia} • ${occ.local}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = occ.risco,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyAlertRed
                                    )
                                }
                                Text(
                                    text = occ.relatoDetalhes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Resp: ${occ.responsavelAcao.ifBlank { "SESMT" }} | Prazo: ${occ.prazoAcao.ifBlank { "Imediato" }}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Ver Alerta ➔",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyAlertRed,
                                        modifier = Modifier.clickable { onNavigateToAlerts() }
                                    )
                                }
                            }
                        }
                    }

                    if (activeCriticals.size > 3) {
                        TextButton(
                            onClick = onNavigateToAlerts,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Ver todas as ${activeCriticals.size} ocorrências críticas ➔", fontSize = 12.sp, color = SafetyAlertRed)
                        }
                    }
                }
            }
        }

        // ========================================================
        // 7. EXPORTAÇÃO DE RELATÓRIOS & ATALHOS EXECUTIVOS
        // ========================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.exportarPlanilhaCsv(context) },
                modifier = Modifier.weight(1f).testTag("btn_powerbi_export_excel"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)) // Excel green
            ) {
                Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Excel", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.gerarRelatorioPdfExecutivo(context) },
                modifier = Modifier.weight(1f).testTag("btn_powerbi_export_pdf"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Relatório PDF", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PowerBiKpiCard(
    title: String,
    value: String,
    statusLabel: String,
    statusColor: Color,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutLegendItem(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pct = if (total > 0) (count * 100f / total).toInt() else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = "$count ($pct%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CapaFunnelStage(
    stage: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    val pct = (progress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stage, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text = "$count ($pct%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
