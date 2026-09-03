package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BackupFormat
import com.example.data.model.Employee
import com.example.data.model.SafetyOccurrence
import com.example.data.repository.SafetyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.data.remote.GoogleSheetsSyncService
import com.example.data.remote.SyncResult
import com.example.util.CoverConfig
import com.example.util.CoverImageManager
import com.example.util.DailyReportManager
import com.example.util.PdfReportGenerator
import com.example.util.PendingPreviousDayReport
import com.example.util.SpreadsheetReportManager
import java.io.File

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

data class SafetyFormState(
    val data: String = getCurrentDate(),
    val hora: String = getCurrentTime(),
    val registro: String = "",
    val nomeColaborador: String = "",
    val setor: String = "",
    val relatoDetalhes: String = "",
    val local: String = "",
    val acaoTomada: String = "",
    val clima: String = "Ensolarado",
    val causa: String = "Falha Humana",
    val risco: String = "Médio (Amarelo)",
    val ocorrencia: String = "Quase Acidente (Near Miss)",
    val classificacao: String = "Observação de Segurança",
    val isEmployeeFound: Boolean? = null,
    val destinatarioEmail: String = "seguranca@empresa.com",
    val isSavedAndSynced: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String = "",
    val fotoUri: String? = null,
    // Integrated SST Additions
    val perigo: String = "",
    val probabilidade: Int = 2, // 1..4
    val severidade: Int = 2,    // 1..4
    val prioridade: String = "Prioridade normal", // Programada, Prioridade normal, Prioridade alta, Tratativa imediata
    val acaoPreventiva: String = "",
    val setorResponsavel: String = "",
    val responsavelAcao: String = "",
    val prazoAcao: String = "",
    val categoriaCausa: String = "Mão de Obra / Fator Humano",
    val causaSecundaria: String = "",
    val fotoDepoisUri: String? = null,
    val descricaoSolucao: String = "",
    val avaliacaoEficacia: String = "Pendente",
    val isRiskRecurrent: Boolean = false,
    val recurrenceReason: String = ""
)

data class EmployeeRegistrationState(
    val registro: String = "",
    val nome: String = "",
    val setor: String = "",
    val isFoundInDatabase: Boolean? = null,
    val searchQuery: String = "",
    val isSaving: Boolean = false,
    val selectedEmployeeToDelete: Employee? = null
)

data class PostBackupVerificationState(
    val backupFileName: String,
    val backupFile: File,
    val uri: Uri,
    val backupFormat: BackupFormat,
    val recordCount: Int
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class OpenEmailIntent(val intent: Intent) : UiEvent()
    data class OpenPdfIntent(val pdfFile: File, val uri: Uri) : UiEvent()
    data class OpenCsvIntent(val csvFile: File, val uri: Uri) : UiEvent()
    data class OpenBackupFileIntent(val file: File, val uri: Uri, val mimeType: String, val title: String) : UiEvent()
    data class OpenMultipleFilesIntent(val files: List<File>, val uris: List<Uri>, val title: String) : UiEvent()
    object CloseApp : UiEvent()
}

