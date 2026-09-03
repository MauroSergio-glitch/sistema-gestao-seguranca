package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.EmployeeDao
import com.example.data.dao.OccurrenceDao
import com.example.data.model.Employee
import com.example.data.model.SafetyOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(
    entities = [SafetyOccurrence::class, Employee::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun employeeDao(): EmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns safely with default values to preserve existing records
                db.execSQL("ALTER TABLE occurrences ADD COLUMN perigo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN probabilidade INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN severidade INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN prioridade TEXT NOT NULL DEFAULT 'Prioridade normal'")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN acaoPreventiva TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN setorResponsavel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN dataAbertura TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN dataConclusao TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN responsavelValidacao TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN observacoesAcao TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN fotoDepoisUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN descricaoSolucao TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN avaliacaoEficacia TEXT NOT NULL DEFAULT 'Pendente'")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN categoriaCausa TEXT NOT NULL DEFAULT 'Mão de Obra / Fator Humano'")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN causaSecundaria TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safety_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialEmployees(database.employeeDao())
                        populateInitialOccurrences(database.occurrenceDao())
                    }
                }
            }

            suspend fun populateInitialEmployees(employeeDao: EmployeeDao) {
                val initialEmployees = listOf(
                    Employee("1001", "João Carlos Silva", "Manutenção Industrial"),
                    Employee("1002", "Maria Eduarda Santos", "Linha de Produção 01"),
                    Employee("1003", "Carlos Alberto Oliveira", "Logística e Expedição"),
                    Employee("1004", "Ana Paula Souza", "Controle de Qualidade"),
                    Employee("1005", "Pedro Henrique Lima", "Almoxarifado Central"),
                    Employee("1006", "Fernanda Costa Rocha", "Operações Especiais"),
                    Employee("1007", "Luciana Martins", "Utilidades e Utilidades"),
                    Employee("1008", "Marcelo Ribeiro", "Engenharia de Processos")
                )
                employeeDao.insertAll(initialEmployees)
            }

            suspend fun populateInitialOccurrences(occurrenceDao: OccurrenceDao) {
                val demoOccurrences = listOf(
                    SafetyOccurrence(
                        data = "16/08/2026",
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
                        prazoAcao = "17/08/2026",
                        perigo = "Fluido hidráulico sob alta pressão e piso escorregadio",
                        probabilidade = 4,
                        severidade = 4,
                        prioridade = "Tratativa imediata",
                        acaoPreventiva = "Inspeção e troca preventiva de todas as mangueiras da linha",
                        setorResponsavel = "Manutenção Mecânica",
                        dataAbertura = "16/08/2026",
                        observacoesAcao = "Aguardando chegada da mangueira blindada de reposição",
                        categoriaCausa = "Máquinas / Equipamentos",
                        causaSecundaria = "Material / Desgaste de mangueira"
                    ),
                    SafetyOccurrence(
                        data = "16/08/2026",
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
                        prazoAcao = "18/08/2026",
                        perigo = "Queda de materiais em altura sobre transeuntes",
                        probabilidade = 3,
                        severidade = 3,
                        prioridade = "Prioridade alta",
                        acaoPreventiva = "Treinamento e DDS sobre limite de empilhamento seguro",
                        setorResponsavel = "Logística",
                        dataAbertura = "16/08/2026",
                        categoriaCausa = "Método / Procedimentos",
                        causaSecundaria = "Mão de Obra / Treinamento operacional"
                    ),
                    SafetyOccurrence(
                        data = "15/08/2026",
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
                        prazoAcao = "15/08/2026",
                        perigo = "Exposição a ruído contínuo acima do limite de tolerância",
                        probabilidade = 2,
                        severidade = 2,
                        prioridade = "Prioridade normal",
                        acaoPreventiva = "Campanha de conscientização de PCA e conservação auditiva",
                        setorResponsavel = "Produção / SESMT",
                        dataAbertura = "15/08/2026",
                        dataConclusao = "15/08/2026",
                        responsavelValidacao = "Téc. Juliana SESMT",
                        descricaoSolucao = "Colaborador reorientado e entregue novo protetor concha certificado",
                        avaliacaoEficacia = "Eficaz",
                        categoriaCausa = "Material / EPI / Ferramentas",
                        causaSecundaria = "Mão de Obra / Fator Humano"
                    ),
                    SafetyOccurrence(
                        data = "15/08/2026",
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
                        prazoAcao = "16/08/2026",
                        perigo = "Tropeço e queda em mesmo nível por baixa iluminação",
                        probabilidade = 2,
                        severidade = 1,
                        prioridade = "Programada",
                        acaoPreventiva = "Cronograma quinzenal de vistoria luminotécnica",
                        setorResponsavel = "Facilities",
                        dataAbertura = "15/08/2026",
                        dataConclusao = "16/08/2026",
                        responsavelValidacao = "Eng. Facilities",
                        descricaoSolucao = "Lâmpada LED 36W instalada e pintura fotoluminescente do degrau",
                        avaliacaoEficacia = "Eficaz",
                        categoriaCausa = "Meio Ambiente",
                        causaSecundaria = "Máquinas / Equipamentos"
                    ),
                    SafetyOccurrence(
                        data = "14/08/2026",
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
                        prazoAcao = "14/08/2026",
                        perigo = "Acesso acidental à área de movimentação do robô industrial (NR-12)",
                        probabilidade = 4,
                        severidade = 4,
                        prioridade = "Tratativa imediata",
                        acaoPreventiva = "Teste diário de intertravamento de segurança antes do início do turno",
                        setorResponsavel = "Automação / EHS",
                        dataAbertura = "14/08/2026",
                        dataConclusao = "14/08/2026",
                        responsavelValidacao = "Eng. Marcelo EHS",
                        descricaoSolucao = "Sensor substituído por modelo redundante certificado Categoria 4 PLe",
                        avaliacaoEficacia = "Eficaz",
                        categoriaCausa = "Máquinas / Equipamentos",
                        causaSecundaria = "Método / Procedimentos"
                    )
                )
                demoOccurrences.forEach { occurrenceDao.insertOccurrence(it) }
            }
        }
    }
}
