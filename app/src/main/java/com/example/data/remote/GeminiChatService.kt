package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.data.model.SafetyOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiChatService {

    companion object {
        private const val TAG = "GeminiChatService"
        private const val PRIMARY_MODEL = "gemini-3.8-flash"
        private const val FALLBACK_MODEL = "gemini-3.5-flash"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        history: List<ChatMessage>,
        occurrencesContext: List<SafetyOccurrence>? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Chave da API Gemini não configurada. Configure o segredo GEMINI_API_KEY no painel de segredos.")
            )
        }

        // Try primary model first, fallback to fallback model if needed
        val primaryResult = executeGenerateContent(PRIMARY_MODEL, apiKey, history, occurrencesContext)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Falha no modelo principal $PRIMARY_MODEL: ${primaryResult.exceptionOrNull()?.message}. Tentando $FALLBACK_MODEL...")
        executeGenerateContent(FALLBACK_MODEL, apiKey, history, occurrencesContext)
    }

    private fun executeGenerateContent(
        model: String,
        apiKey: String,
        history: List<ChatMessage>,
        occurrencesContext: List<SafetyOccurrence>?
    ): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Map message history (filter out pending or errored)
            for (msg in history.filter { !it.isPending && !it.isError }) {
                val role = if (msg.role == MessageRole.USER) "user" else "model"
                val partObj = JSONObject().put("text", msg.text)
                val contentObj = JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            if (contentsArray.length() == 0) {
                return Result.failure(Exception("Nenhuma mensagem válida para envio."))
            }

            // Prepare system instruction with SST context
            val systemText = buildSystemInstruction(occurrencesContext)
            val systemInstructionObj = JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", systemText)))

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstructionObj)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                    put("topP", 0.95)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Erro HTTP ${response.code}: $responseBody")
                return Result.failure(Exception("Falha na chamada Gemini (${response.code}): $responseBody"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Resposta vazia da IA Gemini."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao chamar Gemini: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun buildSystemInstruction(occurrences: List<SafetyOccurrence>?): String {
        val base = """
            Você é o Assistente Virtual Inteligente de Saúde e Segurança do Trabalho (SST) do aplicativo "Foco na Prevenção".
            Sua missão é ajudar engenheiros, técnicos de segurança, gestores e trabalhadores na prevenção de acidentes e conformidade legal.

            Diretrizes e Conhecimentos Essenciais:
            1. Normas Regulamentadoras Brasileiras (especialmente NR-01 GRO/PGR, NR-06 EPI, NR-10 Eletricidade, NR-12 Máquinas, NR-18 Construção, NR-33 Espaços Confinados e NR-35 Trabalho em Altura).
            2. Metodologia CAPA (Ações Corretivas e Preventivas) e 5W2H (O que, Quem, Quando, Onde, Por que, Como, Quanto).
            3. Investigação de Causas Raiz: Método dos 5 Porquês, Diagrama de Ishikawa (6M - Método, Máquina, Material, Mão de obra, Meio ambiente, Medição).
            4. Hierarquia de Controle de Riscos: 1º Eliminação, 2º Substituição, 3º Engenharia/Proteção Coletiva, 4º Medidas Administrativas/Treinamento, 5º EPI.
            5. Seja sempre claro, profissional, encorajador, estruturado com tópicos e focado na preservação de vidas humanas.
        """.trimIndent()

        if (occurrences.isNullOrEmpty()) {
            return base
        }

        val total = occurrences.size
        val critical = occurrences.count { it.risco.contains("Crítico", true) || it.risco.contains("Alto", true) }
        val pendentes = occurrences.count { it.statusAcao.equals("Pendente", true) || it.statusAcao.equals("Atrasado", true) }

        val contextInfo = """

            CONTEXTO OPERACIONAL ATUAL DO APLICATIVO:
            - Total de ocorrências cadastradas: $total
            - Ocorrências de Risco Alto ou Crítico: $critical
            - Ações CAPA pendentes ou em atraso: $pendentes
            Você pode citar esses dados caso o usuário pergunte sobre a situação da empresa ou peça análises de risco.
        """.trimIndent()

        return base + contextInfo
    }
}
