package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import com.example.ui.screens.AboutScreen
import com.example.data.remote.GoogleSheetsSyncService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import com.example.ui.components.BackupAndClearDialog
import com.example.ui.components.PostBackupClearConfirmationDialog
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Surface
import com.example.R
import com.example.ui.components.CameraXCaptureDialog
import com.example.ui.components.CoverSettingsDialog
import com.example.ui.components.DatePickerDialogComponent
import com.example.ui.components.OccurrenceHistorySheet
import com.example.ui.components.SafetyDropdown
import com.example.ui.components.TimePickerDialogComponent
import com.example.ui.theme.SafetyAlertRed
import com.example.ui.theme.SafetyGreenPrimary
import com.example.ui.viewmodel.SafetyViewModel
import com.example.util.CoverType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFormScreen(
    viewModel: SafetyViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val occurrences by viewModel.allOccurrences.collectAsState()
    val pendingReport by viewModel.pendingPreviousDayReport.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val coverConfig by viewModel.coverConfig.collectAsState()
    val showCoverSettings by viewModel.showCoverSettingsDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCoverConfig(context)
        viewModel.checkDailyWarning(context)
    }

    val showClearConfirm by viewModel.showClearConfirmation.collectAsState()
    val showExitConfirm by viewModel.showExitConfirmation.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val showHistorySheet by viewModel.showHistorySheet.collectAsState()
    val showEmailSettings by viewModel.showEmailSettingsDialog.collectAsState()
    val showBackupAndClear by viewModel.showBackupAndClearDialog.collectAsState()
    val postBackupVerification by viewModel.postBackupVerificationState.collectAsState()
    val showAboutScreen by viewModel.showAboutScreen.collectAsState()
    var showSettingsMenu by remember { mutableStateOf(false) }

    var showCameraDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var activeVoiceTarget by remember { mutableStateOf("relato") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: ArrayList<String>? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spokenText?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                when (activeVoiceTarget) {
                    "perigo" -> {
                        val current = formState.perigo.trim()
                        val updated = if (current.isBlank()) text else "$current. $text"
                        viewModel.onPerigoChanged(updated)
                    }
                    "acaoPreventiva" -> {
                        val current = formState.acaoPreventiva.trim()
                        val updated = if (current.isBlank()) text else "$current. $text"
                        viewModel.onAcaoPreventivaChanged(updated)
                    }
                    "acao" -> {
                        val current = formState.acaoTomada.trim()
                        val updated = if (current.isBlank()) text else "$current. $text"
                        viewModel.onAcaoTomadaChanged(updated)
                    }
                    else -> {
                        val current = formState.relatoDetalhes.trim()
                        val updated = if (current.isBlank()) text else "$current. $text"
                        viewModel.onRelatoDetalhesChanged(updated)
                    }
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Áudio convertido em texto com sucesso!")
                }
            }
        }
    }

    val startVoiceRecognition: (String) -> Unit = { target ->
        activeVoiceTarget = target
        val promptText = when (target) {
            "perigo" -> "Dite a identificação do perigo..."
            "acaoPreventiva" -> "Dite a ação preventiva definitiva..."
            "acao" -> "Fale a ação tomada de contenção..."
            else -> "Dite os detalhes da ocorrência..."
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Reconhecimento de voz não disponível neste aparelho.")
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition(activeVoiceTarget)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Permissão de microfone necessária para ditar.")
            }
        }
    }

    val triggerVoiceInput: (String) -> Unit = { target ->
        activeVoiceTarget = target
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            startVoiceRecognition(target)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onFotoSelected(uri?.toString())
    }

    // Options for dropdowns
    val climaOptions = listOf(
        "Ensolarado",
        "Chuvoso",
        "Nublado",
        "Calorento / Quente",
        "Frio",
        "Umidade Alta",
        "Vento Forte",
        "Outro"
    )

    val categoriaCausaOptions = listOf(
        "Mão de Obra (Pessoas/Comportamento)",
        "Método (Procedimentos/Instrução)",
        "Máquinas (Equipamentos/Ferramentas)",
        "Material (Insumos/EPI/Peças)",
        "Meio Ambiente (Local/Piso/Iluminação)",
        "Medição / Gestão (Supervisão/Treinamento)"
    )

    val causaOptions = listOf(
        "Falha Humana / Desvio Operacional",
        "Falha de Equipamento / Desgaste",
        "Condição Inadequada do Posto",
        "Falta ou Uso Incorreto de EPI",
        "Procedimento Inexistente ou Incorreto",
        "Piso Escorregadio / Óleo ou Água",
        "Iluminação ou Ventilação Inadequada",
        "Falta de Treinamento / Orientação",
        "Guarda de Proteção Ausente",
        "Outros"
    )

    val riscoOptions = listOf(
        "Baixo (Verde)",
        "Médio (Amarelo)",
        "Alto (Laranja)",
        "Crítico (Vermelho)"
    )

    val ocorrenciaOptions = listOf(
        "Quase Acidente (Near Miss)",
        "Condição Abaixo do Padrão",
        "Ato Abaixo do Padrão",
        "Incidente sem Lesão",
        "Acidente sem Afastamento",
        "Acidente com Afastamento",
        "Incidente Ambiental"
    )

    val classificacaoOptions = listOf(
        "Observação de Segurança",
        "Oportunidade de Melhoria",
        "Não Conformidade",
        "Situação de Emergência",
        "Boas Práticas / Positivo"
    )

    if (showAboutScreen) {
        AboutScreen(
            onNavigateBack = { viewModel.showAboutScreen.value = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.drawable.img_foco_prevencao_logo_1787517691154)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo Foco na Prevenção",
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Foco na Prevenção",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Registro de Ocorrências SST",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.requestBackupAndClear() },
                        modifier = Modifier.testTag("btn_backup_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Backup e Limpeza de Registros"
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = Modifier.testTag("btn_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Menu de Configurações"
                            )
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false },
                            modifier = Modifier.testTag("dropdown_settings_menu")
                        ) {
                            DropdownMenuItem(
                                text = { Text("Configurar E-mail do Setor") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.showEmailSettingsDialog.value = true
                                },
                                modifier = Modifier.testTag("menu_item_email_settings")
                            )
                            DropdownMenuItem(
                                text = { Text("Backup e Otimização") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.requestBackupAndClear()
                                },
                                modifier = Modifier.testTag("menu_item_backup_clear")
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sobre",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.showAboutScreen.value = true
                                },
                                modifier = Modifier.testTag("menu_item_about")
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.showHistorySheet.value = true },
                        modifier = Modifier.testTag("btn_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Ver Histórico"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val activeAlertsCount = remember(occurrences) {
                    com.example.util.SstManagementEngine.countActiveAlerts(occurrences)
                }

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "Registro") },
                    label = { Text("Registro", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_form"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Painel KPIs") },
                    label = { Text("Painel KPIs", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_dashboard"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        if (activeAlertsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = SafetyAlertRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$activeAlertsCount")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.NotificationImportant, contentDescription = "Alertas Críticos")
                            }
                        } else {
                            Icon(Icons.Default.NotificationImportant, contentDescription = "Alertas Críticos")
                        }
                    },
                    label = { Text("Alertas", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_alerts"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = SafetyAlertRed,
                        selectedTextColor = SafetyAlertRed
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Fluxo & CAPA") },
                    label = { Text("Fluxo CAPA", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_workflow"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = "Funcionários") },
                    label = { Text("Funcionários", fontSize = 11.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_employees"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                1 -> {
                    SafetyManagementDashboardScreen(
                        viewModel = viewModel,
                        occurrences = occurrences,
                        onNavigateToAlerts = { viewModel.selectTab(2) },
                        onNavigateToWorkflow = { viewModel.selectTab(3) },
                        onNavigateToForm = { viewModel.selectTab(0) }
                    )
                }
                2 -> {
                    CriticalSafetyAlertsScreen(
                        viewModel = viewModel,
                        occurrences = occurrences,
                        onNavigateToForm = { viewModel.selectTab(0) }
                    )
                }
                3 -> {
                    StandardizedWorkflowScreen(
                        viewModel = viewModel,
                        occurrences = occurrences,
                        onNavigateToForm = { viewModel.selectTab(0) }
                    )
                }
                4 -> {
                    EmployeeRegistrationScreen(
                        viewModel = viewModel
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
            // Header Image Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.img_sst_cover_banner_1786993083781)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Banner Segurança do Trabalho SST",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.drawable.img_foco_prevencao_logo_1787517691154)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo Foco na Prevenção",
                            modifier = Modifier
                                .padding(end = 14.dp)
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Foco na Prevenção",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Relato de Ocorrências SST — Protegendo a Equipe",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Quick Demo Pre-fill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.preencherExemplo() },
                    modifier = Modifier.testTag("btn_preencher_exemplo")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preencher Exemplo")
                }
            }

            // SECTION 1: DATA E HORA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Data e Hora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Selecione a Data
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("field_data")
                        ) {
                            OutlinedTextField(
                                value = formState.data,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Selecione a Data", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Calendário"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { viewModel.showDatePicker.value = true }
                            )
                        }

                        // Hora
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("field_hora")
                        ) {
                            OutlinedTextField(
                                value = formState.hora,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Hora", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Relógio"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { viewModel.showTimePicker.value = true }
                            )
                        }
                    }
                }
            }

            // SECTION 2: REGISTRO E IDENTIFICAÇÃO DO COLABORADOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Text(
                            text = "Identificação do Colaborador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { viewModel.selectTab(4) },
                            modifier = Modifier.testTag("btn_open_employee_management")
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gerenciar Cadastro", fontSize = 12.sp)
                        }
                    }

                    // Registro (Matrícula)
                    OutlinedTextField(
                        value = formState.registro,
                        onValueChange = { viewModel.onRegistroChanged(it) },
                        label = { Text("Registro (Matrícula / ID)") },
                        placeholder = { Text("Ex: 1001, 1002") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null)
                        },
                        trailingIcon = {
                            if (formState.isEmployeeFound == true) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Encontrado",
                                    tint = SafetyGreenPrimary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_registro"),
                        singleLine = true
                    )

                    if (formState.isEmployeeFound == true) {
                        Text(
                            text = "✓ Colaborador localizado no banco de dados! Dados preenchidos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SafetyGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (formState.isEmployeeFound == false && formState.registro.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ℹ Matrícula não encontrada. Preencha o nome e setor abaixo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (formState.nomeColaborador.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.saveEmployeeFromIncidentForm() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_save_employee_from_form"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cadastrar '${formState.nomeColaborador}' no Banco de Dados", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Nome do Colaborador (Auto-preenchido)
                    OutlinedTextField(
                        value = formState.nomeColaborador,
                        onValueChange = { viewModel.onNomeColaboradorChanged(it) },
                        label = { Text("Nome do Colaborador") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_nome_colaborador"),
                        singleLine = true
                    )

                    // Setor (Auto-preenchido)
                    OutlinedTextField(
                        value = formState.setor,
                        onValueChange = { viewModel.onSetorChanged(it) },
                        label = { Text("Setor") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_setor"),
                        singleLine = true
                    )
                }
            }

            // ALERTA DE REINCIDÊNCIA (SE DETECTADA)
            if (formState.isRiskRecurrent) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_recurrence_alert"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF3C7)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta de Reincidência",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "⚠️ ATENÇÃO: PERIGO / LOCAL REINCIDENTE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = formState.recurrenceReason.ifBlank { "Já constam registros anteriores similares neste mesmo setor/local." },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // SECTION 3: IDENTIFICAÇÃO DO PERIGO E RELATO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Identificação do Perigo e Tipologia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Perigo Identificado Header & Voice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Perigo Identificado (Fonte / Situação)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { triggerVoiceInput("perigo") },
                            modifier = Modifier.testTag("btn_dictate_perigo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ditar Perigo", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = formState.perigo,
                        onValueChange = { viewModel.onPerigoChanged(it) },
                        label = { Text("Perigo Identificado") },
                        placeholder = { Text("Ex: Piso escorregadio por vazamento de óleo, fio desencapado...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { triggerVoiceInput("perigo") }) {
                                Icon(Icons.Default.Mic, contentDescription = "Ditar", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_perigo"),
                        singleLine = false,
                        maxLines = 2
                    )

                    // Ocorrência
                    SafetyDropdown(
                        label = "Tipo de Ocorrência",
                        selectedValue = formState.ocorrencia,
                        options = ocorrenciaOptions,
                        onValueChange = { viewModel.onOcorrenciaChanged(it) },
                        leadingIcon = Icons.Default.Category
                    )

                    // Classifique o Relato
                    SafetyDropdown(
                        label = "Classificação do Relato",
                        selectedValue = formState.classificacao,
                        options = classificacaoOptions,
                        onValueChange = { viewModel.onClassificacaoChanged(it) },
                        leadingIcon = Icons.Default.Info
                    )

                    // Clima
                    SafetyDropdown(
                        label = "Condição Climática",
                        selectedValue = formState.clima,
                        options = climaOptions,
                        onValueChange = { viewModel.onClimaChanged(it) },
                        leadingIcon = Icons.Default.Cloud
                    )
                }
            }

            // SECTION 4: MATRIZ DE AVALIAÇÃO E CLASSIFICAÇÃO DE RISCO (P x S)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Avaliação de Risco (Matriz P × S)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val probOptions = listOf(
                        1 to "1 - Baixa (Rara)",
                        2 to "2 - Média (Possível)",
                        3 to "3 - Alta (Provável)",
                        4 to "4 - Muito Alta (Frequente)"
                    )
                    val selectedProbText = probOptions.firstOrNull { it.first == formState.probabilidade }?.second ?: ""

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Probabilidade (P):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedProbText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            probOptions.forEach { (level, label) ->
                                val isSelected = formState.probabilidade == level
                                Button(
                                    onClick = { viewModel.onProbabilidadeChanged(level) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_prob_$level"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "P$level",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    val sevOptions = listOf(
                        1 to "1 - Leve (Sem afastamento)",
                        2 to "2 - Moderada (Primeiros socorros)",
                        3 to "3 - Grave (Com afastamento)",
                        4 to "4 - Crítica (Invalidez / Fatal)"
                    )
                    val selectedSevText = sevOptions.firstOrNull { it.first == formState.severidade }?.second ?: ""

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Severidade (S):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedSevText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sevOptions.forEach { (level, label) ->
                                val isSelected = formState.severidade == level
                                Button(
                                    onClick = { viewModel.onSeveridadeChanged(level) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_sev_$level"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "S$level",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // DYNAMIC RISK RESULT BADGE (UNIFIED SST ENGINE)
                    val riskAssessment = remember(formState.probabilidade, formState.severidade) {
                        com.example.util.SstManagementEngine.calculateRiskMatrix(formState.probabilidade, formState.severidade)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_dynamic_risk_result"),
                        colors = CardDefaults.cardColors(containerColor = riskAssessment.level.composeLightBg),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, riskAssessment.level.composeBorderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${riskAssessment.level.emoji} Classificação: ${riskAssessment.level.fullLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = riskAssessment.level.composeTextColor
                                )
                                Surface(
                                    color = riskAssessment.level.composeColor,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Score ${riskAssessment.score}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Matriz P×S: Probabilidade ${formState.probabilidade} (P) × Severidade ${formState.severidade} (S) = ${riskAssessment.score} pontos",
                                style = MaterialTheme.typography.bodySmall,
                                color = riskAssessment.level.composeTextColor.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Diretriz de Ação: ${riskAssessment.priority}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = riskAssessment.level.composeTextColor
                            )

                            if (riskAssessment.level == com.example.util.SstManagementEngine.RiskLevel.CRITICAL ||
                                riskAssessment.level == com.example.util.SstManagementEngine.RiskLevel.HIGH
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = riskAssessment.level.composeBorderColor.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Risco Elevado detectado: Esta ocorrência entrará na Central de Alertas e no Painel Gerencial SST após o salvamento.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = riskAssessment.level.composeTextColor,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: ANÁLISE CAUSAL (METODOLOGIA 6M)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Análise de Causa Raiz (6M)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Categoria 6M
                    SafetyDropdown(
                        label = "Categoria Causal (6M)",
                        selectedValue = formState.categoriaCausa,
                        options = categoriaCausaOptions,
                        onValueChange = { viewModel.onCategoriaCausaChanged(it) },
                        leadingIcon = Icons.Default.AccountTree
                    )

                    // Causa Principal
                    SafetyDropdown(
                        label = "Causa Principal Identificada",
                        selectedValue = formState.causa,
                        options = causaOptions,
                        onValueChange = { viewModel.onCausaChanged(it) },
                        leadingIcon = Icons.Default.ReportProblem
                    )

                    // Causa Secundária / Fator Contribuinte
                    OutlinedTextField(
                        value = formState.causaSecundaria,
                        onValueChange = { viewModel.onCausaSecundariaChanged(it) },
                        label = { Text("Causa Secundária / Fator Contribuinte (Opcional)") },
                        placeholder = { Text("Ex: Falta de sinalização no local, pressa na execução") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_causa_secundaria"),
                        singleLine = true
                    )
                }
            }

            // SECTION 6: LOCAL E DETALHES DO RELATO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "4. Circunstâncias e Evidências",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Local
                    OutlinedTextField(
                        value = formState.local,
                        onValueChange = { viewModel.onLocalChanged(it) },
                        label = { Text("Local Específico") },
                        placeholder = { Text("Ex: Galpão B, Linha 3, Próximo à Prensa") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_local"),
                        singleLine = true
                    )

                    // Detalhes da Ocorrência Header & Dictation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detalhes da Ocorrência",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { triggerVoiceInput("relato") },
                            modifier = Modifier.testTag("btn_dictate_relato"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Ditar Relato por Voz",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ditar por Voz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Detalhe a ocorrência do Relato
                    OutlinedTextField(
                        value = formState.relatoDetalhes,
                        onValueChange = { viewModel.onRelatoDetalhesChanged(it) },
                        label = { Text("Detalhe a ocorrência do Relato") },
                        placeholder = { Text("Descreva com detalhes o que aconteceu (ou use o botão 'Ditar por Voz')...") },
                        trailingIcon = {
                            IconButton(
                                onClick = { triggerVoiceInput("relato") },
                                modifier = Modifier.testTag("btn_mic_relato_trailing")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microfone - Ditar Relato",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("field_relato_detalhes"),
                        maxLines = 5
                    )
                }
            }

            // SECTION 7: PLANO DE AÇÃO, RESPONSÁVEL E PRAZO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "5. Plano de Ação, Responsável e SLA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Ação Imediata (Contenção)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ação Imediata (Contenção)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { triggerVoiceInput("acao") },
                            modifier = Modifier.testTag("btn_dictate_acao")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ditar Ação", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    OutlinedTextField(
                        value = formState.acaoTomada,
                        onValueChange = { viewModel.onAcaoTomadaChanged(it) },
                        label = { Text("Ação Imediata Adotada") },
                        placeholder = { Text("Ação rápida executada no momento para isolar o perigo...") },
                        trailingIcon = {
                            IconButton(onClick = { triggerVoiceInput("acao") }) {
                                Icon(Icons.Default.Mic, contentDescription = "Ditar", tint = MaterialTheme.colorScheme.secondary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .testTag("field_acao_tomada"),
                        maxLines = 3
                    )

                    // Ação Preventiva Definitiva
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ação Preventiva Definitiva",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { triggerVoiceInput("acaoPreventiva") },
                            modifier = Modifier.testTag("btn_dictate_acao_prev")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ditar Preventiva", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = formState.acaoPreventiva,
                        onValueChange = { viewModel.onAcaoPreventivaChanged(it) },
                        label = { Text("Ação Preventiva Definitiva") },
                        placeholder = { Text("Solução definitiva de engenharia, processo ou treinamento...") },
                        trailingIcon = {
                            IconButton(onClick = { triggerVoiceInput("acaoPreventiva") }) {
                                Icon(Icons.Default.Mic, contentDescription = "Ditar", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .testTag("field_acao_preventiva"),
                        maxLines = 3
                    )

                    // Responsável e Setor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.responsavelAcao,
                            onValueChange = { viewModel.onResponsavelAcaoChanged(it) },
                            label = { Text("Responsável") },
                            placeholder = { Text("Nome / Função") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("field_responsavel_acao"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = formState.setorResponsavel,
                            onValueChange = { viewModel.onSetorResponsavelChanged(it) },
                            label = { Text("Setor Responsável") },
                            placeholder = { Text("Ex: Manutenção") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("field_setor_responsavel"),
                            singleLine = true
                        )
                    }

                    // Prazo e SLA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = formState.prazoAcao,
                            onValueChange = { viewModel.onPrazoAcaoChanged(it) },
                            label = { Text("Prazo / Data Limite") },
                            placeholder = { Text("DD/MM/AAAA") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("field_prazo_acao"),
                            singleLine = true
                        )

                        // Quick SLA Fill
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SLA Rápido:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val cal = java.util.Calendar.getInstance()
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        viewModel.onPrazoAcaoChanged(sdf.format(cal.time))
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("+24h", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val cal = java.util.Calendar.getInstance()
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        viewModel.onPrazoAcaoChanged(sdf.format(cal.time))
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("+7d", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Anexo de Foto da Ocorrência
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Anexo de Foto (Evidência / Local da Ocorrência)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (formState.fotoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(formState.fotoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Anexada",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { showCameraDialog = true },
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.65f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Nova Foto da Câmera",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onFotoSelected(null) },
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.65f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover Foto",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCameraDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_capturar_camera_camerax"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreenPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tirar Foto com a Câmera (CameraX)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_anexar_galeria"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Escolher Foto da Galeria",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SECTION 5: ACTION BUTTONS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button 1: Salvar dados
                Button(
                    onClick = { viewModel.salvarDadosEPlanilha(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_salvar_dados"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafetyGreenPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !formState.isSyncing
                ) {
                    if (formState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Sincronizando...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Icon(
                            imageVector = if (formState.isSavedAndSynced) Icons.Default.CloudDone else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (formState.isSavedAndSynced) "✓ Salvo no Banco Local" else "Salvar dados",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Card
                if (formState.isSavedAndSynced) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SafetyGreenPrimary.copy(alpha = 0.12f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafetyGreenPrimary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SafetyGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Salvo com sucesso no Banco Local",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyGreenPrimary
                                )
                                Text(
                                    text = formState.lastSyncMessage.ifBlank { "A ocorrência foi salva no banco de dados local com sucesso." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Button 2: Enviar por E-mail
                Button(
                    onClick = { viewModel.enviarOcorrencia(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_enviar"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar por E-mail",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 3: Limpar Campos
                    OutlinedButton(
                        onClick = { viewModel.requestClearForm() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_limpar_campos"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyAlertRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Limpar Campos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Button 4: Fechar
                    OutlinedButton(
                        onClick = { viewModel.requestExitApp() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_fechar"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fechar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

    // DIALOG 1: Confirm Clear Fields
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearForm() },
            title = { Text("Confirmar Limpeza") },
            text = { Text("Tem certeza de que deseja limpar todos os campos preenchidos no formulário?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearForm() },
                    modifier = Modifier.testTag("btn_confirm_clear")
                ) {
                    Text("Limpar", color = SafetyAlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelClearForm() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG 2: Confirm Exit App
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelExitApp() },
            title = { Text("Fechar Aplicativo") },
            text = { Text("Tem certeza de que deseja sair do aplicativo de Segurança do Trabalho?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmExitApp() },
                    modifier = Modifier.testTag("btn_confirm_exit")
                ) {
                    Text("Sair", color = SafetyAlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelExitApp() }) {
                    Text("Continuar")
                }
            }
        )
    }

    // DIALOG 3: Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialogComponent(
            initialDate = formState.data,
            onDateSelected = { viewModel.onDateChanged(it) },
            onDismiss = { viewModel.showDatePicker.value = false }
        )
    }

    // DIALOG 4: Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialogComponent(
            onTimeSelected = { viewModel.onHoraChanged(it) },
            onDismiss = { viewModel.showTimePicker.value = false }
        )
    }

    // DIALOG 5: Email Settings Dialog
    if (showEmailSettings) {
        var emailInput by remember { mutableStateOf(formState.destinatarioEmail) }

        AlertDialog(
            onDismissRequest = { viewModel.showEmailSettingsDialog.value = false },
            title = { Text("E-mail da Equipe de Segurança") },
            text = {
                Column {
                    Text(
                        text = "Informe o endereço de e-mail de destino do setor de SST:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("E-mail do Destinatário") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDestinatarioEmailChanged(emailInput)
                        viewModel.showEmailSettingsDialog.value = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEmailSettingsDialog.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG 7: CameraX Live Camera Capture
    if (showCameraDialog) {
        CameraXCaptureDialog(
            onDismissRequest = { showCameraDialog = false },
            onPhotoCaptured = { uri ->
                viewModel.onFotoSelected(uri.toString())
                showCameraDialog = false
            }
        )
    }

    // DIALOG 8: Previous Day Pending Report Warning Dialog
    pendingReport?.let { pending ->
        AlertDialog(
            onDismissRequest = { /* Require explicit action */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SafetyAlertRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aviso: Relatório do Dia Anterior",
                        fontWeight = FontWeight.Bold,
                        color = SafetyAlertRed
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Foram identificadas ${pending.count} ocorrência(s) registrada(s) no dia anterior (${pending.previousDate}).",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Para não perder essas informações, exporte o relatório em PDF padronizado antes que a pasta de arquivos seja zerada para os registros de hoje.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.exportAndClearPreviousDayReport(context, pending.previousDate) },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyGreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar PDF de Ontem e Limpar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.discardPreviousDayReport(context, pending.previousDate) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyAlertRed)
                ) {
                    Text("Zerar Pasta Sem Exportar")
                }
            }
        )
    }

    // DIALOG: Backup and Clear Records Optimization
    if (showBackupAndClear) {
        BackupAndClearDialog(
            recordCount = occurrences.size,
            onDismiss = { viewModel.dismissBackupAndClear() },
            onBackupOnly = { format -> viewModel.performBackupOnly(context, format) },
            onBackupAndClear = { format -> viewModel.performBackupAndClearRecords(context, format) }
        )
    }

    // DIALOG: Post-Backup Inspection & Clear Confirmation
    if (postBackupVerification != null) {
        val state = postBackupVerification!!
        PostBackupClearConfirmationDialog(
            backupFileName = state.backupFileName,
            backupFormat = state.backupFormat,
            recordCount = state.recordCount,
            onConfirmClear = { viewModel.confirmClearDatabaseAfterBackup(context) },
            onKeepRecords = { viewModel.dismissPostBackupVerification() },
            onReopenFile = { viewModel.reopenBackupFile(context) }
        )
    }

    // DIALOG: Cover Configuration
    if (showCoverSettings) {
        CoverSettingsDialog(
            currentConfig = coverConfig,
            onDismiss = { viewModel.showCoverSettingsDialog.value = false },
            onSelectPreset = { resId -> viewModel.setPresetCover(context, resId) },
            onSelectCustomUri = { uri -> viewModel.setCustomCover(context, uri) },
            onResetToDefault = { viewModel.resetCoverToDefault(context) }
        )
    }

    // BOTTOM SHEET: Occurrence History
    if (showHistorySheet) {
        OccurrenceHistorySheet(
            occurrences = occurrences,
            onDismiss = { viewModel.showHistorySheet.value = false },
            onDelete = { viewModel.deleteOccurrence(it) },
            onExportPdf = { viewModel.exportSingleOccurrencePdf(context, it) },
            onBackup = { viewModel.requestBackupAndClear() },
            onBackupAndClear = { viewModel.requestBackupAndClear() },
            sheetState = sheetState
        )
    }
}
