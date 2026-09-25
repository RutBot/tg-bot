package ru.kruasanich.telegram.bot.engine.core.port

/**
 * Входящий апдейт от пользователя в транспортно-независимом виде.
 *
 * Транспортный слой (Telegram) преобразует «сырой» апдейт в одну из реализаций
 * этого интерфейса, после чего движок работает с ним, ничего не зная о Telegram.
 *
 * @property chatId идентификатор чата, из которого пришёл апдейт.
 */
sealed interface IncomingUpdate {

    val chatId: Long

    /** Команда `/start` — начать или перезапустить сценарий. */
    data class Start(override val chatId: Long) : IncomingUpdate

    /**
     * Нажатие на кнопку (callback).
     *
     * @property payload технический идентификатор нажатой кнопки.
     */
    data class ButtonPressed(
        override val chatId: Long,
        val payload: String,
    ) : IncomingUpdate

    /**
     * Свободный текстовый ввод пользователя.
     *
     * @property text введённый текст.
     */
    data class TextMessage(
        override val chatId: Long,
        val text: String,
    ) : IncomingUpdate
}
