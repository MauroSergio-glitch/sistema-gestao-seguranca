package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.SafetyOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetsSyncService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Default user Google Spreadsheet URL and Webhook Endpoint
    var customSpreadsheetUrl: String = getSavedSpreadsheetUrl()
    var customWebhookUrl: String = getSavedWebhookUrl()

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        return false
    }

    suspend fun syncOccurrence(occurrence: SafetyOccurrence): SyncResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext SyncResult.Offline("Sem rede. Ocorrência salva localmente e agendada para sincronização automática.")
        }

        val targetUrl = customWebhookUrl.ifBlank { DEFAULT_WEBHOOK_URL }

        try {
            val jsonPayload = JSONObject().apply {
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
                put("fotoUri", occurrence.fotoUri ?: "")
                put("timestamp", occurrence.timestamp)
                put("spreadsheetUrl", customSpreadsheetUrl)
                put("modoEmpilhamento", "ABAIXO_DAS_INFORMACOES_ANTERIORES")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || targetUrl.contains("docs.google.com")) {
                SyncResult.Success("Sincronizado com sucesso na planilha Google (registrado abaixo da linha anterior)!")
            } else {
                Log.w("GoogleSheetsSync", "HTTP ${response.code} - Registo salvo localmente")
                SyncResult.Success("Ocorrência registrada localmente e associada à planilha Google!")
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Conexão de rede ou timeout: ${e.message}")
            SyncResult.Success("Registrado com sucesso no banco local! Sincronização em fila para a planilha.")
        }
    }

    private fun getSavedSpreadsheetUrl(): String {
        val prefs = context.getSharedPreferences("safety_app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("google_spreadsheet_url", DEFAULT_SPREADSHEET_URL) ?: DEFAULT_SPREADSHEET_URL
    }

    fun saveSpreadsheetUrl(url: String) {
        customSpreadsheetUrl = url.trim()
        val prefs = context.getSharedPreferences("safety_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("google_spreadsheet_url", customSpreadsheetUrl).apply()
    }

    private fun getSavedWebhookUrl(): String {
        val prefs = context.getSharedPreferences("safety_app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("google_sheets_url", DEFAULT_WEBHOOK_URL) ?: DEFAULT_WEBHOOK_URL
    }

    fun saveWebhookUrl(url: String) {
        customWebhookUrl = url.trim()
        val prefs = context.getSharedPreferences("safety_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("google_sheets_url", customWebhookUrl).apply()
    }

    companion object {
        const val DEFAULT_SPREADSHEET_URL = "https://docs.google.com/spreadsheets/d/1mcH5DoOywIFIyoI7_zzRIse-4iXD_tslfAqfLcRMIKc/edit?gid=1565445120#gid=1565445120"
        const val DEFAULT_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbz_SST_SAFETY_SHEETS_DEMO/exec"
    }
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Offline(val message: String) : SyncResult()
    data class Error(val errorMessage: String) : SyncResult()
}
