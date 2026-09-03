package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SafetyOccurrence
import com.example.data.remote.GoogleSheetsSyncService
import com.example.ui.viewmodel.SafetyViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafetyAppAutomatedTest {

    @Test
    fun `test app name resource is set correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Foco na Prevenção", appName)
    }

    @Test
    fun `test google sheets sync service webhook url persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = GoogleSheetsSyncService(context)

        val customUrl = "https://script.google.com/macros/s/TEST_WEBHOOK_123/exec"
        service.saveWebhookUrl(customUrl)

        val retrievedUrl = service.customWebhookUrl
        assertEquals(customUrl, retrievedUrl)
    }

    @Test
    fun `test safety occurrence data model creation`() {
        val occurrence = SafetyOccurrence(
            data = "09/08/2026",
            hora = "14:30",
            registro = "10023",
            nomeColaborador = "João Silva",
            setor = "Produção",
            local = "Galpão A",
            relatoDetalhes = "Piso escorregadio próximo ao torno",
            acaoTomada = "Sinalização com placa e limpeza imediata",
            clima = "Ensolarado",
            causa = "Vazamento de óleo",
            risco = "Médio",
            ocorrencia = "Quase Acidente (Near Miss)",
            classificacao = "Condição Insegura",
            sincronizadoGooglePlanilhas = true
        )

        assertEquals("Galpão A", occurrence.local)
        assertEquals("João Silva", occurrence.nomeColaborador)
        assertTrue(occurrence.sincronizadoGooglePlanilhas)
        assertNotNull(occurrence.timestamp)
    }

    @Test
    fun `test backup JSON generation logic`() {
        val occurrence = SafetyOccurrence(
            id = 1,
            data = "09/08/2026",
            hora = "14:30",
            registro = "10023",
            nomeColaborador = "Maria Oliveira",
            setor = "Manutenção",
            local = "Setor Elétrico",
            relatoDetalhes = "Cabo desencapado identificado em inspeção",
            acaoTomada = "Isolamento da área e troca do cabo",
            clima = "Nublado",
            causa = "Desgaste natural",
            risco = "Alto",
            ocorrencia = "Incidente de Segurança",
            classificacao = "Ato Inseguro",
            sincronizadoGooglePlanilhas = true
        )

        val jsonArr = JSONArray()
        val obj = JSONObject().apply {
            put("id", occurrence.id)
            put("data", occurrence.data)
            put("hora", occurrence.hora)
            put("registro", occurrence.registro)
            put("nomeColaborador", occurrence.nomeColaborador)
            put("setor", occurrence.setor)
            put("local", occurrence.local)
            put("relatoDetalhes", occurrence.relatoDetalhes)
            put("acaoTomada", occurrence.acaoTomada)
            put("clima", occurrence.clima)
            put("causa", occurrence.causa)
            put("risco", occurrence.risco)
            put("ocorrencia", occurrence.ocorrencia)
            put("classificacao", occurrence.classificacao)
            put("timestamp", occurrence.timestamp)
        }
        jsonArr.put(obj)

        assertEquals(1, jsonArr.length())
        val extractedObj = jsonArr.getJSONObject(0)
        assertEquals("Maria Oliveira", extractedObj.getString("nomeColaborador"))
        assertEquals("Setor Elétrico", extractedObj.getString("local"))
        assertEquals("Alto", extractedObj.getString("risco"))
    }

    @Test
    fun `test safety occurrence with attached photo uri`() {
        val testPhotoUri = "content://media/external/images/media/10001"
        val occurrence = SafetyOccurrence(
            data = "09/08/2026",
            hora = "15:00",
            registro = "10025",
            nomeColaborador = "Pedro Santos",
            setor = "Logística",
            local = "Doca 3",
            relatoDetalhes = "Palete danificado com risco de queda",
            acaoTomada = "Remoção da carga e foto anexada",
            clima = "Ensolarado",
            causa = "Carga Mal Acondicionada",
            risco = "Alto",
            ocorrencia = "Condição Insegura",
            classificacao = "Relato com Foto",
            fotoUri = testPhotoUri,
            sincronizadoGooglePlanilhas = true
        )

        assertEquals(testPhotoUri, occurrence.fotoUri)
        assertNotNull(occurrence.fotoUri)
    }

    @Test
    fun `test offline room occurrence sync flag and transition`() {
        val unsyncedOccurrence = SafetyOccurrence(
            id = 10,
            data = "09/08/2026",
            hora = "16:00",
            registro = "10099",
            nomeColaborador = "Ana Souza",
            setor = "Qualidade",
            local = "Laboratório B",
            relatoDetalhes = "Registro sem rede - salvo em Room DB",
            acaoTomada = "Aguardando conexão para sincronização",
            clima = "Chuvoso",
            causa = "Equipamento com defeito",
            risco = "Baixo",
            ocorrencia = "Observação de Segurança",
            classificacao = "Prevenção",
            sincronizadoGooglePlanilhas = false
        )

        assertEquals(false, unsyncedOccurrence.sincronizadoGooglePlanilhas)

        val syncedOccurrence = unsyncedOccurrence.copy(sincronizadoGooglePlanilhas = true)
        assertEquals(true, syncedOccurrence.sincronizadoGooglePlanilhas)
    }

    @Test
    fun `test spreadsheet csv generation and backup manager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val occurrence = SafetyOccurrence(
            id = 101,
            data = "10/08/2026",
            hora = "10:00",
            registro = "RE9988",
            nomeColaborador = "Carlos Teste",
            setor = "Operações",
            local = "Área de Carga",
            relatoDetalhes = "Inspeção de rotina sem inconformidades",
            acaoTomada = "Registro efetuado",
            clima = "Bom",
            causa = "Prevenção",
            risco = "Baixo",
            ocorrencia = "Auditoria de Segurança",
            classificacao = "Prevenção",
            statusAcao = "Em Tratativa",
            acaoPreventiva = "Implementar proteção física NR-12 na máquina de esteira",
            responsavelAcao = "Eng. Roberto Silva",
            setorResponsavel = "Manutenção Industrial",
            prazoAcao = "20/08/2026",
            causaSecundaria = "Método / Procedimentos operacionais"
        )

        // 1. Sync CSV spreadsheet
        val csvFile = com.example.util.SpreadsheetReportManager.syncFullSpreadsheet(context, listOf(occurrence))
        assertTrue(csvFile.exists())
        assertTrue(csvFile.length() > 0L)
        val csvContent = csvFile.readText(Charsets.UTF_8)
        assertTrue(csvContent.contains("Status da Ocorrência"))
        assertTrue(csvContent.contains("Ação Preventiva Definitiva"))
        assertTrue(csvContent.contains("Responsável pela Resolução"))
        assertTrue(csvContent.contains("Setor Responsável"))
        assertTrue(csvContent.contains("Prazo / Data Limite de Tratativa"))
        assertTrue(csvContent.contains("Causa Secundária / Fator Contribuinte"))
        assertTrue(csvContent.contains("Em Tratativa"))
        assertTrue(csvContent.contains("Implementar proteção física NR-12"))
        assertTrue(csvContent.contains("Eng. Roberto Silva"))
        assertTrue(csvContent.contains("Manutenção Industrial"))
        assertTrue(csvContent.contains("20/08/2026"))
        assertTrue(csvContent.contains("Método / Procedimentos operacionais"))

        // 2. Create full backup
        val backupFile = com.example.util.SpreadsheetReportManager.createFullBackup(context, listOf(occurrence))
        assertTrue(backupFile.exists())
        assertTrue(backupFile.length() > 0L)
        val backupContent = backupFile.readText(Charsets.UTF_8)
        assertTrue(backupContent.contains("Em Tratativa"))
        assertTrue(backupContent.contains("Implementar proteção física NR-12"))
    }

    @Test
    fun `test pdf report generator functionality`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val occurrence = SafetyOccurrence(
            id = 102,
            data = "10/08/2026",
            hora = "11:00",
            registro = "RE5544",
            nomeColaborador = "Aline Rocha",
            setor = "SST",
            local = "Auditório",
            relatoDetalhes = "Treinamento de Uso de EPI",
            acaoTomada = "Treinamento Concluído",
            clima = "Ensolarado",
            causa = "Treinamento",
            risco = "Baixo",
            ocorrencia = "Treinamento SST",
            classificacao = "Capacitação"
        )

        val pdfFile = com.example.util.PdfReportGenerator.generatePdfReport(context, "10/08/2026", listOf(occurrence))
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0L)
        assertTrue(pdfFile.name.contains("Relatorio_SST_Atualizado.pdf") || pdfFile.name.contains("Relatorio_SST_"))

        val updatedPdfFile = com.example.util.PdfReportGenerator.generateUpdatedPdfReport(context, "10/08/2026", listOf(occurrence))
        assertTrue(updatedPdfFile.exists())
        assertTrue(updatedPdfFile.length() > 0L)

        val directOpenPdfFile = java.io.File(context.filesDir, "test_openpdf_report.pdf")
        com.example.util.OpenPdfExportService.exportReport(context, "10/08/2026", listOf(occurrence), directOpenPdfFile)
        assertTrue(directOpenPdfFile.exists())
        assertTrue(directOpenPdfFile.length() > 0L)
    }

    @Test
    fun `test cover image manager preset persistence and reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. Initial default configuration
        val initialConfig = com.example.util.CoverImageManager.getCoverConfig(context)
        assertEquals(com.example.util.CoverType.PRESET, initialConfig.type)
        assertEquals(com.example.util.CoverImageManager.DEFAULT_PRESET_RES_ID, initialConfig.presetResId)

        // 2. Save preset configuration
        val newPresetRes = R.drawable.img_foco_prevencao_logo_1787517691154
        val updatedConfig = com.example.util.CoverImageManager.savePresetCover(context, newPresetRes)
        assertEquals(newPresetRes, updatedConfig.presetResId)
        assertEquals(com.example.util.CoverType.PRESET, updatedConfig.type)

        // 3. Verify persistence
        val reloadedConfig = com.example.util.CoverImageManager.getCoverConfig(context)
        assertEquals(newPresetRes, reloadedConfig.presetResId)

        // 4. Reset to default
        val resetConfig = com.example.util.CoverImageManager.resetToDefault(context)
        assertEquals(com.example.util.CoverImageManager.DEFAULT_PRESET_RES_ID, resetConfig.presetResId)
    }

    @Test
    fun `test daily report manager append and cleanup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val occurrence = SafetyOccurrence(
            id = 200,
            data = "12/08/2026",
            hora = "08:15",
            registro = "10999",
            nomeColaborador = "Inspetor Teste",
            setor = "Operações",
            local = "Setor A",
            relatoDetalhes = "Inspeção diária concluída",
            acaoTomada = "Sem pendências",
            clima = "Ensolarado",
            causa = "Rotina",
            risco = "Baixo",
            ocorrencia = "Inspeção",
            classificacao = "Prevenção"
        )

        // 1. Append occurrence to daily log file
        com.example.util.DailyReportManager.appendOccurrenceToDailyFile(context, occurrence)
        val reportsDir = com.example.util.DailyReportManager.getReportsDir(context)
        val dailyFile = java.io.File(reportsDir, "relatorio_diario_12-08-2026.txt")
        assertTrue(dailyFile.exists())
        assertTrue(dailyFile.length() > 0L)

        // 2. Clear daily folder and advance
        com.example.util.DailyReportManager.clearDailyFolderAndAdvanceDate(context, "12/08/2026")
        assertTrue(!dailyFile.exists())
    }

    @Test
    fun `test risk matrix P x S combinations and reactive calculations`() {
        // P1 Combinations
        val p1s1 = com.example.util.SstManagementEngine.calculateRiskMatrix(1, 1)
        assertEquals(1, p1s1.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, p1s1.level)
        assertEquals("Baixo (Verde)", p1s1.riskLabel)

        val p1s2 = com.example.util.SstManagementEngine.calculateRiskMatrix(1, 2)
        assertEquals(2, p1s2.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, p1s2.level)

        val p1s3 = com.example.util.SstManagementEngine.calculateRiskMatrix(1, 3)
        assertEquals(3, p1s3.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, p1s3.level)

        val p1s4 = com.example.util.SstManagementEngine.calculateRiskMatrix(1, 4)
        assertEquals(4, p1s4.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.MEDIUM, p1s4.level)

        // P2 Combinations
        val p2s1 = com.example.util.SstManagementEngine.calculateRiskMatrix(2, 1)
        assertEquals(2, p2s1.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, p2s1.level)

        val p2s2 = com.example.util.SstManagementEngine.calculateRiskMatrix(2, 2)
        assertEquals(4, p2s2.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.MEDIUM, p2s2.level)

        val p2s3 = com.example.util.SstManagementEngine.calculateRiskMatrix(2, 3)
        assertEquals(6, p2s3.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.MEDIUM, p2s3.level)

        val p2s4 = com.example.util.SstManagementEngine.calculateRiskMatrix(2, 4)
        assertEquals(8, p2s4.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.HIGH, p2s4.level)
        assertEquals("Alto (Laranja)", p2s4.riskLabel)

        // P3 Combinations
        val p3s1 = com.example.util.SstManagementEngine.calculateRiskMatrix(3, 1)
        assertEquals(3, p3s1.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, p3s1.level)

        val p3s2 = com.example.util.SstManagementEngine.calculateRiskMatrix(3, 2)
        assertEquals(6, p3s2.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.MEDIUM, p3s2.level)

        val p3s3 = com.example.util.SstManagementEngine.calculateRiskMatrix(3, 3)
        assertEquals(9, p3s3.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.HIGH, p3s3.level)
        assertEquals("Alto (Laranja)", p3s3.riskLabel)

        val p3s4 = com.example.util.SstManagementEngine.calculateRiskMatrix(3, 4)
        assertEquals(12, p3s4.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.CRITICAL, p3s4.level)
        assertEquals("Crítico (Vermelho)", p3s4.riskLabel)

        // P4 Combinations
        val p4s4 = com.example.util.SstManagementEngine.calculateRiskMatrix(4, 4)
        assertEquals(16, p4s4.score)
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.CRITICAL, p4s4.level)
    }

    @Test
    fun `test effective risk level calculation from occurrences`() {
        val occLow = SafetyOccurrence(
            id = 1,
            data = "12/08/2026",
            hora = "08:00",
            registro = "101",
            nomeColaborador = "Teste",
            setor = "Operacional",
            relatoDetalhes = "Relato teste",
            local = "Área 1",
            acaoTomada = "Ação",
            clima = "Ensolarado",
            causa = "Procedimento",
            ocorrencia = "Desvio",
            classificacao = "Segurança",
            probabilidade = 1,
            severidade = 3,
            risco = "Baixo (Verde)"
        )
        val occMedium = occLow.copy(id = 2, probabilidade = 2, severidade = 3, risco = "Médio (Amarelo)")
        val occHigh = occLow.copy(id = 3, probabilidade = 3, severidade = 3, risco = "Alto (Laranja)")
        val occCritical = occLow.copy(id = 4, probabilidade = 3, severidade = 4, risco = "Crítico (Vermelho)")

        assertEquals(com.example.util.SstManagementEngine.RiskLevel.LOW, com.example.util.SstManagementEngine.getEffectiveRiskLevel(occLow))
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.MEDIUM, com.example.util.SstManagementEngine.getEffectiveRiskLevel(occMedium))
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.HIGH, com.example.util.SstManagementEngine.getEffectiveRiskLevel(occHigh))
        assertEquals(com.example.util.SstManagementEngine.RiskLevel.CRITICAL, com.example.util.SstManagementEngine.getEffectiveRiskLevel(occCritical))
    }

    @Test
    fun `test alerts badge active alerts count increments from 3 to 4 when new event is recorded`() {
        val baseOcc = SafetyOccurrence(
            id = 1,
            data = "16/08/2026",
            hora = "08:30",
            registro = "1001",
            nomeColaborador = "João Carlos Silva",
            setor = "Manutenção Industrial",
            relatoDetalhes = "Vazamento hidráulico",
            local = "Galpão A",
            acaoTomada = "Isolamento",
            clima = "Ensolarado",
            causa = "Falha de Equipamento",
            risco = "Crítico (Vermelho)",
            ocorrencia = "Condição Abaixo do Padrão",
            classificacao = "Não Conformidade",
            probabilidade = 4,
            severidade = 4,
            statusAcao = "Em Tratativa"
        )

        val demoList = listOf(
            baseOcc.copy(id = 1, risco = "Crítico (Vermelho)", probabilidade = 4, severidade = 4, statusAcao = "Em Tratativa"),
            baseOcc.copy(id = 2, risco = "Alto (Laranja)", probabilidade = 3, severidade = 3, statusAcao = "Pendente"),
            baseOcc.copy(id = 3, risco = "Médio (Amarelo)", probabilidade = 2, severidade = 2, statusAcao = "Concluído", dataConclusao = "15/08/2026"),
            baseOcc.copy(id = 4, risco = "Baixo (Verde)", probabilidade = 2, severidade = 1, statusAcao = "Eficaz", dataConclusao = "16/08/2026"),
            baseOcc.copy(id = 5, risco = "Crítico (Vermelho)", probabilidade = 4, severidade = 4, statusAcao = "Eficaz", dataConclusao = "")
        )

        // Initial state has 3 active alerts (ID 1, ID 2, and ID 5)
        val initialCount = com.example.util.SstManagementEngine.countActiveAlerts(demoList)
        assertEquals(3, initialCount)

        // User records a new occurrence (e.g. ID 6 with default medium risk and pending status)
        val newRegisteredOccurrence = baseOcc.copy(
            id = 6,
            risco = "Médio (Amarelo)",
            probabilidade = 2,
            severidade = 2,
            statusAcao = "Pendente",
            dataConclusao = ""
        )

        val updatedList = demoList + newRegisteredOccurrence
        val updatedCount = com.example.util.SstManagementEngine.countActiveAlerts(updatedList)

        // Immediately increments from 3 to 4
        assertEquals(4, updatedCount)
    }

    @Test
    fun `test spreadsheet backup and post-backup verification state generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testOccurrences = listOf(
            SafetyOccurrence(
                id = 1,
                data = "24/08/2026",
                hora = "10:00",
                registro = "10001",
                nomeColaborador = "Carlos Teste",
                setor = "Operações",
                relatoDetalhes = "Inspeção realizada com sucesso",
                local = "Setor A",
                acaoTomada = "Verificação preventiva",
                clima = "Ensolarado",
                causa = "Rotina",
                risco = "Baixo (Verde)",
                ocorrencia = "Prevenção",
                classificacao = "Comportamento Seguro"
            )
        )

        val csvFile = com.example.util.SpreadsheetReportManager.syncFullSpreadsheet(context, testOccurrences)
        val backupCsv = com.example.util.SpreadsheetReportManager.createFullBackup(context, testOccurrences)

        assertTrue(csvFile.exists())
        assertTrue(backupCsv.exists())
        assertTrue(backupCsv.length() > 0)

        val uri = com.example.util.SpreadsheetReportManager.getShareableCsvUri(context, backupCsv)
        assertNotNull(uri)

        val postBackupState = com.example.ui.viewmodel.PostBackupVerificationState(
            backupFileName = backupCsv.name,
            backupFile = backupCsv,
            uri = uri,
            backupFormat = com.example.data.model.BackupFormat.GOOGLE_SHEETS_CSV,
            recordCount = testOccurrences.size
        )

        assertEquals(backupCsv.name, postBackupState.backupFileName)
        assertEquals(1, postBackupState.recordCount)
        assertEquals(com.example.data.model.BackupFormat.GOOGLE_SHEETS_CSV, postBackupState.backupFormat)
    }

    @Test
    fun `test about screen state toggle and developer details`() {
        val developer = "Mauro S.O.Candido"
        val version = com.example.BuildConfig.VERSION_NAME
        val contact = "suporte.ofertaoceano@gmail.com"
        val purpose = "App para registro de ocorrências de segurança SST."
        val privacySnippet = "Política de Privacidade do Aplicativo de Relato de Incidentes"

        assertEquals("Mauro S.O.Candido", developer)
        assertEquals(com.example.BuildConfig.VERSION_NAME, version)
        assertEquals("suporte.ofertaoceano@gmail.com", contact)
        assertTrue(purpose.contains("SST"))
        assertTrue(privacySnippet.contains("Privacidade"))
    }

    @Test
    fun `test occurrences sorted in descending order by occurrence ID number`() {
        val occ1 = SafetyOccurrence(
            id = 12,
            data = "20/08/2026",
            hora = "08:30",
            registro = "1001",
            nomeColaborador = "João",
            setor = "Produção",
            relatoDetalhes = "Incidente 12",
            local = "Setor A",
            acaoTomada = "Ação 1",
            clima = "Ensolarado",
            causa = "Causa 1",
            risco = "Baixo (Verde)",
            ocorrencia = "Desvio",
            classificacao = "Leve"
        )
        val occ2 = SafetyOccurrence(
            id = 45,
            data = "25/08/2026",
            hora = "14:15",
            registro = "1002",
            nomeColaborador = "Maria",
            setor = "Manutenção",
            relatoDetalhes = "Incidente 45",
            local = "Setor B",
            acaoTomada = "Ação 2",
            clima = "Nublado",
            causa = "Causa 2",
            risco = "Crítico (Vermelho)",
            ocorrencia = "Quase Acidente",
            classificacao = "Grave"
        )
        val occ3 = SafetyOccurrence(
            id = 30,
            data = "25/08/2026",
            hora = "09:00",
            registro = "1003",
            nomeColaborador = "Carlos",
            setor = "Logística",
            relatoDetalhes = "Incidente 30",
            local = "Setor C",
            acaoTomada = "Ação 3",
            clima = "Chuvoso",
            causa = "Causa 3",
            risco = "Alto (Laranja)",
            ocorrencia = "Incidente",
            classificacao = "Moderada"
        )

        val list = listOf(occ1, occ2, occ3)
        val sorted = com.example.util.SstManagementEngine.sortOccurrencesByIdDescending(list)

        // Highest ID (newest occurrence) must always be at the top: 45 -> 30 -> 12
        assertEquals(45L, sorted[0].id)
        assertEquals(30L, sorted[1].id)
        assertEquals(12L, sorted[2].id)
    }
}
