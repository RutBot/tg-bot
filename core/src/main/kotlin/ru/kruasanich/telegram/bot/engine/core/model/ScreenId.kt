package ru.kruasanich.telegram.bot.engine.core.model

/**
 * Типобезопасный идентификатор экрана сценария.
 *
 * Используется как ключ в реестре экранов и в переходах между ними.
 * Обёртка над [String] защищает от случайной передачи произвольной строки
 * там, где ожидается ссылка на экран.
 *
 * @property value уникальное в рамках сценария имя экрана.
 */
@JvmInline
value class ScreenId(val value: String) {
    init {
        require(value.isNotBlank()) { "ScreenId must not be blank" }
    }

    override fun toString(): String = value
}
