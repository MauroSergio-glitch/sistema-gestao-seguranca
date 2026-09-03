package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SafetyOccurrence
import com.example.ui.theme.SafetyAlertRed
import com.example.ui.theme.SafetyGoldSecondary
import com.example.ui.theme.SafetyGreenPrimary
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccurrenceHistorySheet(
    occurrences: List<SafetyOccurrence>,
    onDismiss: () -> Unit,
    onDelete: (SafetyOccurrence) -> Unit,
    onExportPdf: ((SafetyOccurrence) -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
    onBackupAndClear: (() -> Unit)? = null,
    sheetState: SheetState
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico (${occurrences.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackupAndClear != null && occurrences.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onBackupAndClear,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyAlertRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyAlertRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limpar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (onBackup != null) {
                        Button(
                            onClick = onBackup,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyGreenPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Backup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (occurrences.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma ocorrência registrada ainda.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(occurrences, key = { it.id }) { item ->
                        OccurrenceCardItem(
                            occurrence = item,
                            onDelete = { onDelete(item) },
                            onExportPdf = if (onExportPdf != null) { { onExportPdf(item) } } else null,
                            onResendEmail = {
                                val subject = "[Relato SST] Re: Ocorrência - ${item.local} - ${item.data}"
                                val body = buildString {
                                    appendLine("=== OCORRÊNCIA SST REGISTRADA ===")
                                    appendLine("Data: ${item.data} - Hora: ${item.hora}")
                                    appendLine("Colaborador: ${item.nomeColaborador} (${item.registro}) - Setor: ${item.setor}")
                                    appendLine("Local: ${item.local}")
                                    appendLine("Grau de Risco: ${item.risco}")
                                    appendLine("Tipo: ${item.ocorrencia}")
                                    appendLine("Classificação: ${item.classificacao}")
                                    appendLine("Clima: ${item.clima}")
                                    appendLine("Causa: ${item.causa}")
                                    appendLine("\nDetalhes:\n${item.relatoDetalhes}")
                                    appendLine("\nAção Tomada:\n${item.acaoTomada}")
                                }

                                val emailIntent = com.example.ui.viewmodel.SafetyViewModel.createEmailIntent(
                                    context = context,
                                    recipient = "",
                                    subject = subject,
                                    body = body,
                                    fotoUri = item.fotoUri
                                )
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Enviar Ocorrência SST por E-mail"))
                                } catch (e: Exception) {
                                    // Fallback if no activity handler available
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fechar Histórico")
            }
        }
    }
}

@Composable
fun OccurrenceCardItem(
    occurrence: SafetyOccurrence,
    onDelete: () -> Unit,
    onExportPdf: (() -> Unit)? = null,
    onResendEmail: () -> Unit
) {
    val riskLevel = com.example.util.SstManagementEngine.getEffectiveRiskLevel(occurrence)
    val riskScore = com.example.util.SstManagementEngine.getEffectiveScore(occurrence)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskLevel.composeBorderColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskLevel.composeLightBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${riskLevel.emoji} ${riskLevel.namePt} (P${occurrence.probabilidade}×S${occurrence.severidade}=$riskScore)",
                            color = riskLevel.composeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${occurrence.data} às ${occurrence.hora}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Salvo no Banco Local",
                        tint = SafetyGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (onExportPdf != null) {
                        IconButton(onClick = onExportPdf) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Gerar e Abrir PDF",
                                tint = SafetyGreenPrimary
                            )
                        }
                    }
                    IconButton(onClick = onResendEmail) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Reenviar por e-mail",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir ocorrência",
                            tint = SafetyAlertRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = occurrence.ocorrencia,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (occurrence.local.isNotBlank()) {
                Text(
                    text = "Local: ${occurrence.local}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (occurrence.nomeColaborador.isNotBlank()) {
                Text(
                    text = "Colaborador: ${occurrence.nomeColaborador} (${occurrence.setor})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (occurrence.relatoDetalhes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = occurrence.relatoDetalhes,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
            }
        }
    }
}
