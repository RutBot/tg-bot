package ru.kruasanich.telegram.bot.engine.core.dsl

import ru.kruasanich.telegram.bot.engine.core.model.ActionHandler
import ru.kruasanich.telegram.bot.engine.core.model.Button
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.Screen
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.session.UserSession
import ru.kruasanich.telegram.bot.engine.core.session.scope

/**
 * Builder одного экрана. Накапливает контент и клавиатуру, затем собирает [Screen].
 *
 * Контент по умолчанию динамический ([ScreenContent] вычисляется по сессии),
 * но для статичных экранов есть удобные сеттеры [text]/[video]/[image].
 *
 * @property id идентификатор собираемого экрана.
 */
@ScenarioDsl
class ScreenBuilder(private val id: ScreenId) {

    private var contentProvider: suspend (UserSession) -> ScreenContent = { ScreenContent.Empty }
    private val rows = mutableListOf<List<Button>>()
    private var keyboardProvider: (suspend (UserSession) -> List<List<Button>>)? = null
    private var textHandler: (suspend (UserSession, String) -> InputResult?)? = null
    private var guardProvider: (suspend (UserSession) -> ScreenId?)? = null
    private var enterHandler: (suspend (UserSession) -> Unit)? = null
    private val extraActions = LinkedHashMap<String, ActionHandler>()

    /** Задать статичный текстовый контент (Markdown). */
    fun text(markdown: String) {
        contentProvider = { ScreenContent.Text(markdown) }
    }

    /** Задать динамический контент, зависящий от сессии. */
    fun content(provider: suspend (UserSession) -> ScreenContent) {
        contentProvider = provider
    }

    /** Задать видеолекцию. */
    fun video(url: String, caption: String? = null) {
        contentProvider = { ScreenContent.Video(url, caption) }
    }

    /** Задать изображение. */
    fun image(url: String, caption: String? = null) {
        contentProvider = { ScreenContent.Image(url, caption) }
    }

    /** Задать документ для скачивания (PDF и т.п.). */
    fun document(url: String, fileName: String? = null, caption: String? = null) {
        contentProvider = { ScreenContent.Document(url, fileName, caption) }
    }

    /** Добавить кнопку перехода на другой экран. */
    fun navigate(label: String, target: String) {
        addButton(Button(label, ButtonAction.Navigate(ScreenId(target))))
    }

    /** Добавить кнопку «назад». */
    fun back(label: String = "⬅️ Назад") {
        addButton(Button(label, ButtonAction.Back))
    }

    /** Добавить кнопку «в начало». */
    fun home(label: String = "🏠 В меню") {
        addButton(Button(label, ButtonAction.Home))
    }

    /** Добавить кнопку с пользовательским действием и типизированными аргументами. */
    fun action(label: String, name: String, vararg args: Pair<String, String>) {
        addButton(Button(label, ButtonAction.Custom(name, args.toMap())))
    }

    /**
     * Добавить строку из нескольких кнопок (горизонтально).
     *
     * @param block описание кнопок строки.
     */
    fun row(block: RowBuilder.() -> Unit) {
        rows.add(RowBuilder().apply(block).build())
    }

    /**
     * Задать динамическую клавиатуру, зависящую от сессии.
     *
     * Если задана, переопределяет статичные кнопки экрана. Нужна для
     * экранов, где состав кнопок зависит от прогресса пользователя
     * (список лекций с блокировками, варианты ответа из БД и т.п.).
     *
     * @param provider функция, строящая строки кнопок по сессии.
     */
    fun keyboard(provider: suspend (UserSession) -> List<List<Button>>) {
        keyboardProvider = provider
    }

    /**
     * Задать обработчик свободного текстового ввода (для экранов-вопросов).
     *
     * @param handler функция обработки текста, возвращающая [InputResult] или `null`.
     */
    fun onText(handler: suspend (session: UserSession, text: String) -> InputResult?) {
        textHandler = handler
    }

    /**
     * Декларативный guard входа на экран.
     *
     * Если [allowed] возвращает `false`, движок перенаправит пользователя на
     * экран [redirectTo] (например, «занятие закрыто»). Это выносит контроль
     * доступа в карту сценария — вместо `if`-ов в обработчиках действий.
     *
     * @param redirectTo экран-перенаправление при запрете доступа.
     * @param allowed предикат разрешения входа.
     */
    fun guard(redirectTo: String, allowed: suspend (UserSession) -> Boolean) {
        val target = ScreenId(redirectTo)
        guardProvider = { session -> if (allowed(session)) null else target }
    }

