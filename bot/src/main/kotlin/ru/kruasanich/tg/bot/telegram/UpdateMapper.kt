package ru.kruasanich.tg.bot.telegram

import org.telegram.telegrambots.meta.api.objects.Update
import ru.kruasanich.telegram.bot.engine.core.port.IncomingUpdate

/**
 * Преобразует «сырой» Telegram [Update] в транспортно-независимый [IncomingUpdate].
 *
 * Вся логика распознавания типа апдейта сосредоточена здесь, чтобы ядро движка
 * оставалось свободным от деталей Telegram API.
 */
object UpdateMapper {

    private const val START_COMMAND = "/start"

    /**
     * Сопоставить Telegram-апдейт с доменным апдейтом движка.
     *
     * @param update апдейт от Telegram.
     * @return доменный апдейт или `null`, если апдейт не поддерживается.
     */
    fun map(update: Update): IncomingUpdate? = when {
        update.hasCallbackQuery() -> toButtonPressed(update)
        update.hasMessage() && update.message.hasText() -> toMessage(update)
        else -> null
    }

    private fun toButtonPressed(update: Update): IncomingUpdate {
        val query = update.callbackQuery
        return IncomingUpdate.ButtonPressed(
            chatId = query.message.chatId,
            payload = query.data,
        )
    }

    private fun toMessage(update: Update): IncomingUpdate {
        val message = update.message
        val text = message.text.trim()
        return if (text.startsWith(START_COMMAND)) {
            IncomingUpdate.Start(message.chatId)
        } else {
            IncomingUpdate.TextMessage(message.chatId, text)
        }
    }
}
