package ru.kruasanich.telegram.bot.engine.core.session

import ru.kruasanich.telegram.bot.engine.core.model.ScreenId

/**
 * Сессия пользователя — изменяемое состояние диалога одного пользователя с ботом.
 *
 * Хранит текущий экран, стек навигации (для кнопки «назад») и произвольные
 * пользовательские данные (ответы на вопросы, прогресс по урокам и т.п.).
 *
 * @property chatId идентификатор чата Telegram (он же ключ сессии).
 * @property current текущий экран пользователя.
 * @property history стек посещённых экранов для навигации «назад».
 * @property attributes произвольные строковые данные сессии.
 */
data class UserSession(
    val chatId: Long,
    var current: ScreenId,
    val history: MutableList<ScreenId> = mutableListOf(),
    val attributes: MutableMap<String, String> = mutableMapOf(),
) {

    /**
     * Перейти на новый экран, запомнив текущий в истории.
     *
     * @param target экран назначения.
     */
    fun moveTo(target: ScreenId) {
        if (target != current) {
            history.add(current)
            current = target
        }
    }

    /**
     * Вернуться на предыдущий экран из истории.
     *
     * @return экран, на который вернулись, или `null`, если история пуста.
     */
    fun back(): ScreenId? {
        val previous = history.removeLastOrNull() ?: return null
        current = previous
        return previous
    }

    /** Прочитать атрибут сессии. */
    fun attribute(key: String): String? = attributes[key]

    /** Записать атрибут сессии. */
    fun putAttribute(key: String, value: String) {
        attributes[key] = value
    }
}
