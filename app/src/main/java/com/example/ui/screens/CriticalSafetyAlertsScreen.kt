package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SafetyOccurrence
import com.example.ui.theme.SafetyAlertRed
import com.example.ui.theme.SafetyGoldSecondary
import com.example.ui.theme.SafetyGreenPrimary
import com.example.ui.viewmodel.SafetyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CriticalSafetyAlertsScreen(
    viewModel: SafetyViewModel,
    occurrences: List<SafetyOccurrence>,
    onNavigateToForm: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Todos") }
    var occurrenceToEditAction by remember { mutableStateOf<SafetyOccurrence?>(null) }

    // Sorted by occurrence ID in descending order (highest ID/newest occurrence always at top)
    val sortedOccurrences = remember(occurrences) {
        com.example.util.SstManagementEngine.sortOccurrencesByIdDescending(occurrences)
    }

    val criticalCount = occurrences.count { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.CRITICAL }
    val highCount = occurrences.count { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.HIGH }
    val mediumCount = occurrences.count { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.MEDIUM }
    val lowCount = occurrences.count { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.LOW }

    val pendingCount = occurrences.count { it.statusAcao.equals("Pendente", true) || it.statusAcao.isBlank() }
    val inProgressCount = occurrences.count { it.statusAcao.equals("Em Tratativa", true) }
    val completedCount = occurrences.count { it.statusAcao.equals("Concluído", true) || it.statusAcao.equals("Eficaz", true) }
    val overdueCount = occurrences.count {
        val dInfo = com.example.util.SstManagementEngine.calculateDeadlineInfo(it.dataAbertura.ifBlank { it.data }, it.prazoAcao, it.statusAcao)
        dInfo.isAtrasado
    }

    val filteredOccurrences = when (selectedFilter) {
        "🔴 Críticos" -> sortedOccurrences.filter { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.CRITICAL }
        "🟠 Altos" -> sortedOccurrences.filter { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.HIGH }
        "🟡 Médios" -> sortedOccurrences.filter { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.MEDIUM }
        "🟢 Baixos" -> sortedOccurrences.filter { com.example.util.SstManagementEngine.getEffectiveRiskLevel(it) == com.example.util.SstManagementEngine.RiskLevel.LOW }
        "⏳ Pendentes" -> sortedOccurrences.filter { it.statusAcao.equals("Pendente", true) || it.statusAcao.isBlank() }
        "⚙ Em Tratativa" -> sortedOccurrences.filter { it.statusAcao.equals("Em Tratativa", true) }
        "✓ Concluídos" -> sortedOccurrences.filter { it.statusAcao.equals("Concluído", true) || it.statusAcao.equals("Eficaz", true) }
        "🔴 Atrasados" -> sortedOccurrences.filter {
            val dInfo = com.example.util.SstManagementEngine.calculateDeadlineInfo(it.dataAbertura.ifBlank { it.data }, it.prazoAcao, it.statusAcao)
            dInfo.isAtrasado
        }
        else -> sortedOccurrences
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Alert Banner
        Card(
            modifier = Modifier.fillMaxWidth().testTag("header_critical_alerts"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = SafetyAlertRed.copy(alpha = 0.08f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAlertRed.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SafetyAlertRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationImportant,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Central de Alertas & Riscos SST",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SafetyAlertRed
                        )
                        Text(
                            text = "Controle integrado de severidade, matriz P×S e prazos CAPA",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4-Level Risk KPI Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlertMiniBadge("🔴 Crítico: $criticalCount", Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    AlertMiniBadge("🟠 Alto: $highCount", Color(0xFFF97316), modifier = Modifier.weight(1f))
                    AlertMiniBadge("🟡 Médio: $mediumCount", Color(0xFFEAB308), modifier = Modifier.weight(1f))
                    AlertMiniBadge("🟢 Baixo: $lowCount", Color(0xFF22C55E), modifier = Modifier.weight(1f))
                }

                // Action status secondary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlertMiniBadge("Total: ${occurrences.size}", MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    AlertMiniBadge("Pendentes: $pendingCount", SafetyAlertRed, modifier = Modifier.weight(1f))
                    AlertMiniBadge("Em Tratativa: $inProgressCount", SafetyGoldSecondary, modifier = Modifier.weight(1f))
                    if (overdueCount > 0) {
                        AlertMiniBadge("Atrasados: $overdueCount", Color(0xFFB91C1C), modifier = Modifier.weight(1f))
                    } else {
                        AlertMiniBadge("Concluídos: $completedCount", SafetyGreenPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Filter chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "Todos",
                "🔴 Críticos",
                "🟠 Altos",
                "🟡 Médios",
                "🟢 Baixos",
                "⏳ Pendentes",
                "⚙ Em Tratativa",
                "✓ Concluídos",
                "🔴 Atrasados"
            ).forEach { filterName ->
                FilterChip(
                    selected = selectedFilter == filterName,
                    onClick = { selectedFilter = filterName },
                    label = { Text(filterName, fontSize = 11.sp, fontWeight = if (selectedFilter == filterName) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_$filterName")
                )
            }
        }

        // List of Alerts
        if (filteredOccurrences.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SafetyGreenPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (occurrences.isEmpty()) "Nenhuma ocorrência registrada no sistema." else "Nenhuma ocorrência encontrada para o filtro '$selectedFilter'.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Utilize o formulário para registrar desvios e ocorrências com classificação de risco.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateToForm,
                        modifier = Modifier.padding(top = 8.dp).testTag("btn_navigate_to_form_from_alerts")
                    ) {
                        Text("Novo Registro SST")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredOccurrences, key = { it.id }) { item ->
                    CriticalAlertCard(
                        occurrence = item,
                        onEditAction = { occurrenceToEditAction = item },
                        onExportPdf = { viewModel.exportSingleOccurrencePdf(context, item) },
                        onShareAlert = { shareCriticalAlert(context, item) }
                    )
                }
            }
        }
    }

    // Modal Dialog to update CAPA / Action Status and Risk Matrix
    occurrenceToEditAction?.let { occ ->
        EditActionStatusDialog(
            occurrence = occ,
            onDismiss = { occurrenceToEditAction = null },
            onSave = { newStatus, responsavel, prazo, newProb, newSev ->
                viewModel.updateOccurrenceAction(
                    occurrence = occ,
                    newStatus = newStatus,
                    responsavel = responsavel,
                    prazo = prazo,
                    context = context,
                    newProbabilidade = newProb,
                    newSeveridade = newSev
                )
                occurrenceToEditAction = null
            }
        )
    }
}

@Composable
private fun AlertMiniBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        )
    }
}

