package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "occurrences")
data class SafetyOccurrence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val data: String,
    val hora: String,
    val registro: String,
    val nomeColaborador: String,
    val setor: String,
    val relatoDetalhes: String,
    val local: String,
    val acaoTomada: String,
    val clima: String,
    val causa: String,
    val risco: String,
    val ocorrencia: String,
    val classificacao: String,
    val sincronizadoGooglePlanilhas: Boolean = true,
    val fotoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val statusAcao: String = "Pendente", // "Pendente", "Em Tratativa", "Aguardando Validação", "Concluído", "Atrasado", "Cancelado", "Eficaz"
    val responsavelAcao: String = "",
    val prazoAcao: String = "",
    // Integrated SST Management Enhancements (Preserving backward compatibility)
    val perigo: String = "",
    val probabilidade: Int = 2, // 1: Baixa, 2: Média, 3: Alta, 4: Muito Alta
    val severidade: Int = 2,    // 1: Leve, 2: Moderada, 3: Grave, 4: Crítica/Catastrófica
    val prioridade: String = "Prioridade normal", // "Programada", "Prioridade normal", "Prioridade alta", "Tratativa imediata"
    val acaoPreventiva: String = "",
    val setorResponsavel: String = "",
    val dataAbertura: String = "",
    val dataConclusao: String = "",
    val responsavelValidacao: String = "",
    val observacoesAcao: String = "",
    val fotoDepoisUri: String? = null,
    val descricaoSolucao: String = "",
    val avaliacaoEficacia: String = "Pendente", // "Pendente", "Eficaz", "Ineficaz"
    val categoriaCausa: String = "Mão de Obra / Fator Humano", // 6M standard
    val causaSecundaria: String = ""
)
