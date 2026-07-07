package ru.kruasanich.telegram.bot.engine.core.port

import ru.kruasanich.telegram.bot.engine.core.model.ScreenView

/**
 * Исходящий порт: отправка контента пользователю.
 *
 * Реализуется транспортным слоем (Telegram). Движок вызывает этот порт,
 * чтобы показать экран или отправить служебное сообщение, не зная деталей API.
 */
interface BotSender {

    /**
     * Отправить пользователю готовое представление экрана.
     *
     * @param chatId чат получателя.
     * @param view контент и клавиатура экрана.
     */
    suspend fun send(chatId: Long, view: ScreenView)

    /**
     * Отправить простое текстовое сообщение без клавиатуры.
     *
     * Используется для подсказок и ответов на текстовый ввод.
     *
     * @param chatId чат получателя.
     * @param text текст сообщения.
     */
    suspend fun sendText(chatId: Long, text: String)
}
