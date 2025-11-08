package ua.company.tzd.model

/**
 * Опис одного списку для довготривалого зберігання результатів сканування.
 * Зберігаємо мінімальні поля, які потрібні для відображення переліку та службових дій.
 */
data class ScanListInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
