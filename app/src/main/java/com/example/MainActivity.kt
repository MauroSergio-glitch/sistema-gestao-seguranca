package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.database.AppDatabase
import com.example.data.repository.SafetyRepository
import com.example.ui.screens.MainFormScreen
import com.example.ui.theme.SafetyTheme
import com.example.ui.viewmodel.SafetyViewModel
import com.example.ui.viewmodel.SafetyViewModelFactory
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this, applicationScope)
        val repository = SafetyRepository(database.occurrenceDao(), database.employeeDao())
        val factory = SafetyViewModelFactory(repository)
        val viewModel: SafetyViewModel by viewModels { factory }

        setContent {
            SafetyTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                            is UiEvent.OpenEmailIntent -> {
                                try {
                                    val chooser = android.content.Intent.createChooser(
                                        event.intent,
                                        "Enviar Ocorrência SST por E-mail"
                                    )
                                    startActivity(chooser)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Nenhum aplicativo de e-mail encontrado para envio.")
                                }
                            }
                            is UiEvent.OpenPdfIntent -> {
                                try {
                                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(event.uri, "application/pdf")
                                        clipData = android.content.ClipData.newRawUri("Relatório PDF SST", event.uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        clipData = android.content.ClipData.newRawUri("Relatório PDF SST", event.uri)
                                        putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = android.content.Intent.createChooser(viewIntent, "Abrir ou Exportar Relatório PDF SST").apply {
                                        putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, arrayOf(sendIntent))
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(chooser)
                                } catch (e: Exception) {
                                    try {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            clipData = android.content.ClipData.newRawUri("Relatório PDF SST", event.uri)
                                            putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        val chooser = android.content.Intent.createChooser(sendIntent, "Exportar Relatório PDF SST").apply {
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(chooser)
                                    } catch (ex: Exception) {
                                        snackbarHostState.showSnackbar("Relatório PDF gerado em: ${event.pdfFile.name}")
                                    }
                                }
                            }
                            is UiEvent.OpenCsvIntent -> {
                                try {
                                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(event.uri, "text/csv")
                                        clipData = android.content.ClipData.newRawUri("Planilha SST CSV", event.uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        clipData = android.content.ClipData.newRawUri("Planilha SST CSV", event.uri)
                                        putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = android.content.Intent.createChooser(viewIntent, "Abrir Planilha SST (Excel / Google Planilhas)").apply {
                                        putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, arrayOf(sendIntent))
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(chooser)
                                } catch (e: Exception) {
                                    try {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            clipData = android.content.ClipData.newRawUri("Planilha SST CSV", event.uri)
                                            putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        val chooser = android.content.Intent.createChooser(sendIntent, "Exportar Planilha SST CSV").apply {
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(chooser)
                                    } catch (ex: Exception) {
                                        snackbarHostState.showSnackbar("Planilha CSV gerada em: ${event.csvFile.name}")
                                    }
                                }
                            }
                            is UiEvent.OpenBackupFileIntent -> {
                                try {
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = event.mimeType
                                        putExtra(android.content.Intent.EXTRA_STREAM, event.uri)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[Backup SST] ${event.file.name}")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val chooser = android.content.Intent.createChooser(sendIntent, event.title).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(chooser)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Backup salvo com sucesso no aparelho em: ${event.file.name}")
                                }
                            }
                            is UiEvent.OpenMultipleFilesIntent -> {
                                try {
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, java.util.ArrayList(event.uris))
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[Backup Consolidado SST] ${SafetyViewModel.getCurrentDate()}")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val chooser = android.content.Intent.createChooser(sendIntent, event.title).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(chooser)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Pacote de backup salvo com sucesso no aparelho (${event.files.size} arquivos).")
                                }
                            }
                            is UiEvent.CloseApp -> {
                                finish()
                            }
                        }
                    }
                }

                MainFormScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }

    private val applicationScope by lazy {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    }
}

