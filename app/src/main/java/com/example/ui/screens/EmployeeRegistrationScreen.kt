package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Employee
import com.example.ui.theme.SafetyGreenPrimary
import com.example.ui.viewmodel.SafetyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeRegistrationScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val employeeState by viewModel.employeeState.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()

    val filteredEmployees = remember(allEmployees, employeeState.searchQuery) {
        val query = employeeState.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allEmployees
        } else {
            allEmployees.filter { emp ->
                emp.registro.lowercase().contains(query) ||
                        emp.nome.lowercase().contains(query) ||
                        emp.setor.lowercase().contains(query)
            }
        }
    }

    val departmentSuggestions = listOf(
        "Manutenção Industrial",
        "Linha de Produção 01",
        "Logística e Expedição",
        "Controle de Qualidade",
        "SESMT / Segurança",
        "Almoxarifado Central",
        "Engenharia de Processos",
        "Operações Especiais",
        "Recursos Humanos",
        "Facilities & Utilidades"
    )

    // Delete confirmation dialog
    if (employeeState.selectedEmployeeToDelete != null) {
        val emp = employeeState.selectedEmployeeToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.selectEmployeeToDelete(null) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Excluir Funcionário") },
            text = {
                Text("Deseja realmente remover o colaborador ${emp.nome} (Matrícula: ${emp.registro}) do banco de dados?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteEmployee(emp) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_employee")
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.selectEmployeeToDelete(null) }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_employee_header"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Ícone Colaborador",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cadastro de Funcionários",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Preenchimento automático inteligente por ID / Matrícula com sincronização no banco local",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // FORM CARD: REGISTRATION, NAME, DEPARTMENT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_employee_form"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Formulário de Cadastro & Consulta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (employeeState.registro.isNotBlank() || employeeState.nome.isNotBlank()) {
                            TextButton(
                                onClick = { viewModel.clearEmployeeRegistrationForm() },
                                modifier = Modifier.testTag("btn_clear_employee_form")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Limpar", fontSize = 12.sp)
                            }
                        }
                    }

                    // FIELD 1: REGISTRO / ID (AUTO-QUERY TRIGGER)
                    OutlinedTextField(
                        value = employeeState.registro,
                        onValueChange = { viewModel.onEmployeeInputRegistroChanged(it) },
                        label = { Text("Matrícula / ID do Funcionário") },
                        placeholder = { Text("Ex: 1001, 1002, 1009...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Matrícula ID",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            when (employeeState.isFoundInDatabase) {
                                true -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Registro encontrado",
                                        tint = SafetyGreenPrimary
                                    )
                                }
                                false -> {
                                    if (employeeState.registro.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Novo registro",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                null -> null
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_employee_id")
                    )

                    // LIVE DATABASE QUERY FEEDBACK BANNER
                    AnimatedVisibility(
                        visible = employeeState.isFoundInDatabase != null && employeeState.registro.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (employeeState.isFoundInDatabase == true) {
                            Surface(
                                color = SafetyGreenPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafetyGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "✓ ID localizado no banco de dados! Nome e departamento preenchidos automaticamente.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SafetyGreenPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else if (employeeState.isFoundInDatabase == false) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ℹ ID não encontrado no banco. Preencha o nome e setor para cadastrar um novo colaborador.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // FIELD 2: FULL NAME (NOME COMPLETO)
                    OutlinedTextField(
                        value = employeeState.nome,
                        onValueChange = { viewModel.onEmployeeInputNomeChanged(it) },
                        label = { Text("Nome Completo do Funcionário") },
                        placeholder = { Text("Ex: João Carlos Silva") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Nome",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_employee_fullname")
                    )

                    // FIELD 3: DEPARTMENT (DEPARTAMENTO / SETOR)
                    OutlinedTextField(
                        value = employeeState.setor,
                        onValueChange = { viewModel.onEmployeeInputSetorChanged(it) },
                        label = { Text("Departamento / Setor") },
                        placeholder = { Text("Ex: Manutenção Industrial, Produção...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Departamento",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_employee_department")
                    )

                    // DEPARTMENT SUGGESTION CHIPS
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sugestões de Departamentos:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(departmentSuggestions) { dept ->
                                val isSelected = employeeState.setor.equals(dept, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onEmployeeInputSetorChanged(dept) },
                                    label = { Text(dept, fontSize = 11.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // ACTION BUTTONS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveEmployeeFromManagement() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_save_employee"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (employeeState.isFoundInDatabase == true) "Atualizar no Banco" else "Cadastrar no Banco",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (employeeState.isFoundInDatabase == true) {
                            OutlinedButton(
                                onClick = {
                                    val currentEmp = allEmployees.find { it.registro == employeeState.registro.trim() }
                                        ?: Employee(employeeState.registro.trim(), employeeState.nome.trim(), employeeState.setor.trim())
                                    viewModel.selectEmployeeToDelete(currentEmp)
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_delete_employee"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir")
                            }
                        }
                    }
                }
            }
        }

        // DIRECTORY & SEARCH SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colaboradores Cadastrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${allEmployees.size} no banco",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // SEARCH BAR
                OutlinedTextField(
                    value = employeeState.searchQuery,
                    onValueChange = { viewModel.onEmployeeSearchQueryChanged(it) },
                    placeholder = { Text("Buscar por Matrícula, Nome ou Setor...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                    },
                    trailingIcon = {
                        if (employeeState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onEmployeeSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_search_employee")
                )
            }
        }

        // LIST OF REGISTERED EMPLOYEES
        if (filteredEmployees.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (employeeState.searchQuery.isNotBlank()) "Nenhum colaborador encontrado com '${employeeState.searchQuery}'." else "Nenhum colaborador cadastrado no banco.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredEmployees, key = { it.registro }) { employee ->
                EmployeeListItemCard(
                    employee = employee,
                    isSelectedInForm = employeeState.registro == employee.registro,
                    onEdit = { viewModel.selectEmployeeForEdit(employee) },
                    onDelete = { viewModel.selectEmployeeToDelete(employee) },
                    onUseInOccurrence = { viewModel.useEmployeeInOccurrenceForm(employee) }
                )
            }
        }
    }
}

@Composable
private fun EmployeeListItemCard(
    employee: Employee,
    isSelectedInForm: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUseInOccurrence: () -> Unit
) {
    val initials = remember(employee.nome) {
        val parts = employee.nome.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "ID"
        }
    }

    val avatarColor = remember(employee.registro) {
        val colors = listOf(
            Color(0xFF1976D2), // Blue
            Color(0xFF2E7D32), // Green
            Color(0xFFD84315), // Deep Orange
            Color(0xFF6A1B9A), // Purple
            Color(0xFF00838F), // Cyan
            Color(0xFFC2185B)  // Pink
        )
        val hash = employee.registro.hashCode()
        colors[Math.abs(hash) % colors.size]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_employee_item_${employee.registro}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedInForm) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AVATAR CIRCLE
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // NAME & DEPARTMENT INFO
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Matrícula: ${employee.registro}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = employee.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = employee.setor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // ACTION BUTTONS FOR EACH EMPLOYEE CARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onUseInOccurrence,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_use_employee_${employee.registro}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preencher no Relato SST", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_edit_employee_${employee.registro}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar no formulário",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_delete_employee_${employee.registro}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir colaborador",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
