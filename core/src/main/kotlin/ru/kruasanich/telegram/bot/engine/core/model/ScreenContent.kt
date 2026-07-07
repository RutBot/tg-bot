package ru.kruasanich.telegram.bot.engine.core.model

/**
 * Контент экрана, отображаемый пользователю.
 *
 * Контент сознательно отделён от транспорта: ядро описывает «что показать»,
 * а конкретный рендерер (Telegram и т.п.) решает «как показать».
 */
sealed interface ScreenContent {

    /** Простой текстовый блок (статья, абзац урока, приветствие). */
    data class Text(val markdown: String) : ScreenContent

    /**
     * Видеолекция: ссылка на видео и опциональное текстовое описание.
     *
     * @property url URL видео (внешняя ссылка или file_id Telegram).
     * @property caption подпись под видео.
     */
    data class Video(val url: String, val caption: String? = null) : ScreenContent

    /**
     * Изображение с опциональной подписью.
     *
     * @property url URL картинки или file_id Telegram.
     * @property caption подпись под изображением.
     */
    data class Image(val url: String, val caption: String? = null) : ScreenContent

    /**
     * Документ (файл) для скачивания: PDF полного текста лекции, методичка и т.п.
     *
     * @property url URL файла или file_id Telegram.
     * @property fileName имя файла, под которым он придёт пользователю.
     * @property caption подпись к документу.
     */
    data class Document(
        val url: String,
        val fileName: String? = null,
        val caption: String? = null,
    ) : ScreenContent

    /** Экран без статичного контента (например, чистый вопрос-промпт). */
    data object Empty : ScreenContent
}
