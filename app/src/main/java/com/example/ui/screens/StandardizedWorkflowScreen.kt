package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.vector.ImageVector
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
fun StandardizedWorkflowScreen(
    viewModel: SafetyViewModel,
    occurrences: List<SafetyOccurrence>,
    onNavigateToForm: () -> Unit
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf(0) } // 0: Metodologia de Causas, 1: Fluxo PDCA/CAPA, 2: Matriz de Ações

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Sub-Tabs
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Categorias de Causa", fontSize = 12.sp, fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("tab_cause_methodology")
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Fluxo CAPA / PDCA", fontSize = 12.sp, fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("tab_workflow_pdca")
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text("Matriz de Ações (${occurrences.size})", fontSize = 12.sp, fontWeight = if (subTab == 2) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("tab_action_tracker")
            )
        }

        when (subTab) {
            0 -> CauseCategorizationMethodologySection()
            1 -> StandardizedCapaWorkflowSection()
            2 -> CorrectiveActionTrackerSection(viewModel, occurrences)
        }
    }
}

// ==========================================
// 1. CAUSE CATEGORIZATION METHODOLOGY (ISHIKAWA 6M & 5 PORQUÊS)
// ==========================================
@Composable
private fun CauseCategorizationMethodologySection() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Column {
                    Text(
                        text = "Padronização de Causa Raiz (Ishikawa 6M)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Classificação técnica de desvios para eliminação definitiva de riscos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "As 6 Famílias Padronizadas de Causa Raiz SST:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        CauseFamilyCard(
            title = "1. Mão de Obra / Fator Humano",
            icon = Icons.Default.People,
            color = Color(0xFF1976D2),
            description = "Fatores comportamentais, desvios operacionais, pressa, fadiga ou excesso de confiança.",
            examples = listOf("Operar máquina sem autorização", "Desatenção ou distração em área de risco", "Não cumprimento de instrução conhecida"),
            recommendedAction = "Diálogo Diário de Segurança (DDS), retreinamento comportamental e reciclagem de segurança."
        )

        CauseFamilyCard(
            title = "2. Método & Procedimentos (IT / POP)",
            icon = Icons.Default.Rule,
            color = Color(0xFF7B1FA2),
            description = "Inexistência, desatualização ou falta de clareza nas Instruções de Trabalho e Análise de Risco (APR).",
            examples = listOf("Execução de trabalho em altura sem PT", "Instrução de trabalho sem detalhamento de bloqueio LOTO", "Atalho em procedimento de manutenção"),
            recommendedAction = "Revisão e homologação do Procedimento Operacional Padrão (POP) com a equipe de campo."
        )

        CauseFamilyCard(
            title = "3. Máquinas & Equipamentos",
            icon = Icons.Default.Build,
            color = Color(0xFFC2185B),
            description = "Falhas mecânicas, hidráulicas, elétricas, ausência de proteção física NR-12 ou falta de manutenção.",
            examples = listOf("Rompimento de mangueira hidráulica pressurizada", "Botão de emergência inoperante", "Ausência de grade de proteção em correia"),
            recommendedAction = "Abertura imediata de OS de Manutenção Preventiva / Corretiva e bloqueio LOTO."
        )

        CauseFamilyCard(
            title = "4. Material & EPI / Ferramentas",
            icon = Icons.Default.Construction,
            color = Color(0xFFE65100),
            description = "EPI ausente, danificado ou inadequado ao risco, bem como ferramentas improvisadas.",
            examples = listOf("Uso de luva com CA inadequado para corte", "Protetor auricular tipo concha ausente em área de ruído", "Ferramenta manual desgastada/rachada"),
            recommendedAction = "Substituição imediata do lote de EPIs e auditoria de Certificados de Aprovação (CA)."
        )

        CauseFamilyCard(
            title = "5. Meio Ambiente & Condições Físicas",
            icon = Icons.Default.LocationCity,
            color = Color(0xFF00796B),
            description = "Condições do ambiente de trabalho como piso escorregadio, iluminação insuficiente, ruído e calor.",
            examples = listOf("Mancha de óleo ou água em corredor de pedestres", "Iluminação queimada em desnível ou escada", "Vazamento de produto químico com odor"),
            recommendedAction = "Contenção com barreiras absorventes, manutenção predial e sinalização preventiva."
        )

        CauseFamilyCard(
            title = "6. Medição, Gestão & Treinamento",
            icon = Icons.Default.Speed,
            color = Color(0xFF388E3C),
            description = "Falta de fiscalização, dimensionamento inadequado da equipe ou prazos incompatíveis com a segurança.",
            examples = listOf("Colaborador novo sem integração de segurança", "Inspeção de rotina do setor não realizada", "Meta de produção sobreposta ao tempo seguro"),
            recommendedAction = "Auditoria de liderança ativa, matriz de competências e plano de capacitação periódica."
        )

        // 5 Whys Technique Box
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = SafetyGoldSecondary)
                    Text(
                        text = "Exemplo Prático: Técnica dos 5 Porquês",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val whys = listOf(
                    "1º Por quê? O operador escorregou no galpão. -> Porque havia óleo no chão.",
                    "2º Por quê? Havia óleo no chão. -> Porque a empilhadeira estava vazando.",
                    "3º Por quê? A empilhadeira estava vazando. -> Porque o retentor rompeu por fadiga.",
                    "4º Por quê? O retentor rompeu. -> Porque a manutenção preventiva de 500h estava atrasada.",
                    "5º Por quê? A manutenção atrasou. -> Causa Raiz: O cronograma do software de manutenção não emitia alerta automático para o supervisor."
                )

                whys.forEach { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CauseFamilyCard(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    examples: List<String>,
    recommendedAction: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Exemplos Comuns em Fábricas:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    examples.forEach { ex ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = color, fontWeight = FontWeight.Bold)
                            Text(ex, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Ação Recomendada: $recommendedAction",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. STANDARDIZED CAPA / PDCA WORKFLOW
// ==========================================
@Composable
private fun StandardizedCapaWorkflowSection() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
                Column {
                    Text(
                        text = "Ciclo de Resposta & Ações Corretivas (CAPA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fluxo operacional padronizado da identificação à validação de eficácia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val steps = listOf(
            CapaStep(
                stepNum = 1,
                title = "Identificação & Notificação Imediata",
                sla = "0 a 1 hora",
                owner = "Colaborador no local / Líder de Setor",
                details = "Registro rápido do desvio no aplicativo com fotos, local exato e preenchimento das condições.",
                color = MaterialTheme.colorScheme.primary
            ),
            CapaStep(
                stepNum = 2,
                title = "Contenção Imediata do Risco",
                sla = "0 a 2 horas",
                owner = "Brigada / Encarregado da Área",
                details = "Isolamento da área com fita zebrada, parada de máquina ou bloqueio LOTO provisório para evitar novos acidentes.",
                color = SafetyAlertRed
            ),
            CapaStep(
                stepNum = 3,
                title = "Investigação & Causa Raiz",
                sla = "1 a 3 dias úteis",
                owner = "SESMT / Técnico de Segurança / CIPA",
                details = "Aplicação da metodologia dos 5 Porquês e Diagrama de Ishikawa com os operadores envolvidos.",
                color = SafetyGoldSecondary
            ),
            CapaStep(
                stepNum = 4,
                title = "Elaboração do Plano de Ação 5W2H",
                sla = "3 a 5 dias úteis",
                owner = "Gestor do Setor + EHS",
                details = "Definição clara de O QUE será feito, QUEM é o responsável, QUAL o prazo e COMO será implementado.",
                color = Color(0xFF1976D2)
            ),
            CapaStep(
                stepNum = 5,
                title = "Auditoria de Eficácia & Fechamento",
                sla = "15 a 30 dias após ação",
                owner = "Engenheiro de Segurança / Auditor Interno",
                details = "Inspeção em campo para validar se o risco foi 100% neutralizado e não houve reincidência.",
                color = SafetyGreenPrimary
            )
        )

        steps.forEach { step ->
            WorkflowStepCard(step)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class CapaStep(
    val stepNum: Int,
    val title: String,
    val sla: String,
    val owner: String,
    val details: String,
    val color: Color
)

@Composable
private fun WorkflowStepCard(step: CapaStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step Number Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(step.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${step.stepNum}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = step.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = step.sla,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = step.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Responsável: ${step.owner}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = step.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 3. INTERACTIVE CORRECTIVE ACTION TRACKER (CAPA MASTER BOARD)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CorrectiveActionTrackerSection(
    viewModel: SafetyViewModel,
    occurrences: List<SafetyOccurrence>
) {
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf("Todos") }
    var occurrenceToEdit by remember { mutableStateOf<SafetyOccurrence?>(null) }

    val filteredList = when (filterStatus) {
        "Pendentes" -> occurrences.filter { it.statusAcao.equals("Pendente", true) || it.statusAcao.isBlank() }
        "Em Tratativa" -> occurrences.filter { it.statusAcao.equals("Em Tratativa", true) }
        "Concluídos" -> occurrences.filter { it.statusAcao.equals("Concluído", true) }
        "Eficazes" -> occurrences.filter { it.statusAcao.equals("Eficaz", true) }
        else -> occurrences
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Pendentes", "Em Tratativa", "Concluídos", "Eficazes").forEach { filter ->
                FilterChip(
                    selected = filterStatus == filter,
                    onClick = { filterStatus = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("action_filter_$filter")
                )
            }
        }

        if (filteredList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, tint = SafetyGreenPrimary, modifier = Modifier.size(40.dp))
                    Text("Nenhuma ação no status selecionado.", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    ActionTrackerCard(
                        occurrence = item,
                        onEdit = { occurrenceToEdit = item }
                    )
                }
            }
        }
    }

    occurrenceToEdit?.let { occ ->
        EditActionDialog(
            occurrence = occ,
            onDismiss = { occurrenceToEdit = null },
            onSave = { newStatus, responsavel, prazo, acaoPrev, setorResp, descSol, respVal, eficacia, newProb, newSev ->
                viewModel.updateOccurrenceFullAction(
                    context = context,
                    occurrence = occ,
                    newStatus = newStatus,
                    responsavel = responsavel,
                    prazo = prazo,
                    acaoPreventiva = acaoPrev,
                    setorResponsavel = setorResp,
                    descricaoSolucao = descSol,
                    fotoDepoisUri = null,
                    responsavelValidacao = respVal,
                    avaliacaoEficacia = eficacia,
                    observacoes = occ.observacoesAcao,
                    newProbabilidade = newProb,
                    newSeveridade = newSev
                )
                occurrenceToEdit = null
            }
        )
    }
}

@Composable
private fun ActionTrackerCard(
    occurrence: SafetyOccurrence,
    onEdit: () -> Unit
) {
    val riskLevel = com.example.util.SstManagementEngine.getEffectiveRiskLevel(occurrence)
    val riskScore = com.example.util.SstManagementEngine.getEffectiveScore(occurrence)

    val (badgeBg, badgeColor) = when (occurrence.statusAcao.lowercase()) {
        "concluído", "concluido" -> Pair(SafetyGreenPrimary.copy(alpha = 0.15f), SafetyGreenPrimary)
        "eficaz" -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary)
        "em tratativa" -> Pair(SafetyGoldSecondary.copy(alpha = 0.15f), SafetyGoldSecondary)
        else -> Pair(SafetyAlertRed.copy(alpha = 0.15f), SafetyAlertRed)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("action_item_${occurrence.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, riskLevel.composeBorderColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ocorrência #${occurrence.id} - ${occurrence.local}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = occurrence.statusAcao.ifBlank { "Pendente" },
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Risk Matrix Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = riskLevel.composeLightBg,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, riskLevel.composeBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${riskLevel.emoji} ${riskLevel.namePt.uppercase()} (Score $riskScore • P${occurrence.probabilidade} × S${occurrence.severidade})",
                        color = riskLevel.composeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Causa Identificada: ${occurrence.causa.ifBlank { "Não especificada" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Ação Corretiva: ${occurrence.acaoTomada.ifBlank { "Pendente de definição." }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (occurrence.responsavelAcao.isNotBlank()) "Resp: ${occurrence.responsavelAcao}" else "Sem responsável atribuído",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (occurrence.prazoAcao.isNotBlank()) "Prazo: ${occurrence.prazoAcao}" else "Sem prazo",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditActionDialog(
    occurrence: SafetyOccurrence,
    onDismiss: () -> Unit,
    onSave: (
        status: String,
        resp: String,
        prazo: String,
        acaoPrev: String,
        setorResp: String,
        descSol: String,
        respVal: String,
        eficacia: String,
        newProb: Int,
        newSev: Int
    ) -> Unit
) {
    var status by remember { mutableStateOf(occurrence.statusAcao.ifBlank { "Pendente" }) }
    var responsavel by remember { mutableStateOf(occurrence.responsavelAcao) }
    var prazo by remember { mutableStateOf(occurrence.prazoAcao) }
    var acaoPreventiva by remember { mutableStateOf(occurrence.acaoPreventiva) }
    var setorResponsavel by remember { mutableStateOf(occurrence.setorResponsavel) }
    var descricaoSolucao by remember { mutableStateOf(occurrence.descricaoSolucao) }
    var responsavelValidacao by remember { mutableStateOf(occurrence.responsavelValidacao) }
    var avaliacaoEficacia by remember { mutableStateOf(occurrence.avaliacaoEficacia.ifBlank { "Pendente de Avaliação" }) }
    var prob by remember { mutableStateOf(occurrence.probabilidade.coerceIn(1, 4)) }
    var sev by remember { mutableStateOf(occurrence.severidade.coerceIn(1, 4)) }

    var expandedDropdown by remember { mutableStateOf(false) }
    var expandedEficaciaDropdown by remember { mutableStateOf(false) }

    val statusOptions = listOf("Pendente", "Em Tratativa", "Concluído", "Eficaz")
    val eficaciaOptions = listOf("Pendente de Avaliação", "Eficaz (Risco Eliminado/Mitigado)", "Requer Ajustes no Plano")

    val riskAssessment = remember(prob, sev) {
        com.example.util.SstManagementEngine.calculateRiskMatrix(prob, sev)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Gerenciamento de Ação SST (CAPA)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ocorrência #${occurrence.id}: ${occurrence.local}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Live Risk Matrix Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskAssessment.level.composeLightBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskAssessment.level.composeBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${riskAssessment.level.emoji} ${riskAssessment.level.fullLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = riskAssessment.level.composeTextColor
                        )
                        Text(
                            text = "Score: ${riskAssessment.score} (P$prob × S$sev) • Prioridade: ${riskAssessment.priority}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Probability selector
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Probabilidade (P):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..4).forEach { pVal ->
                            val isSel = prob == pVal
                            Button(
                                onClick = { prob = pVal },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("P$pVal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Severity selector
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Severidade (S):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..4).forEach { sVal ->
                            val isSel = sev == sVal
                            Button(
                                onClick = { sev = sVal },
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) riskAssessment.level.composeBorderColor else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("S$sVal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Status
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

                // Ação Preventiva Definitiva
                OutlinedTextField(
                    value = acaoPreventiva,
                    onValueChange = { acaoPreventiva = it },
                    label = { Text("Ação Preventiva Definitiva") },
                    placeholder = { Text("Solução definitiva de engenharia/procedimento") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Descrição da Solução / Evidência Conclusiva
                OutlinedTextField(
                    value = descricaoSolucao,
                    onValueChange = { descricaoSolucao = it },
                    label = { Text("Evidência / Descrição da Conclusão") },
                    placeholder = { Text("Descreva o que foi realizado na prática...") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Responsável e Setor
                OutlinedTextField(
                    value = responsavel,
                    onValueChange = { responsavel = it },
                    label = { Text("Responsável Técnico") },
                    placeholder = { Text("Ex: Eng. Roberto / Supervisor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = setorResponsavel,
                    onValueChange = { setorResponsavel = it },
                    label = { Text("Setor Responsável") },
                    placeholder = { Text("Ex: Manutenção, EHS, Operação") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Prazo
                OutlinedTextField(
                    value = prazo,
                    onValueChange = { prazo = it },
                    label = { Text("Prazo Limite / SLA") },
                    placeholder = { Text("DD/MM/AAAA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Avaliação da Eficácia
                ExposedDropdownMenuBox(
                    expanded = expandedEficaciaDropdown,
                    onExpandedChange = { expandedEficaciaDropdown = !expandedEficaciaDropdown }
                ) {
                    OutlinedTextField(
                        value = avaliacaoEficacia,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Avaliação de Eficácia") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEficaciaDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedEficaciaDropdown,
                        onDismissRequest = { expandedEficaciaDropdown = false }
                    ) {
                        eficaciaOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    avaliacaoEficacia = option
                                    expandedEficaciaDropdown = false
                                }
                            )
                        }
                    }
                }

                // Supervisor / Validador
                OutlinedTextField(
                    value = responsavelValidacao,
                    onValueChange = { responsavelValidacao = it },
                    label = { Text("Validação / Supervisor EHS") },
                    placeholder = { Text("Nome do responsável pela homologação") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        status,
                        responsavel,
                        prazo,
                        acaoPreventiva,
                        setorResponsavel,
                        descricaoSolucao,
                        responsavelValidacao,
                        avaliacaoEficacia,
                        prob,
                        sev
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