    /**
     * Хук входа на экран (см. [Screen.onEnter]).
     *
     * @param handler действие при каждом входе на экран.
     */
    fun onEnter(handler: suspend (UserSession) -> Unit) {
        enterHandler = handler
    }

    /**
     * Пошаговый экран: перелистывание коллекции элементов с встроенным индексом.
     *
     * Закрывает самый частый «сложный» паттерн — последовательный проход по
     * блокам теории, страницам лекции, вопросам и т.п. Движок сам ведёт индекс
     * в области сессии, рисует кнопки «Назад/Далее» и по завершении переходит
     * на [onFinish]. Индекс сбрасывается при каждом входе на экран.
     *
     * @param items элементы для перелистывания (могут зависеть от сессии/БД).
     * @param render построение контента текущего шага: элемент, индекс, всего.
     * @param onFinish экран перехода после последнего шага.
     * @param nextLabel подпись кнопки перехода к следующему шагу.
     * @param finishLabel подпись кнопки на последнем шаге.
     * @param prevLabel подпись кнопки возврата к предыдущему шагу.
     * @param homeLabel подпись кнопки выхода в меню.
     */
    fun <T> steps(
        items: suspend (UserSession) -> List<T>,
        render: suspend (item: T, index: Int, total: Int) -> ScreenContent,
        onFinish: String,
        nextLabel: String = "Далее ➡️",
        finishLabel: String = "Завершить ✅",
        prevLabel: String = "⬅️ Назад",
        homeLabel: String = "🏠 В меню",
    ) {
        val scopeName = "step.${id.value}"
        val nextAction = "${id.value}$STEP_NEXT_SUFFIX"
        val prevAction = "${id.value}$STEP_PREV_SUFFIX"
        val finishId = ScreenId(onFinish)

        contentProvider = provider@{ session ->
            val list = items(session)
            if (list.isEmpty()) return@provider ScreenContent.Empty
            val index = (session.scope(scopeName).int(STEP_INDEX) ?: 0).coerceIn(0, list.size - 1)
            render(list[index], index, list.size)
        }

        keyboardProvider = { session ->
            val total = items(session).size
            val index = (session.scope(scopeName).int(STEP_INDEX) ?: 0).coerceIn(0, maxOf(0, total - 1))
            val navRow = mutableListOf<Button>()
            navRow += if (index > 0) Button(prevLabel, ButtonAction.Custom(prevAction))
            else Button(prevLabel, ButtonAction.Back)
            val isLast = index >= total - 1
            navRow += Button(if (isLast) finishLabel else nextLabel, ButtonAction.Custom(nextAction))
            listOf(navRow, listOf(Button(homeLabel, ButtonAction.Home)))
        }

        enterHandler = { session -> session.scope(scopeName).put(STEP_INDEX, 0) }

        extraActions[nextAction] = next@{
            val list = items(session)
            val steps = scope(scopeName)
            val index = steps.int(STEP_INDEX) ?: 0
            if (index + 1 >= list.size) {
                steps.put(STEP_INDEX, 0)
                return@next goto(finishId.value)
            }
            steps.put(STEP_INDEX, index + 1)
            stay
        }

        extraActions[prevAction] = {
            val steps = scope(scopeName)
            val index = steps.int(STEP_INDEX) ?: 0
            if (index > 0) steps.put(STEP_INDEX, index - 1)
            stay
        }
    }

    /** Собрать экран из накопленных контента, клавиатуры и хуков. */
    internal fun build(): Screen = DslScreen(id, ::buildView, textHandler, guardProvider, enterHandler)

    /** Действия, автоматически зарегистрированные экраном (пошаговые переходы). */
    internal fun actions(): Map<String, ActionHandler> = extraActions

    /** Построить представление: контент + клавиатура (динамическая имеет приоритет). */
    private suspend fun buildView(session: UserSession): ScreenView {
        val buttons = keyboardProvider?.invoke(session) ?: rows.toList()
        return ScreenView(contentProvider(session), buttons)
    }

    private fun addButton(button: Button) {
        rows.add(listOf(button))
    }

    private companion object {
        const val STEP_INDEX = "i"
        const val STEP_NEXT_SUFFIX = ".__step_next"
        const val STEP_PREV_SUFFIX = ".__step_prev"
    }
}
