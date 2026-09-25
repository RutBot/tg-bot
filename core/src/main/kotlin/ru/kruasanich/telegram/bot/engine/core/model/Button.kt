package ru.kruasanich.telegram.bot.engine.core.model

/**
 * Кнопка экрана. Нажатие на кнопку — основной способ навигации в боте.
 *
 * Кнопка несёт человекочитаемый [label] и действие [action], которое движок
 * выполняет при нажатии. Технический идентификатор callback'а генерируется
 * транспортным слоем из позиции кнопки на экране.
 *
 * @property label текст на кнопке.
 * @property action действие, выполняемое при нажатии.
 */
data class Button(
    val label: String,
    val action: ButtonAction,
)

/**
 * Действие кнопки. Набор действий намеренно минимален и расширяется
 * через [Custom], чтобы не раздувать ядро.
 */
sealed interface ButtonAction {

    /** Перейти на другой экран сценария. */
    data class Navigate(val target: ScreenId) : ButtonAction

    /** Вернуться на предыдущий экран (по стеку навигации сессии). */
    data object Back : ButtonAction

    /** Вернуться на стартовый экран сценария. */
    data object Home : ButtonAction

    /**
     * Произвольное именованное действие, обрабатываемое прикладным кодом.
     *
     * Помимо имени действие несёт типобезопасный набор аргументов [args] —
     * это избавляет прикладной код от ручной «склейки» параметров в строку
     * (`"open:123:2"`) и их парсинга. Аргументы кодируются движком в
     * `callback_data` (см. `ActionCodec`); помните про лимит Telegram в 64 байта,
     * поэтому держите ключи/значения короткими и без символов `:`, `&`, `=`.
     *
     * @property name имя действия, по которому прикладной обработчик его опознаёт.
     * @property args аргументы действия (`lessonId` → `42` и т.п.).
     */
    data class Custom(
        val name: String,
        val args: Map<String, String> = emptyMap(),
    ) : ButtonAction
}
