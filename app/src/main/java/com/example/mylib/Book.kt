package com.example.mylib

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import java.io.Serializable
import java.time.LocalDate

data class Book(
    val title: String,
    val authors: String,
    val thumbnail: String,
    val description: String,
    var note: String? = null,
    var genre: String? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    val publisher: String,
    val publishedDate: String,
    val pageCount: Int,
    val previewLink: String,
    val webReaderLink: String,
    val accessInfo: AccessInfo? = null,
    val searchInfo: SearchInfo? = null,
    var isFavorite: Boolean = false
) : Serializable {

    var isRead: Boolean = false

    init {
        updateReadStatus()  // Проверяем статус при создании
    }

    // Публичный метод для изменения статуса прочитанности
    fun setReadStatus(isRead: Boolean) {
        this.isRead = isRead
        updateReadStatus()
    }

    fun updateReadStatus() {
        isRead = endDate?.isBefore(LocalDate.now().plusDays(1)) == true
    }

    val text: String?
        get() = searchInfo?.textSnippet ?: "Текст недоступен"
}



// Модель для доступа к форматам книги
data class AccessInfo(
    val epub: FormatInfo?,  // Доступность EPUB
    val pdf: FormatInfo?,   // Доступность PDF
    val viewability: String // Статус видимости (например, "ALL_PAGES")
)

// Модель для информации о формате (например, EPUB или PDF)
data class FormatInfo(
    val isAvailable: Boolean, // Доступность формата
    val downloadLink: String? // Ссылка на скачивание
)

// Модель для хранения отрывка текста (textSnippet)
data class SearchInfo(
    val textSnippet: String?
)