@Composable
private fun CriticalAlertCard(
    occurrence: SafetyOccurrence,
    onEditAction: () -> Unit,
    onExportPdf: () -> Unit,
    onShareAlert: () -> Unit
) {
    val riskLevel = com.example.util.SstManagementEngine.getEffectiveRiskLevel(occurrence)
    val riskScore = com.example.util.SstManagementEngine.getEffectiveScore(occurrence)
    val prob = occurrence.probabilidade.coerceIn(1, 4)
    val sev = occurrence.severidade.coerceIn(1, 4)

    val deadlineInfo = com.example.util.SstManagementEngine.calculateDeadlineInfo(
        dataAberturaStr = occurrence.dataAbertura.ifBlank { occurrence.data },
        prazoAcaoStr = occurrence.prazoAcao,
        statusAcao = occurrence.statusAcao
    )

    val (statusBg, statusColor, statusText) = when (occurrence.statusAcao.lowercase()) {
        "concluído", "concluido" -> Triple(SafetyGreenPrimary.copy(alpha = 0.15f), SafetyGreenPrimary, "Concluído")
        "eficaz" -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary, "Eficaz / Validado")
        "em tratativa" -> Triple(SafetyGoldSecondary.copy(alpha = 0.15f), SafetyGoldSecondary, "Em Tratativa")
        else -> Triple(SafetyAlertRed.copy(alpha = 0.15f), SafetyAlertRed, "Pendente")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_card_${occurrence.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, riskLevel.composeBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row with full 4-level color risk badge & Action Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = riskLevel.composeLightBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskLevel.composeBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${riskLevel.emoji} ${riskLevel.namePt.uppercase()} (Score $riskScore • P$prob × S$sev)",
                            color = riskLevel.composeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    modifier = Modifier.clickable { onEditAction() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Status: $statusText",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar status",
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Location & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ocorrência #${occurrence.id} - ${occurrence.local.ifBlank { "Local não informado" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${occurrence.data} às ${occurrence.hora}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Perigo & Causa tags
            if (occurrence.perigo.isNotBlank() || occurrence.causa.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (occurrence.perigo.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Perigo: ${occurrence.perigo}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (occurrence.causa.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Causa: ${occurrence.causa}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Occurrence description
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Relato da Ocorrência:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = occurrence.relatoDetalhes.ifBlank { "Sem relato informado." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Immediate Containment Action
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SafetyGreenPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, SafetyGreenPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ação Imediata / Contenção Adotada:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SafetyGreenPrimary
                    )
                    Text(
                        text = occurrence.acaoTomada.ifBlank { "Nenhuma ação imediata registrada." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action owner, deadline & overdue badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (occurrence.responsavelAcao.isNotBlank()) "Resp: ${occurrence.responsavelAcao}" else "Resp: Não atribuído",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (deadlineInfo.label.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = deadlineInfo.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = deadlineInfo.label,
                            color = deadlineInfo.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (occurrence.prazoAcao.isNotBlank()) "Prazo: ${occurrence.prazoAcao}" else "Sem prazo definido",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Actions row: Edit CAPA, Single PDF, Share Alert
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEditAction,
                    modifier = Modifier.weight(1f).testTag("btn_edit_capa_${occurrence.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gerenciar CAPA", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onShareAlert,
                    modifier = Modifier.weight(1f).testTag("btn_share_alert_${occurrence.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disparar", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier.size(36.dp).testTag("btn_pdf_alert_${occurrence.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Gerar PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditActionStatusDialog(
    occurrence: SafetyOccurrence,
    onDismiss: () -> Unit,
    onSave: (newStatus: String, responsavel: String, prazo: String, newProb: Int, newSev: Int) -> Unit
) {
    var status by remember { mutableStateOf(occurrence.statusAcao.ifBlank { "Pendente" }) }
    var responsavel by remember { mutableStateOf(occurrence.responsavelAcao) }
    var prazo by remember { mutableStateOf(occurrence.prazoAcao) }
    var prob by remember { mutableStateOf(occurrence.probabilidade.coerceIn(1, 4)) }
    var sev by remember { mutableStateOf(occurrence.severidade.coerceIn(1, 4)) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val statusOptions = listOf("Pendente", "Em Tratativa", "Concluído", "Eficaz")

    val riskAssessment = remember(prob, sev) {
        com.example.util.SstManagementEngine.calculateRiskMatrix(prob, sev)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Plano de Ação Corretiva & Risco (CAPA)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ocorrência #${occurrence.id} - ${occurrence.local}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Risk Matrix Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskAssessment.level.composeLightBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskAssessment.level.composeBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${riskAssessment.level.emoji} ${riskAssessment.level.fullLabel}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = riskAssessment.level.composeTextColor
                            )
                            Text(
                                text = "Score: ${riskAssessment.score} (P$prob × S$sev) • Prioridade: ${riskAssessment.priority}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Probability selector (1..4)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Probabilidade (P):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..4).forEach { pVal ->
                            val isSel = prob == pVal
                            Button(
                                onClick = { prob = pVal },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("P$pVal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Severity selector (1..4)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Severidade (S):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..4).forEach { sVal ->
                            val isSel = sev == sVal
                            Button(
                                onClick = { sev = sVal },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) riskAssessment.level.composeBorderColor else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("S$sVal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status da Ação") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = responsavel,
                    onValueChange = { responsavel = it },
                    label = { Text("Responsável Técnico EHS / Liderança") },
                    placeholder = { Text("Ex: Eng. Roberto / Sup. Manutenção") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prazo,
                    onValueChange = { prazo = it },
                    label = { Text("Prazo de Conclusão / Verificação") },
                    placeholder = { Text("Ex: 24h ou 20/08/2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(status, responsavel, prazo, prob, sev) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Salvar Atualização")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun shareCriticalAlert(context: Context, occurrence: SafetyOccurrence) {
    val riskLevel = com.example.util.SstManagementEngine.getEffectiveRiskLevel(occurrence)
    val riskScore = com.example.util.SstManagementEngine.getEffectiveScore(occurrence)

    val message = buildString {
        append("🚨 *ALERTA DE SEGURANÇA (SST) — ${riskLevel.namePt.uppercase()}* 🚨\n\n")
        append("📍 *Local:* ${occurrence.local}\n")
        append("📅 *Data/Hora:* ${occurrence.data} às ${occurrence.hora}\n")
        append("⚠️ *Classificação do Risco:* ${riskLevel.fullLabel} (Score: $riskScore | P${occurrence.probabilidade} × S${occurrence.severidade})\n")
        append("🔍 *Tipo de Evento:* ${occurrence.ocorrencia}\n")
        if (occurrence.perigo.isNotBlank()) append("⚠️ *Perigo:* ${occurrence.perigo}\n")
        if (occurrence.causa.isNotBlank()) append("🔬 *Causa Raiz:* ${occurrence.causa}\n")
        append("\n📝 *Relato:* ${occurrence.relatoDetalhes}\n\n")
        append("🛡️ *Ação Imediata:* ${occurrence.acaoTomada}\n\n")
        append("⚙️ *Status CAPA:* ${occurrence.statusAcao.ifBlank { "Pendente" }}\n")
        if (occurrence.responsavelAcao.isNotBlank()) {
            append("👤 *Responsável:* ${occurrence.responsavelAcao}\n")
        }
        if (occurrence.prazoAcao.isNotBlank()) {
            append("⏱️ *Prazo:* ${occurrence.prazoAcao}\n")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "[ALERTA SST] ${occurrence.local} - ${occurrence.data}")
        putExtra(Intent.EXTRA_TEXT, message)
    }

    context.startActivity(Intent.createChooser(intent, "Disparar Alerta SST"))
}
