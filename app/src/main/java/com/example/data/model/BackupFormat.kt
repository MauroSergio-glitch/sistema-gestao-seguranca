package com.example.data.model

enum class BackupFormat(
    val displayName: String,
    val description: String,
    val extensionLabel: String
) {
    PDF(
        displayName = "Relatório Executivo PDF",
        description = "Documento diagramado com tabelas de ocorrências, causas, status de ações e gráficos para impressão e auditoria.",
        extensionLabel = ".PDF"
    ),
    GOOGLE_SHEETS_CSV(
        displayName = "Planilha Google Sheets / Excel",
        description = "Arquivo CSV com colunas formatadas e UTF-8 BOM, pronto para abrir diretamente no Google Planilhas ou Microsoft Excel.",
        extensionLabel = ".CSV"
    ),
    JSON_DATA(
        displayName = "Arquivo de Dados Estruturado (JSON)",
        description = "Backup técnico completo contendo todos os registros em JSON para restauração, integração ou arquivo de segurança.",
        extensionLabel = ".JSON"
    ),
    ALL_FORMATS(
        displayName = "Pacote Completo (PDF + Planilha CSV)",
        description = "Gera e exporta simultaneamente o Relatório Executivo em PDF e a Planilha consolidada em CSV.",
        extensionLabel = ".PDF + .CSV"
    )
}
