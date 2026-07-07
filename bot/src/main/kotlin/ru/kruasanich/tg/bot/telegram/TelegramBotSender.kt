package ru.kruasanich.tg.bot.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.kruasanich.telegram.bot.engine.core.engine.ActionCodec
import ru.kruasanich.telegram.bot.engine.core.model.Button
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.port.BotSender

/**
 * Telegram-реализация [BotSender] (API telegrambots 7.x).
 *
 * Преобразует контент экрана в соответствующий Telegram-метод (текст/фото/видео)
 * и строит inline-клавиатуру, кодируя действия кнопок через [ActionCodec].
 * Сетевые вызовы выполняются на [Dispatchers.IO] через [TelegramClient].
 *
 * @property client низкоуровневый Telegram-клиент.
 */
class TelegramBotSender(
    private val client: TelegramClient,
) : BotSender {

    private companion object {
        const val MARKDOWN = "Markdown"
    }

    override suspend fun send(chatId: Long, view: ScreenView) {
        val keyboard = view.buttons.toMarkup()
        when (val content = view.content) {
            is ScreenContent.Text -> execute(textMessage(chatId, content.markdown, keyboard))
            is ScreenContent.Video -> execute(videoMessage(chatId, content, keyboard))
            is ScreenContent.Image -> execute(photoMessage(chatId, content, keyboard))
            is ScreenContent.Document -> execute(documentMessage(chatId, content, keyboard))
            ScreenContent.Empty -> execute(textMessage(chatId, "…", keyboard))
        }
    }

    override suspend fun sendText(chatId: Long, text: String) {
        execute(textMessage(chatId, text, null))
    }

    private fun textMessage(chatId: Long, text: String, markup: InlineKeyboardMarkup?): SendMessage =
        SendMessage(chatId.toString(), text).apply {
            parseMode = MARKDOWN
            markup?.let { replyMarkup = it }
        }

    private fun videoMessage(chatId: Long, content: ScreenContent.Video, markup: InlineKeyboardMarkup?): SendVideo =
        SendVideo(chatId.toString(), InputFile(content.url)).apply {
            content.caption?.let {
                caption = it
                parseMode = MARKDOWN
            }
            markup?.let { replyMarkup = it }
        }

    private fun photoMessage(chatId: Long, content: ScreenContent.Image, markup: InlineKeyboardMarkup?): SendPhoto =
        SendPhoto(chatId.toString(), InputFile(content.url)).apply {
            content.caption?.let {
                caption = it
                parseMode = MARKDOWN
            }
            markup?.let { replyMarkup = it }
        }

    private fun documentMessage(chatId: Long, content: ScreenContent.Document, markup: InlineKeyboardMarkup?): SendDocument =
        SendDocument(chatId.toString(), InputFile(content.url)).apply {
            content.caption?.let {
                caption = it
                parseMode = MARKDOWN
            }
            markup?.let { replyMarkup = it }
        }

    /** Построить inline-клавиатуру из строк кнопок; `null`, если кнопок нет. */
    private fun List<List<Button>>.toMarkup(): InlineKeyboardMarkup? {
        if (isEmpty()) return null
        val rows = map { row -> InlineKeyboardRow(row.map { it.toInlineButton() }) }
        return InlineKeyboardMarkup(rows)
    }

    private fun Button.toInlineButton(): InlineKeyboardButton =
        InlineKeyboardButton(label).apply { callbackData = ActionCodec.encode(action) }

    private suspend fun execute(method: SendMessage) = withContext(Dispatchers.IO) { client.execute(method) }
    private suspend fun execute(method: SendVideo) = withContext(Dispatchers.IO) { client.execute(method) }
    private suspend fun execute(method: SendPhoto) = withContext(Dispatchers.IO) { client.execute(method) }
    private suspend fun execute(method: SendDocument) = withContext(Dispatchers.IO) { client.execute(method) }
}
