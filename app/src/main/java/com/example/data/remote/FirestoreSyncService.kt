package com.example.data.remote

import android.util.Log
import com.example.data.model.SafetyOccurrence
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncService {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    companion object {
        private const val TAG = "FirestoreSyncService"
        const val COLLECTION_OCCURRENCES = "occurrences"
    }

    suspend fun saveOccurrenceToFirestore(occurrence: SafetyOccurrence): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = occurrence.id.toString()
            val data = occurrenceToMap(occurrence)

            firestore.collection(COLLECTION_OCCURRENCES)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Ocorrência $docId persistida com sucesso no Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar ocorrência no Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllToFirestore(occurrences: List<SafetyOccurrence>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var syncedCount = 0
            val batch = firestore.batch()

            for (occ in occurrences) {
                val docRef = firestore.collection(COLLECTION_OCCURRENCES).document(occ.id.toString())
                batch.set(docRef, occurrenceToMap(occ), SetOptions.merge())
                syncedCount++
            }

            if (syncedCount > 0) {
                batch.commit().await()
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar lote com Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchOccurrencesFromFirestore(): Result<List<SafetyOccurrence>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_OCCURRENCES)
                .get()
                .await()

            val list = mutableListOf<SafetyOccurrence>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                try {
                    val occ = mapToOccurrence(doc.id, data)
                    list.add(occ)
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao parsear doc ${doc.id}: ${e.message}")
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar ocorrências do Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteOccurrenceFromFirestore(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_OCCURRENCES)
                .document(id.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar ocorrência no Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun occurrenceToMap(occ: SafetyOccurrence): Map<String, Any?> {
        return hashMapOf(
            "id" to occ.id,
            "data" to occ.data,
            "hora" to occ.hora,
            "registro" to occ.registro,
            "nomeColaborador" to occ.nomeColaborador,
            "setor" to occ.setor,
            "relatoDetalhes" to occ.relatoDetalhes,
            "local" to occ.local,
            "acaoTomada" to occ.acaoTomada,
            "clima" to occ.clima,
            "causa" to occ.causa,
            "risco" to occ.risco,
            "ocorrencia" to occ.ocorrencia,
            "classificacao" to occ.classificacao,
            "sincronizadoGooglePlanilhas" to occ.sincronizadoGooglePlanilhas,
            "fotoUri" to (occ.fotoUri ?: ""),
            "timestamp" to occ.timestamp,
            "statusAcao" to occ.statusAcao,
            "responsavelAcao" to occ.responsavelAcao,
            "prazoAcao" to occ.prazoAcao,
            "perigo" to occ.perigo,
            "probabilidade" to occ.probabilidade,
            "severidade" to occ.severidade,
            "prioridade" to occ.prioridade,
            "acaoPreventiva" to occ.acaoPreventiva,
            "setorResponsavel" to occ.setorResponsavel,
            "dataAbertura" to occ.dataAbertura,
            "dataConclusao" to occ.dataConclusao,
            "responsavelValidacao" to occ.responsavelValidacao,
            "observacoesAcao" to occ.observacoesAcao,
            "fotoDepoisUri" to (occ.fotoDepoisUri ?: ""),
            "descricaoSolucao" to occ.descricaoSolucao,
            "avaliacaoEficacia" to occ.avaliacaoEficacia,
            "categoriaCausa" to occ.categoriaCausa,
            "causaSecundaria" to occ.causaSecundaria
        )
    }

    private fun mapToOccurrence(docId: String, data: Map<String, Any?>): SafetyOccurrence {
        val idLong = (data["id"] as? Number)?.toLong() ?: docId.toLongOrNull() ?: 0L
        return SafetyOccurrence(
            id = idLong,
            data = data["data"] as? String ?: "",
            hora = data["hora"] as? String ?: "",
            registro = data["registro"] as? String ?: "",
            nomeColaborador = data["nomeColaborador"] as? String ?: "",
            setor = data["setor"] as? String ?: "",
            relatoDetalhes = data["relatoDetalhes"] as? String ?: "",
            local = data["local"] as? String ?: "",
            acaoTomada = data["acaoTomada"] as? String ?: "",
            clima = data["clima"] as? String ?: "",
            causa = data["causa"] as? String ?: "",
            risco = data["risco"] as? String ?: "Médio (Amarelo)",
            ocorrencia = data["ocorrencia"] as? String ?: "Condição Abaixo do Padrão",
            classificacao = data["classificacao"] as? String ?: "Observação de Segurança",
            sincronizadoGooglePlanilhas = data["sincronizadoGooglePlanilhas"] as? Boolean ?: true,
            fotoUri = (data["fotoUri"] as? String)?.takeIf { it.isNotBlank() },
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            statusAcao = data["statusAcao"] as? String ?: "Pendente",
            responsavelAcao = data["responsavelAcao"] as? String ?: "",
            prazoAcao = data["prazoAcao"] as? String ?: "",
            perigo = data["perigo"] as? String ?: "",
            probabilidade = (data["probabilidade"] as? Number)?.toInt() ?: 2,
            severidade = (data["severidade"] as? Number)?.toInt() ?: 2,
            prioridade = data["prioridade"] as? String ?: "Prioridade normal",
            acaoPreventiva = data["acaoPreventiva"] as? String ?: "",
            setorResponsavel = data["setorResponsavel"] as? String ?: "",
            dataAbertura = data["dataAbertura"] as? String ?: "",
            dataConclusao = data["dataConclusao"] as? String ?: "",
            responsavelValidacao = data["responsavelValidacao"] as? String ?: "",
            observacoesAcao = data["observacoesAcao"] as? String ?: "",
            fotoDepoisUri = (data["fotoDepoisUri"] as? String)?.takeIf { it.isNotBlank() },
            descricaoSolucao = data["descricaoSolucao"] as? String ?: "",
            avaliacaoEficacia = data["avaliacaoEficacia"] as? String ?: "Pendente",
            categoriaCausa = data["categoriaCausa"] as? String ?: "Mão de Obra / Fator Humano",
            causaSecundaria = data["causaSecundaria"] as? String ?: ""
        )
    }
}