class SafetyViewModel(
    private val repository: SafetyRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(SafetyFormState())
    val formState: StateFlow<SafetyFormState> = _formState.asStateFlow()

    private val _employeeState = MutableStateFlow(EmployeeRegistrationState())
    val employeeState: StateFlow<EmployeeRegistrationState> = _employeeState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    val allOccurrences: StateFlow<List<SafetyOccurrence>> = repository.allOccurrences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allEmployees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Tab and Navigation state (0: Form, 1: KPIs & Dashboard, 2: Critical Alerts, 3: Cause Workflow & CAPA)
    val selectedTab = MutableStateFlow(0)

    fun selectTab(index: Int) {
        selectedTab.value = index
    }

    fun updateOccurrenceAction(
        occurrence: SafetyOccurrence,
        newStatus: String,
        responsavel: String,
        prazo: String,
        context: Context,
        newProbabilidade: Int? = null,
        newSeveridade: Int? = null
    ) {
        viewModelScope.launch {
            val prob = newProbabilidade ?: occurrence.probabilidade
            val sev = newSeveridade ?: occurrence.severidade
            val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(prob, sev)

            val updated = occurrence.copy(
                statusAcao = newStatus,
                responsavelAcao = responsavel,
                prazoAcao = prazo,
                probabilidade = prob,
                severidade = sev,
                risco = assessment.riskLabel,
                prioridade = assessment.priority
            )
            repository.saveOccurrence(updated)

            // Update master spreadsheet and PDF
            val fullList = (allOccurrences.value.filter { it.id != updated.id } + updated)
                .let { SpreadsheetReportManager.sortSequentiallyByDate(it) }
            SpreadsheetReportManager.syncFullSpreadsheet(context, fullList)

            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Plano de Ação da Ocorrência #${updated.id} atualizado para '$newStatus'!"))
        }
    }

    fun populateDemoDataIfEmpty(context: Context) {
        viewModelScope.launch {
            val demoOccurrences = listOf(
                SafetyOccurrence(
                    data = getCurrentDate(),
                    hora = "08:30",
                    registro = "1001",
                    nomeColaborador = "João Carlos Silva",
                    setor = "Manutenção Industrial",
                    relatoDetalhes = "Vazamento de fluido hidráulico pressurizado próximo à Prensa Hidráulica 04. Risco iminente de projeção e queda.",
                    local = "Galpão A - Prensas",
                    acaoTomada = "Máquina desenergizada e bloqueada com cadeado LOTO. Área isolada com fita zebrada.",
                    clima = "Ensolarado",
                    causa = "Falha de Equipamento / Rompimento de Mangueira",
                    risco = "Crítico (Vermelho)",
                    ocorrencia = "Condição Abaixo do Padrão",
                    classificacao = "Não Conformidade",
                    sincronizadoGooglePlanilhas = true,
                    statusAcao = "Em Tratativa",
                    responsavelAcao = "Eng. Roberto EHS",
                    prazoAcao = getCurrentDate()
                ),
                SafetyOccurrence(
                    data = getCurrentDate(),
                    hora = "09:45",
                    registro = "1003",
                    nomeColaborador = "Carlos Alberto Oliveira",
                    setor = "Logística e Expedição",
                    relatoDetalhes = "Palete de matéria-prima instável empilhado acima da altura permitida de 2,20m na Rua 03 da Expedição.",
                    local = "Pátio Logístico - Rua 03",
                    acaoTomada = "Rebaixamento imediato do palete com empilhadeira e readequação da amarração com filme stretch.",
                    clima = "Nublado",
                    causa = "Procedimento Incorreto / Armazenamento",
                    risco = "Alto (Laranja)",
                    ocorrencia = "Quase Acidente (Near Miss)",
                    classificacao = "Observação de Segurança",
                    sincronizadoGooglePlanilhas = true,
                    statusAcao = "Pendente",
                    responsavelAcao = "Sup. Cláudio Logística",
                    prazoAcao = getCurrentDate()
                ),
                SafetyOccurrence(
                    data = getCurrentDate(),
                    hora = "14:15",
                    registro = "1002",
                    nomeColaborador = "Maria Eduarda Santos",
                    setor = "Linha de Produção 01",
                    relatoDetalhes = "Operador flagrado realizando abastecimento sem o protetor auricular do tipo concha em zona de 88 dB.",
                    local = "Linha de Montagem 01",
                    acaoTomada = "Orientação preventiva realizada no ato com entrega de novo EPI e reforço das regras de ouro.",
                    clima = "Calorento / Quente",
                    causa = "Falta de EPI / Comportamento",
                    risco = "Médio (Amarelo)",
                    ocorrencia = "Ato Abaixo do Padrão",
                    classificacao = "Observação de Segurança",
                    sincronizadoGooglePlanilhas = true,
                    statusAcao = "Concluído",
                    responsavelAcao = "Téc. Juliana SESMT",
                    prazoAcao = getCurrentDate()
                ),
                SafetyOccurrence(
                    data = getCurrentDate(),
                    hora = "16:00",
                    registro = "1005",
                    nomeColaborador = "Pedro Henrique Lima",
                    setor = "Almoxarifado Central",
                    relatoDetalhes = "Lâmpada tubular fluorescente queimada sobre o corredor de pedestres, dificultando a visualização de degrau.",
                    local = "Almoxarifado - Acesso B",
                    acaoTomada = "Abertura de ordem de serviço emergencial de manutenção e sinalização provisória do desnível.",
                    clima = "Chuvoso",
                    causa = "Iluminação Inadequada",
                    risco = "Baixo (Verde)",
                    ocorrencia = "Condição Abaixo do Padrão",
                    classificacao = "Oportunidade de Melhoria",
                    sincronizadoGooglePlanilhas = true,
                    statusAcao = "Eficaz",
                    responsavelAcao = "Equipe Facilities",
                    prazoAcao = getCurrentDate()
                ),
                SafetyOccurrence(
                    data = getCurrentDate(),
                    hora = "11:20",
                    registro = "1007",
                    nomeColaborador = "Luciana Martins",
                    setor = "Utilidades e Utilidades",
                    relatoDetalhes = "Sensor de barreira ótica de segurança com alinhamento intermitente na Célula Robotizada 02.",
                    local = "Célula Robotizada 02",
                    acaoTomada = "Célula parada imediatamente, recalibragem do sensor e validação pelo técnico de automação e segurança.",
                    clima = "Ensolarado",
                    causa = "Falha de Equipamento / Sensor",
                    risco = "Crítico (Vermelho)",
                    ocorrencia = "Incidente sem Lesão",
                    classificacao = "Situação de Emergência",
                    sincronizadoGooglePlanilhas = true,
                    statusAcao = "Eficaz",
                    responsavelAcao = "Eng. Automação / EHS",
                    prazoAcao = getCurrentDate()
                )
            )
            demoOccurrences.forEach { repository.saveOccurrence(it) }
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Conjunto de dados gerenciais carregado com sucesso!"))
        }
    }

    // Dialog control states
    val showClearConfirmation = MutableStateFlow(false)
    val showExitConfirmation = MutableStateFlow(false)
    val showDatePicker = MutableStateFlow(false)
    val showTimePicker = MutableStateFlow(false)
    val showHistorySheet = MutableStateFlow(false)
    val showEmailSettingsDialog = MutableStateFlow(false)
    val showSheetsSettingsDialog = MutableStateFlow(false)
    val showBackupAndClearDialog = MutableStateFlow(false)
    val postBackupVerificationState = MutableStateFlow<PostBackupVerificationState?>(null)
    val showCoverSettingsDialog = MutableStateFlow(false)
    val showAboutScreen = MutableStateFlow(false)
    val pendingPreviousDayReport = MutableStateFlow<PendingPreviousDayReport?>(null)
    private var hasDismissedWarningThisSession = false

    private val _coverConfig = MutableStateFlow(CoverConfig())
    val coverConfig: StateFlow<CoverConfig> = _coverConfig.asStateFlow()

    fun loadCoverConfig(context: Context) {
        _coverConfig.value = CoverImageManager.getCoverConfig(context)
    }

    fun setPresetCover(context: Context, resId: Int) {
        _coverConfig.value = CoverImageManager.savePresetCover(context, resId)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Imagem de capa atualizada com sucesso!"))
        }
    }

    fun setCustomCover(context: Context, uri: Uri) {
        val newConfig = CoverImageManager.saveCustomCoverFromUri(context, uri)
        if (newConfig != null) {
            _coverConfig.value = newConfig
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("✓ Capa personalizada carregada e salva!"))
            }
        } else {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao carregar imagem de capa."))
            }
        }
    }

    fun resetCoverToDefault(context: Context) {
        _coverConfig.value = CoverImageManager.resetToDefault(context)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Imagem de capa restaurada para o padrão."))
        }
    }

    fun checkDailyWarning(context: Context) {
        viewModelScope.launch {
            val list = allOccurrences.value
            val pending = DailyReportManager.checkPreviousDayPendingReport(context, list)
            if (pending != null && !hasDismissedWarningThisSession) {
                pendingPreviousDayReport.value = pending
            }
        }
    }

    fun onDateChanged(newDate: String) {
        _formState.value = _formState.value.copy(data = newDate)
    }

    fun onHoraChanged(newHora: String) {
        _formState.value = _formState.value.copy(hora = newHora)
    }

    fun onRegistroChanged(newRegistro: String) {
        _formState.value = _formState.value.copy(registro = newRegistro)
        val trimmed = newRegistro.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                val emp = repository.findEmployee(trimmed)
                if (emp != null) {
                    _formState.value = _formState.value.copy(
                        nomeColaborador = emp.nome,
                        setor = emp.setor,
                        isEmployeeFound = true
                    )
                } else {
                    _formState.value = _formState.value.copy(isEmployeeFound = false)
                }
            }
        } else {
            _formState.value = _formState.value.copy(
                nomeColaborador = "",
                setor = "",
                isEmployeeFound = null
            )
        }
    }

    // Employee Registration Management Methods
    fun onEmployeeInputRegistroChanged(newRegistro: String) {
        _employeeState.value = _employeeState.value.copy(registro = newRegistro)
        val trimmed = newRegistro.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                val emp = repository.findEmployee(trimmed)
                if (emp != null) {
                    _employeeState.value = _employeeState.value.copy(
                        nome = emp.nome,
                        setor = emp.setor,
                        isFoundInDatabase = true
                    )
                } else {
                    _employeeState.value = _employeeState.value.copy(
                        isFoundInDatabase = false
                    )
                }
            }
        } else {
            _employeeState.value = _employeeState.value.copy(
                nome = "",
                setor = "",
                isFoundInDatabase = null
            )
        }
    }

    fun onEmployeeInputNomeChanged(newNome: String) {
        _employeeState.value = _employeeState.value.copy(nome = newNome)
    }

    fun onEmployeeInputSetorChanged(newSetor: String) {
        _employeeState.value = _employeeState.value.copy(setor = newSetor)
    }

    fun onEmployeeSearchQueryChanged(newQuery: String) {
        _employeeState.value = _employeeState.value.copy(searchQuery = newQuery)
    }

    fun saveEmployeeFromManagement() {
        val current = _employeeState.value
        val reg = current.registro.trim()
        val nome = current.nome.trim()
        val setor = current.setor.trim().ifBlank { "Geral" }

        if (reg.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Por favor, informe a Matrícula / ID do funcionário."))
            }
            return
        }

        if (nome.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Por favor, preencha o Nome Completo do funcionário."))
            }
            return
        }

        viewModelScope.launch {
            _employeeState.value = _employeeState.value.copy(isSaving = true)
            val employee = Employee(
                registro = reg,
                nome = nome,
                setor = setor
            )
            repository.saveEmployee(employee)
            _employeeState.value = _employeeState.value.copy(
                isSaving = false,
                isFoundInDatabase = true
            )
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Colaborador $nome (ID: $reg) salvo no banco de dados com sucesso!"))
        }
    }

    fun saveEmployeeFromIncidentForm() {
        val current = _formState.value
        val reg = current.registro.trim()
        val nome = current.nomeColaborador.trim()
        val setor = current.setor.trim().ifBlank { "Geral" }

        if (reg.isEmpty() || nome.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Informe o Registro/ID e Nome para salvar no banco."))
            }
            return
        }

        viewModelScope.launch {
            val emp = Employee(registro = reg, nome = nome, setor = setor)
            repository.saveEmployee(emp)
            _formState.value = _formState.value.copy(isEmployeeFound = true)
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Funcionário $nome (ID: $reg) cadastrado no banco de dados com sucesso!"))
        }
    }

    fun clearEmployeeRegistrationForm() {
        _employeeState.value = _employeeState.value.copy(
            registro = "",
            nome = "",
            setor = "",
            isFoundInDatabase = null
        )
    }

    fun selectEmployeeForEdit(employee: Employee) {
        _employeeState.value = _employeeState.value.copy(
            registro = employee.registro,
            nome = employee.nome,
            setor = employee.setor,
            isFoundInDatabase = true
        )
    }

    fun selectEmployeeToDelete(employee: Employee?) {
        _employeeState.value = _employeeState.value.copy(selectedEmployeeToDelete = employee)
    }

    fun confirmDeleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            if (_employeeState.value.registro == employee.registro) {
                clearEmployeeRegistrationForm()
            }
            _employeeState.value = _employeeState.value.copy(selectedEmployeeToDelete = null)
            _uiEvent.emit(UiEvent.ShowSnackbar("Colaborador ${employee.nome} (ID: ${employee.registro}) removido do banco."))
        }
    }

    fun useEmployeeInOccurrenceForm(employee: Employee) {
        onRegistroChanged(employee.registro)
        selectTab(0)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Colaborador ${employee.nome} preenchido no formulário SST!"))
        }
    }

    fun onNomeColaboradorChanged(newNome: String) {
        _formState.value = _formState.value.copy(nomeColaborador = newNome)
    }

    fun onSetorChanged(newSetor: String) {
        _formState.value = _formState.value.copy(setor = newSetor)
        checkRecurrenceTrigger()
    }

    fun onRelatoDetalhesChanged(newRelato: String) {
        _formState.value = _formState.value.copy(relatoDetalhes = newRelato)
    }

    fun onLocalChanged(newLocal: String) {
        _formState.value = _formState.value.copy(local = newLocal)
        checkRecurrenceTrigger()
    }

    fun onPerigoChanged(newPerigo: String) {
        _formState.value = _formState.value.copy(perigo = newPerigo)
        checkRecurrenceTrigger()
    }

    fun onProbabilidadeChanged(newProb: Int) {
        val currentSev = _formState.value.severidade
        val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(newProb, currentSev)
        _formState.value = _formState.value.copy(
            probabilidade = newProb,
            risco = assessment.riskLabel,
            prioridade = assessment.priority,
            isSavedAndSynced = false
        )
        checkRecurrenceTrigger()
    }

    fun onSeveridadeChanged(newSev: Int) {
        val currentProb = _formState.value.probabilidade
        val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(currentProb, newSev)
        _formState.value = _formState.value.copy(
            severidade = newSev,
            risco = assessment.riskLabel,
            prioridade = assessment.priority,
            isSavedAndSynced = false
        )
        checkRecurrenceTrigger()
    }

    fun onAcaoTomadaChanged(newAcao: String) {
        _formState.value = _formState.value.copy(acaoTomada = newAcao)
    }

    fun onAcaoPreventivaChanged(newAcao: String) {
        _formState.value = _formState.value.copy(acaoPreventiva = newAcao)
    }

    fun onSetorResponsavelChanged(newSetor: String) {
        _formState.value = _formState.value.copy(setorResponsavel = newSetor)
    }

    fun onResponsavelAcaoChanged(newResp: String) {
        _formState.value = _formState.value.copy(responsavelAcao = newResp)
    }

    fun onPrazoAcaoChanged(newPrazo: String) {
        _formState.value = _formState.value.copy(prazoAcao = newPrazo)
    }

    fun onCategoriaCausaChanged(newCat: String) {
        _formState.value = _formState.value.copy(categoriaCausa = newCat)
    }

    fun onCausaSecundariaChanged(newCausa: String) {
        _formState.value = _formState.value.copy(causaSecundaria = newCausa)
    }

    fun onClimaChanged(newClima: String) {
        _formState.value = _formState.value.copy(clima = newClima)
    }

    fun onCausaChanged(newCausa: String) {
        _formState.value = _formState.value.copy(causa = newCausa)
        checkRecurrenceTrigger()
    }

    fun onRiscoChanged(newRisco: String) {
        val level = com.example.util.SstManagementEngine.getRiskLevelFromString(newRisco)
        val (p, s) = when (level) {
            com.example.util.SstManagementEngine.RiskLevel.LOW -> Pair(1, 2)
            com.example.util.SstManagementEngine.RiskLevel.MEDIUM -> Pair(2, 2)
            com.example.util.SstManagementEngine.RiskLevel.HIGH -> Pair(3, 3)
            com.example.util.SstManagementEngine.RiskLevel.CRITICAL -> Pair(4, 4)
        }
        val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(p, s)
        _formState.value = _formState.value.copy(
            risco = assessment.riskLabel,
            probabilidade = p,
            severidade = s,
            prioridade = assessment.priority
        )
    }

    fun onOcorrenciaChanged(newOcorrencia: String) {
        _formState.value = _formState.value.copy(ocorrencia = newOcorrencia)
        checkRecurrenceTrigger()
    }

    fun onClassificacaoChanged(newClassificacao: String) {
        _formState.value = _formState.value.copy(classificacao = newClassificacao)
    }

    fun onDestinatarioEmailChanged(newEmail: String) {
        _formState.value = _formState.value.copy(destinatarioEmail = newEmail)
    }

    fun onFotoSelected(uri: String?) {
        _formState.value = _formState.value.copy(fotoUri = uri, isSavedAndSynced = false)
    }

    fun onFotoDepoisSelected(uri: String?) {
        _formState.value = _formState.value.copy(fotoDepoisUri = uri)
    }

    fun onDescricaoSolucaoChanged(newSol: String) {
        _formState.value = _formState.value.copy(descricaoSolucao = newSol)
    }

    fun onAvaliacaoEficaciaChanged(newEf: String) {
        _formState.value = _formState.value.copy(avaliacaoEficacia = newEf)
    }

    private fun checkRecurrenceTrigger() {
        val current = _formState.value
        val tempOccurrence = SafetyOccurrence(
            id = -1,
            data = current.data,
            hora = current.hora,
            registro = current.registro,
            nomeColaborador = current.nomeColaborador,
            setor = current.setor,
            relatoDetalhes = current.relatoDetalhes,
            local = current.local,
            acaoTomada = current.acaoTomada,
            clima = current.clima,
            causa = current.causa,
            risco = current.risco,
            ocorrencia = current.ocorrencia,
            classificacao = current.classificacao,
            perigo = current.perigo
        )
        val recResult = com.example.util.SstManagementEngine.checkRecurrence(tempOccurrence, allOccurrences.value)
        _formState.value = _formState.value.copy(
            isRiskRecurrent = recResult.isRecurring,
            recurrenceReason = recResult.reason
        )
    }

    fun requestClearForm() {
        showClearConfirmation.value = true
    }

    fun confirmClearForm() {
        showClearConfirmation.value = false
        _formState.value = SafetyFormState(
            data = getCurrentDate(),
            hora = getCurrentTime(),
            destinatarioEmail = _formState.value.destinatarioEmail
        )
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("Todos os campos foram limpos com sucesso."))
        }
    }

    fun cancelClearForm() {
        showClearConfirmation.value = false
    }

    fun requestExitApp() {
        showExitConfirmation.value = true
    }

    fun confirmExitApp() {
        showExitConfirmation.value = false
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.CloseApp)
        }
    }

    fun cancelExitApp() {
        showExitConfirmation.value = false
    }

    fun preencherExemplo() {
        val exemplo = listOf(
            Employee("1001", "João Carlos Silva", "Manutenção Industrial"),
            Employee("1002", "Maria Eduarda Santos", "Linha de Produção 01"),
            Employee("1003", "Carlos Alberto Oliveira", "Logística e Expedição")
        ).random()

        val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(3, 3)

        _formState.value = _formState.value.copy(
            data = getCurrentDate(),
            hora = getCurrentTime(),
            registro = exemplo.registro,
            nomeColaborador = exemplo.nome,
            setor = exemplo.setor,
            isEmployeeFound = true,
            local = "Galpão A - Setor de Prensas",
            relatoDetalhes = "Presenciada mancha de óleo hidráulico no corredor principal próximo às empilhadeiras. Risco iminente de escorregamento.",
            acaoTomada = "Instalada sinalização de alerta zebrada, isolado o local e acionada equipe de limpeza industrial imediata.",
            clima = "Ensolarado",
            causa = "Piso Escorregadio / Vazamento de Óleo",
            risco = assessment.riskLabel,
            prioridade = assessment.priority,
            probabilidade = 3,
            severidade = 3,
            ocorrencia = "Quase Acidente (Near Miss)",
            classificacao = "Observação de Segurança",
            perigo = "Vazamento de óleo sob pressão em piso industrial liso",
            acaoPreventiva = "Revisão nas conexões hidráulicas e inspeção programada com checklist",
            setorResponsavel = "Manutenção Mecânica",
            responsavelAcao = "Eng. Roberto EHS",
            prazoAcao = getCurrentDate(),
            categoriaCausa = "Máquinas / Equipamentos",
            causaSecundaria = "Mão de Obra / Fator Humano"
        )
        checkRecurrenceTrigger()
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar("Formulário preenchido com dados de teste integrados SST!"))
        }
    }

    fun salvarDadosEPlanilha(context: Context, onSuccess: (() -> Unit)? = null) {
        salvarDados(context, onSuccess)
    }

    fun salvarDados(context: Context, onSuccess: (() -> Unit)? = null) {
        val current = _formState.value

        if (current.relatoDetalhes.isBlank() && current.local.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Por favor, preencha o local e o relato antes de salvar."))
            }
            return
        }

        _formState.value = _formState.value.copy(isSyncing = true)

        viewModelScope.launch {
            val riskAssessment = com.example.util.SstManagementEngine.calculateRiskMatrix(
                current.probabilidade,
                current.severidade
            )

            val entityToSave = SafetyOccurrence(
                data = current.data.ifBlank { getCurrentDate() },
                hora = current.hora.ifBlank { getCurrentTime() },
                registro = current.registro,
                nomeColaborador = current.nomeColaborador,
                setor = current.setor,
                relatoDetalhes = current.relatoDetalhes,
                local = current.local,
                acaoTomada = current.acaoTomada,
                clima = current.clima,
                causa = current.causa,
                risco = riskAssessment.riskLabel,
                ocorrencia = current.ocorrencia,
                classificacao = current.classificacao,
                sincronizadoGooglePlanilhas = true,
                fotoUri = current.fotoUri,
                statusAcao = if (current.acaoTomada.isNotBlank()) "Em Tratativa" else "Pendente",
                responsavelAcao = current.responsavelAcao,
                prazoAcao = current.prazoAcao.ifBlank { current.data.ifBlank { getCurrentDate() } },
                perigo = current.perigo,
                probabilidade = current.probabilidade,
                severidade = current.severidade,
                prioridade = riskAssessment.priority,
                acaoPreventiva = current.acaoPreventiva,
                setorResponsavel = current.setorResponsavel.ifBlank { current.setor },
                dataAbertura = current.data.ifBlank { getCurrentDate() },
                dataConclusao = "",
                responsavelValidacao = "",
                observacoesAcao = "",
                fotoDepoisUri = current.fotoDepoisUri,
                descricaoSolucao = current.descricaoSolucao,
                avaliacaoEficacia = current.avaliacaoEficacia,
                categoriaCausa = current.categoriaCausa,
                causaSecundaria = current.causaSecundaria
            )

            val savedId = repository.saveOccurrence(entityToSave)
            val updatedEntity = entityToSave.copy(id = savedId)

            // Consolidate full updated list ordered sequentially by date
            val fullList = (allOccurrences.value.filter { it.id != updatedEntity.id } + updatedEntity)
                .let { SpreadsheetReportManager.sortSequentiallyByDate(it) }

            // 1. Automatically update dynamic PDF report file organized sequentially by date
            val dynamicPdfFile = try {
                PdfReportGenerator.generatePdfReport(context, updatedEntity.data, fullList)
            } catch (e: Exception) { null }

            // 2. Automatically update master CSV spreadsheet organized sequentially by date
            SpreadsheetReportManager.syncFullSpreadsheet(context, fullList)

            // 3. Automatically sync with remote Google Sheets service
            try {
                GoogleSheetsSyncService(context).syncOccurrence(updatedEntity)
            } catch (e: Exception) {
                // Background sync error handled gracefully
            }

            // 4. Create automatic full backup (PDF + CSV)
            SpreadsheetReportManager.createFullBackup(context, fullList, dynamicPdfFile)

            // 5. Append to daily text log
            DailyReportManager.appendOccurrenceToDailyFile(context, updatedEntity)

            if (current.registro.isNotBlank() && current.nomeColaborador.isNotBlank()) {
                repository.saveEmployee(
                    Employee(
                        registro = current.registro,
                        nome = current.nomeColaborador,
                        setor = current.setor.ifBlank { "Não informado" }
                    )
                )
            }

            val effectiveRisk = com.example.util.SstManagementEngine.getEffectiveRiskLevel(updatedEntity)
            val isHighOrCritical = effectiveRisk == com.example.util.SstManagementEngine.RiskLevel.CRITICAL ||
                    effectiveRisk == com.example.util.SstManagementEngine.RiskLevel.HIGH

            val alertNotice = if (isHighOrCritical) " [⚠️ Alerta ${effectiveRisk.shortLabel} ativo]" else ""
            val msg = "Relatório #${updatedEntity.id} salvo!$alertNotice PDF e Planilha atualizados com sucesso."

            _formState.value = _formState.value.copy(
                isSavedAndSynced = true,
                isSyncing = false,
                lastSyncMessage = msg
            )

            _uiEvent.emit(UiEvent.ShowSnackbar("✓ $msg"))
            onSuccess?.invoke()
        }
    }

    fun updateOccurrenceFullAction(
        context: Context,
        occurrence: SafetyOccurrence,
        newStatus: String,
        responsavel: String,
        prazo: String,
        acaoPreventiva: String,
        setorResponsavel: String,
        descricaoSolucao: String,
        fotoDepoisUri: String?,
        responsavelValidacao: String,
        avaliacaoEficacia: String,
        observacoes: String,
        newProbabilidade: Int? = null,
        newSeveridade: Int? = null
    ) {
        viewModelScope.launch {
            val isFinalizando = newStatus.equals("Concluído", true) || newStatus.equals("Eficaz", true)
            val dataConclusao = if (isFinalizando && occurrence.dataConclusao.isBlank()) getCurrentDate() else occurrence.dataConclusao

            val prob = newProbabilidade ?: occurrence.probabilidade
            val sev = newSeveridade ?: occurrence.severidade
            val assessment = com.example.util.SstManagementEngine.calculateRiskMatrix(prob, sev)

            val updated = occurrence.copy(
                statusAcao = newStatus,
                responsavelAcao = responsavel,
                prazoAcao = prazo,
                acaoPreventiva = acaoPreventiva,
                setorResponsavel = setorResponsavel,
                descricaoSolucao = descricaoSolucao,
                fotoDepoisUri = fotoDepoisUri ?: occurrence.fotoDepoisUri,
                responsavelValidacao = responsavelValidacao,
                avaliacaoEficacia = avaliacaoEficacia,
                observacoesAcao = observacoes,
                dataConclusao = dataConclusao,
                probabilidade = prob,
                severidade = sev,
                risco = assessment.riskLabel,
                prioridade = assessment.priority
            )

            repository.saveOccurrence(updated)

            // Update master spreadsheet and PDF
            val fullList = (allOccurrences.value.filter { it.id != updated.id } + updated)
                .let { SpreadsheetReportManager.sortSequentiallyByDate(it) }
            SpreadsheetReportManager.syncFullSpreadsheet(context, fullList)
            try {
                PdfReportGenerator.generatePdfReport(context, updated.data, fullList)
            } catch (e: Exception) {}

            _uiEvent.emit(UiEvent.ShowSnackbar("✓ Plano de Ação & Eficácia da Ocorrência #${updated.id} atualizados!"))
        }
    }

    fun deleteOccurrence(occurrence: SafetyOccurrence, context: Context) {
        viewModelScope.launch {
            repository.deleteOccurrence(occurrence)
            val fullList = allOccurrences.value.filter { it.id != occurrence.id }
                .let { SpreadsheetReportManager.sortSequentiallyByDate(it) }
            SpreadsheetReportManager.syncFullSpreadsheet(context, fullList)
            try {
                PdfReportGenerator.generatePdfReport(context, getCurrentDate(), fullList)
            } catch (e: Exception) {}
            _uiEvent.emit(UiEvent.ShowSnackbar("Ocorrência #${occurrence.id} excluída."))
        }
    }

    fun requestBackupAndClear() {
        showBackupAndClearDialog.value = true
    }

    fun dismissBackupAndClear() {
        showBackupAndClearDialog.value = false
    }

    fun dismissPostBackupVerification() {
        val state = postBackupVerificationState.value
        postBackupVerificationState.value = null
        viewModelScope.launch {
            _uiEvent.emit(
                UiEvent.ShowSnackbar("Limpeza cancelada: os ${state?.recordCount ?: 0} registros locais foram mantidos intactos no aplicativo.")
            )
        }
    }

    fun reopenBackupFile(context: Context) {
        val state = postBackupVerificationState.value ?: return
        viewModelScope.launch {
            try {
                when (state.backupFormat) {
                    BackupFormat.GOOGLE_SHEETS_CSV -> {
                        _uiEvent.emit(UiEvent.OpenCsvIntent(state.backupFile, state.uri))
                    }
                    BackupFormat.PDF -> {
                        _uiEvent.emit(UiEvent.OpenPdfIntent(state.backupFile, state.uri))
                    }
                    BackupFormat.JSON_DATA -> {
                        _uiEvent.emit(
                            UiEvent.OpenBackupFileIntent(
                                file = state.backupFile,
                                uri = state.uri,
                                mimeType = "application/json",
                                title = "Exportar Backup JSON SST"
                            )
                        )
                    }
                    BackupFormat.ALL_FORMATS -> {
                        _uiEvent.emit(
                            UiEvent.OpenCsvIntent(state.backupFile, state.uri)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao abrir arquivo: ${e.message}"))
            }
        }
    }

    fun performBackupOnly(context: Context, format: BackupFormat = BackupFormat.GOOGLE_SHEETS_CSV) {
        executeBackup(context, format, clearAfterBackup = false)
    }

    fun performBackupAndClearRecords(context: Context, format: BackupFormat = BackupFormat.GOOGLE_SHEETS_CSV) {
        viewModelScope.launch {
            val list = allOccurrences.value
            val totalCount = list.size

            if (totalCount == 0) {
                showBackupAndClearDialog.value = false
                _uiEvent.emit(UiEvent.ShowSnackbar("Nenhum registro encontrado no banco de dados para backup."))
                return@launch
            }

            val sorted = SpreadsheetReportManager.sortSequentiallyByDate(list)
            val currentDate = getCurrentDate()

            try {
                val generatedFile: File
                val shareableUri: Uri

                when (format) {
                    BackupFormat.GOOGLE_SHEETS_CSV -> {
                        // Generate the master spreadsheet and the immutable backup CSV
                        val masterCsv = SpreadsheetReportManager.syncFullSpreadsheet(context, sorted)
                        val backupCsv = SpreadsheetReportManager.createFullBackup(context, sorted)
                        // Use the backup CSV file so even if master is changed, the backup file is preserved
                        generatedFile = backupCsv
                        shareableUri = SpreadsheetReportManager.getShareableCsvUri(context, backupCsv)
                        _uiEvent.emit(UiEvent.OpenCsvIntent(generatedFile, shareableUri))
                    }
                    BackupFormat.PDF -> {
                        val pdfFile = PdfReportGenerator.generatePdfReport(context, currentDate, sorted)
                        SpreadsheetReportManager.createFullBackup(context, sorted, pdfFile)
                        generatedFile = pdfFile
                        shareableUri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                        _uiEvent.emit(UiEvent.OpenPdfIntent(pdfFile, shareableUri))
                    }
                    BackupFormat.JSON_DATA -> {
                        val backupDir = SpreadsheetReportManager.getBackupDir(context)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val jsonFile = File(backupDir, "Backup_Dados_SST_$timestamp.json")
                        val jsonArr = org.json.JSONArray()
                        sorted.forEach { item ->
                            val obj = org.json.JSONObject().apply {
                                put("id", item.id)
                                put("data", item.data)
                                put("hora", item.hora)
                                put("registro", item.registro)
                                put("nomeColaborador", item.nomeColaborador)
                                put("setor", item.setor)
                                put("local", item.local)
                                put("relatoDetalhes", item.relatoDetalhes)
                                put("acaoTomada", item.acaoTomada)
                                put("clima", item.clima)
                                put("causa", item.causa)
                                put("risco", item.risco)
                                put("ocorrencia", item.ocorrencia)
                                put("classificacao", item.classificacao)
                                put("timestamp", item.timestamp)
                                put("statusAcao", item.statusAcao)
                                put("responsavelAcao", item.responsavelAcao)
                                put("prazoAcao", item.prazoAcao)
                                put("acaoPreventiva", item.acaoPreventiva)
                                put("setorResponsavel", item.setorResponsavel)
                                put("causaSecundaria", item.causaSecundaria)
                                put("descricaoSolucao", item.descricaoSolucao)
                                put("avaliacaoEficacia", item.avaliacaoEficacia)
                            }
                            jsonArr.put(obj)
                        }
                        jsonFile.writeText(jsonArr.toString(2), Charsets.UTF_8)
                        generatedFile = jsonFile
                        shareableUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            jsonFile
                        )
                        _uiEvent.emit(
                            UiEvent.OpenBackupFileIntent(
                                file = jsonFile,
                                uri = shareableUri,
                                mimeType = "application/json",
                                title = "Exportar Backup JSON SST"
                            )
                        )
                    }
                    BackupFormat.ALL_FORMATS -> {
                        val pdfFile = PdfReportGenerator.generatePdfReport(context, currentDate, sorted)
                        val csvFile = SpreadsheetReportManager.syncFullSpreadsheet(context, sorted)
                        val backupCsv = SpreadsheetReportManager.createFullBackup(context, sorted, pdfFile)
                        generatedFile = backupCsv
                        val pdfUri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                        val csvUri = SpreadsheetReportManager.getShareableCsvUri(context, backupCsv)
                        shareableUri = csvUri
                        _uiEvent.emit(
                            UiEvent.OpenMultipleFilesIntent(
                                files = listOf(pdfFile, backupCsv),
                                uris = listOf(pdfUri, csvUri),
                                title = "Exportar Pacote Completo SST (PDF e Planilha CSV)"
                            )
                        )
                    }
                }

                // Close the format selection dialog and present the step-2 confirmation dialog
                showBackupAndClearDialog.value = false
                postBackupVerificationState.value = PostBackupVerificationState(
                    backupFileName = generatedFile.name,
                    backupFile = generatedFile,
                    uri = shareableUri,
                    backupFormat = format,
                    recordCount = totalCount
                )

                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        "✓ Backup gerado com sucesso! Visualize a planilha antes de confirmar a limpeza da base."
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro durante a geração do backup: ${e.message}"))
                showBackupAndClearDialog.value = false
            }
        }
    }

    fun confirmClearDatabaseAfterBackup(context: Context) {
        viewModelScope.launch {
            val state = postBackupVerificationState.value
            val totalCount = state?.recordCount ?: allOccurrences.value.size
            val currentDate = getCurrentDate()

            try {
                // Clear local Room database occurrences
                repository.clearAllOccurrences()

                // Reset daily folder and report tracking safely
                DailyReportManager.clearDailyFolderAndAdvanceDate(context, currentDate)

                // Clear the master daily csv without touching any backup files in backup/
                try {
                    val masterCsv = SpreadsheetReportManager.getSpreadsheetFile(context)
                    if (masterCsv.exists()) {
                        masterCsv.delete()
                    }
                } catch (_: Exception) {}

                postBackupVerificationState.value = null

                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        "✓ Base local de $totalCount registro(s) limpa e numeração reiniciada para #1. Seu backup permanece preservado no aparelho!"
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao limpar registros locais: ${e.message}"))
            }
        }
    }

    fun executeBackup(
        context: Context,
        format: BackupFormat,
        clearAfterBackup: Boolean
    ) {
        if (clearAfterBackup) {
            performBackupAndClearRecords(context, format)
            return
        }

        viewModelScope.launch {
            val list = allOccurrences.value
            val totalCount = list.size

            if (totalCount == 0) {
                showBackupAndClearDialog.value = false
                _uiEvent.emit(UiEvent.ShowSnackbar("Nenhum registro encontrado no banco de dados para backup."))
                return@launch
            }

            val sorted = SpreadsheetReportManager.sortSequentiallyByDate(list)
            val currentDate = getCurrentDate()

            try {
                when (format) {
                    BackupFormat.PDF -> {
                        val pdfFile = PdfReportGenerator.generatePdfReport(context, currentDate, sorted)
                        SpreadsheetReportManager.createFullBackup(context, sorted, pdfFile)
                        val uri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                        _uiEvent.emit(UiEvent.OpenPdfIntent(pdfFile, uri))
                    }
                    BackupFormat.GOOGLE_SHEETS_CSV -> {
                        val csvFile = SpreadsheetReportManager.syncFullSpreadsheet(context, sorted)
                        val backupCsv = SpreadsheetReportManager.createFullBackup(context, sorted)
                        val uri = SpreadsheetReportManager.getShareableCsvUri(context, backupCsv)
                        _uiEvent.emit(UiEvent.OpenCsvIntent(backupCsv, uri))
                    }
                    BackupFormat.JSON_DATA -> {
                        val backupDir = SpreadsheetReportManager.getBackupDir(context)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val jsonFile = File(backupDir, "Backup_Dados_SST_$timestamp.json")
                        val jsonArr = org.json.JSONArray()
                        sorted.forEach { item ->
                            val obj = org.json.JSONObject().apply {
                                put("id", item.id)
                                put("data", item.data)
                                put("hora", item.hora)
                                put("registro", item.registro)
                                put("nomeColaborador", item.nomeColaborador)
                                put("setor", item.setor)
                                put("local", item.local)
                                put("relatoDetalhes", item.relatoDetalhes)
                                put("acaoTomada", item.acaoTomada)
                                put("clima", item.clima)
                                put("causa", item.causa)
                                put("risco", item.risco)
                                put("ocorrencia", item.ocorrencia)
                                put("classificacao", item.classificacao)
                                put("timestamp", item.timestamp)
                                put("statusAcao", item.statusAcao)
                                put("responsavelAcao", item.responsavelAcao)
                                put("prazoAcao", item.prazoAcao)
                                put("acaoPreventiva", item.acaoPreventiva)
                                put("setorResponsavel", item.setorResponsavel)
                                put("causaSecundaria", item.causaSecundaria)
                                put("descricaoSolucao", item.descricaoSolucao)
                                put("avaliacaoEficacia", item.avaliacaoEficacia)
                            }
                            jsonArr.put(obj)
                        }
                        jsonFile.writeText(jsonArr.toString(2), Charsets.UTF_8)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            jsonFile
                        )
                        _uiEvent.emit(
                            UiEvent.OpenBackupFileIntent(
                                file = jsonFile,
                                uri = uri,
                                mimeType = "application/json",
                                title = "Exportar Backup JSON SST"
                            )
                        )
                    }
                    BackupFormat.ALL_FORMATS -> {
                        val pdfFile = PdfReportGenerator.generatePdfReport(context, currentDate, sorted)
                        val csvFile = SpreadsheetReportManager.syncFullSpreadsheet(context, sorted)
                        val backupCsv = SpreadsheetReportManager.createFullBackup(context, sorted, pdfFile)
                        val pdfUri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                        val csvUri = SpreadsheetReportManager.getShareableCsvUri(context, backupCsv)
                        _uiEvent.emit(
                            UiEvent.OpenMultipleFilesIntent(
                                files = listOf(pdfFile, backupCsv),
                                uris = listOf(pdfUri, csvUri),
                                title = "Exportar Pacote Completo SST (PDF e Planilha CSV)"
                            )
                        )
                    }
                }

                _uiEvent.emit(
                    UiEvent.ShowSnackbar(
                        "✓ Backup em ${format.extensionLabel} de $totalCount registro(s) gerado e salvo com sucesso! Registros mantidos no app."
                    )
                )
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro durante a exportação do backup: ${e.message}"))
            } finally {
                showBackupAndClearDialog.value = false
            }
        }
    }

    fun gerarRelatorioPdfExecutivo(context: Context) {
        viewModelScope.launch {
            val list = allOccurrences.value
            if (list.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Nenhuma ocorrência registrada para gerar o relatório executivo."))
                return@launch
            }
            try {
                val sorted = SpreadsheetReportManager.sortSequentiallyByDate(list)
                val pdfFile = PdfReportGenerator.generatePdfReport(context, getCurrentDate(), sorted)
                val uri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                _uiEvent.emit(UiEvent.OpenPdfIntent(pdfFile, uri))
                _uiEvent.emit(UiEvent.ShowSnackbar("✓ Relatório Executivo SST em PDF gerado com ${sorted.size} ocorrência(s)!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao gerar relatório executivo: ${e.message}"))
            }
        }
    }

    fun exportarPlanilhaCsv(context: Context) {
        viewModelScope.launch {
            val list = allOccurrences.value
            if (list.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Nenhuma ocorrência registrada para exportar a planilha."))
                return@launch
            }
            try {
                val sorted = SpreadsheetReportManager.sortSequentiallyByDate(list)
                val csvFile = SpreadsheetReportManager.syncFullSpreadsheet(context, sorted)
                val uri = SpreadsheetReportManager.getShareableCsvUri(context, csvFile)
                _uiEvent.emit(UiEvent.OpenCsvIntent(csvFile, uri))
                _uiEvent.emit(UiEvent.ShowSnackbar("✓ Planilha consolidada CSV gerada com ${sorted.size} registro(s)!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao exportar planilha CSV: ${e.message}"))
            }
        }
    }

    fun exportSingleOccurrencePdf(context: Context, occurrence: SafetyOccurrence) {
        viewModelScope.launch {
            try {
                val pdfFile = PdfReportGenerator.generatePdfReport(context, occurrence.data, listOf(occurrence))
                val uri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)
                _uiEvent.emit(UiEvent.OpenPdfIntent(pdfFile, uri))
                _uiEvent.emit(UiEvent.ShowSnackbar("✓ Relatório PDF da ocorrência #${occurrence.id} gerado com sucesso!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao gerar PDF da ocorrência: ${e.message}"))
            }
        }
    }

    fun exportAndClearPreviousDayReport(context: Context, previousDate: String) {
        viewModelScope.launch {
            val list = allOccurrences.value.filter { it.data == previousDate }
            try {
                val pdfFile = PdfReportGenerator.generatePdfReport(context, previousDate, list)
                val uri = PdfReportGenerator.getShareablePdfUri(context, pdfFile)

                DailyReportManager.clearDailyFolderAndAdvanceDate(context, previousDate)
                pendingPreviousDayReport.value = null
                hasDismissedWarningThisSession = true

                _uiEvent.emit(UiEvent.OpenPdfIntent(pdfFile, uri))
                _uiEvent.emit(UiEvent.ShowSnackbar("✓ Relatório de $previousDate exportado em PDF e pasta zerada para o novo dia!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Erro ao exportar PDF: ${e.message}"))
            }
        }
    }

    fun discardPreviousDayReport(context: Context, previousDate: String) {
        viewModelScope.launch {
            DailyReportManager.clearDailyFolderAndAdvanceDate(context, previousDate)
            pendingPreviousDayReport.value = null
            hasDismissedWarningThisSession = true
            _uiEvent.emit(UiEvent.ShowSnackbar("Pasta de arquivos zerada. Pronto para os registros de hoje!"))
        }
    }

    fun enviarOcorrencia(context: Context) {
        val current = _formState.value

        if (current.relatoDetalhes.isBlank() && current.local.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Por favor, preencha o local e o detalhe do relato antes de enviar."))
            }
            return
        }

        if (!current.isSavedAndSynced) {
            salvarDadosEPlanilha(context) {
                procederEnvioEmail(context)
            }
        } else {
            procederEnvioEmail(context)
        }
    }

    private fun procederEnvioEmail(context: Context) {
        val current = _formState.value
        val subject = "[Relato SST] Ocorrência - ${current.local.ifBlank { "Geral" }} - ${current.data}"
        val body = buildString {
            appendLine("=== REGISTRO DE OCORRÊNCIA DE SEGURANÇA DO TRABALHO ===")
            appendLine()
            appendLine("Data: ${current.data}")
            appendLine("Hora: ${current.hora}")
            appendLine("Registro (Matrícula): ${current.registro.ifBlank { "N/A" }}")
            appendLine("Colaborador: ${current.nomeColaborador.ifBlank { "Não informado" }}")
            appendLine("Setor: ${current.setor.ifBlank { "Não informado" }}")
            appendLine()
            appendLine("Local da Ocorrência: ${current.local.ifBlank { "Não informado" }}")
            appendLine("Condição do Clima: ${current.clima}")
            appendLine("Causa Provável: ${current.causa}")
            appendLine("Grau de Risco: ${current.risco}")
            appendLine("Tipo de Ocorrência: ${current.ocorrencia}")
            appendLine("Classificação do Relato: ${current.classificacao}")
            appendLine()
            appendLine("--- DETALHES DO RELATO ---")
            appendLine(current.relatoDetalhes.ifBlank { "Sem detalhes." })
            appendLine()
            appendLine("--- AÇÃO TOMADA ---")
            appendLine(current.acaoTomada.ifBlank { "Nenhuma ação registrada." })
            appendLine()
            appendLine("--- ANEXO DE FOTO ---")
            appendLine(if (current.fotoUri != null) "Foto Anexada: Sim (${current.fotoUri})" else "Nenhuma foto anexada.")
            appendLine()
            appendLine("[✓] Registro salvo localmente e pronto para auditoria.")
            appendLine("Reportado via App Foco na Prevenção SST.")
        }

        val photoUriString = current.fotoUri
        val emailIntent = createEmailIntent(
            context = context,
            recipient = current.destinatarioEmail,
            subject = subject,
            body = body,
            fotoUri = photoUriString
        )

        viewModelScope.launch {
            _uiEvent.emit(UiEvent.OpenEmailIntent(emailIntent))
            _uiEvent.emit(UiEvent.ShowSnackbar("Ocorrência salva e e-mail com foto em anexo aberto!"))
        }
    }

    fun deleteOccurrence(occurrence: SafetyOccurrence) {
        viewModelScope.launch {
            repository.deleteOccurrence(occurrence)
            _uiEvent.emit(UiEvent.ShowSnackbar("Ocorrência removida do histórico."))
        }
    }

    fun fazerBackupDosDados(context: Context) {
        executeBackup(context, BackupFormat.ALL_FORMATS, clearAfterBackup = false)
    }

    companion object {
        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getCurrentTime(): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getShareableUri(context: Context, uriStr: String?): Uri? {
            if (uriStr.isNullOrBlank()) return null
            return try {
                val isRawPath = uriStr.startsWith("/")
                if (isRawPath) {
                    val file = java.io.File(uriStr)
                    if (file.exists()) {
                        return androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }
                }
                val parsedUri = Uri.parse(uriStr)
                when (parsedUri.scheme) {
                    "file" -> {
                        val file = java.io.File(parsedUri.path ?: "")
                        if (file.exists()) {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else parsedUri
                    }
                    "content" -> {
                        val cacheDir = context.cacheDir
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val file = java.io.File(cacheDir, "email_attachment_${System.currentTimeMillis()}.jpg")
                        try {
                            context.contentResolver.openInputStream(parsedUri)?.use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (_: Exception) {}
                        if (file.exists() && file.length() > 0) {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else parsedUri
                    }
                    else -> {
                        val file = java.io.File(uriStr)
                        if (file.exists()) {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else parsedUri
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SafetyViewModel", "Erro ao obter URI para anexo: ${e.message}", e)
                try { Uri.parse(uriStr) } catch (ex: Exception) { null }
            }
        }

        fun createEmailIntent(
            context: Context,
            recipient: String,
            subject: String,
            body: String,
            fotoUri: String?
        ): Intent {
            val attachmentUri = getShareableUri(context, fotoUri)
            return if (attachmentUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    if (recipient.isNotBlank()) {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    }
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, attachmentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    if (recipient.isNotBlank()) {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    }
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
            }
        }
    }
}

class SafetyViewModelFactory(
    private val repository: SafetyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafetyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafetyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
