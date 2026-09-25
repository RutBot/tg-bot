package ru.kruasanich.telegram.bot.engine.core.dsl

import ru.kruasanich.telegram.bot.engine.core.model.ActionHandler
import ru.kruasanich.telegram.bot.engine.core.model.Button
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.model.Screen
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.session.UserSession
import ru.kruasanich.telegram.bot.engine.core.session.scope

/**
 * Builder сценария. Регистрирует экраны, действия и фиксирует стартовый экран.
 *
 * @property id логическое имя сценария.
 */
@ScenarioDsl
class ScenarioBuilder(private val id: String) {

    private val screens = LinkedHashMap<ScreenId, Screen>()
    private val actions = LinkedHashMap<String, ActionHandler>()
    private var start: ScreenId? = null

    /**
     * Описать экран сценария.
     *
     * @param id строковый идентификатор экрана.
     * @param block конфигурация экрана.
     */
    fun screen(id: String, block: ScreenBuilder.() -> Unit) {
        val screenId = ScreenId(id)
        require(!screens.containsKey(screenId)) { "Duplicate screen id '$id' in scenario '${this.id}'" }
        val builder = ScreenBuilder(screenId).apply(block)
        screens[screenId] = builder.build()
        builder.actions().forEach { (name, handler) -> registerAction(name, handler) }
    }

    /**
     * Зарегистрировать обработчик кнопочного действия (маршрут).
     *
     * Стиль «маршрутизации»: действие кнопки `action("open", ...)` обрабатывается
     * блоком `action("open") { ... }` рядом с описанием экранов. Внутри блока
     * доступны имя, сессия и типизированные аргументы (`argLong`, `arg` и т.п.),
     * а возврат [ScreenId] задаёт переход (`null` — остаться на экране).
     *
     * @param name имя действия.
     * @param handler обработчик.
     */
    fun action(name: String, handler: ActionHandler) {
        registerAction(name, handler)
    }

    /**
     * Описать многошаговую форму как отдельный экран.
     *
     * Экран с идентификатором [id] последовательно собирает поля (с валидацией),
     * складывает ответы в область сессии `form.<id>` и по завершении переходит
     * на экран [onComplete]. Каждый вход на экран начинает форму заново.
     *
     * @param id идентификатор экрана-формы.
     * @param onComplete экран перехода после заполнения всех полей.
     * @param completion опциональное сообщение после завершения формы.
     * @param cancelLabel подпись кнопки отмены (возврат назад).
     * @param block описание полей формы.
     */
    fun form(
        id: String,
        onComplete: String,
        completion: String? = "Готово!",
        cancelLabel: String = "⬅️ Отмена",
        block: FormBuilder.() -> Unit,
    ) {
        val screenId = ScreenId(id)
        require(!screens.containsKey(screenId)) { "Duplicate screen id '$id' in scenario '${this.id}'" }
        val fields = FormBuilder().apply(block).fields.toList()
        require(fields.isNotEmpty()) { "Form '$id' must declare at least one field" }
        val scopeName = "form.$id"
        val completeId = ScreenId(onComplete)

        val view: suspend (UserSession) -> ScreenView = { session ->
            val index = (session.scope(scopeName).int(FORM_INDEX) ?: 0).coerceIn(0, fields.size - 1)
            ScreenView(
                ScreenContent.Text(fields[index].prompt),
                listOf(listOf(Button(cancelLabel, ButtonAction.Back))),
            )
        }

        val onText: suspend (UserSession, String) -> InputResult? = { session, text ->
            val formScope = session.scope(scopeName)
            val index = formScope.int(FORM_INDEX) ?: 0
            val field = fields[index]
            val valid = field.validate?.invoke(text) ?: true
            if (!valid) {
                InputResult(reply = field.error)
            } else {
                formScope.put(field.name, text)
                val nextIndex = index + 1
                if (nextIndex >= fields.size) {
                    formScope.put(FORM_INDEX, 0)
                    InputResult(reply = completion, next = completeId)
                } else {
                    formScope.put(FORM_INDEX, nextIndex)
                    InputResult(reply = fields[nextIndex].prompt)
                }
            }
        }

        val onEnter: suspend (UserSession) -> Unit = { session ->
            session.scope(scopeName).put(FORM_INDEX, 0)
        }

        screens[screenId] = DslScreen(screenId, view, onText, guardProvider = null, enterHandler = onEnter)
    }

    /**
     * Указать стартовый экран сценария (точка входа по `/start`).
     *
     * @param id идентификатор стартового экрана.
     */
    fun startAt(id: String) {
        start = ScreenId(id)
    }

    /** Собрать готовый [Scenario]. */
    internal fun build(): Scenario {
        val startId = requireNotNull(start) { "Start screen is not set for scenario '$id'" }
        return Scenario(id = id, start = startId, screens = screens, actions = actions)
    }

    private fun registerAction(name: String, handler: ActionHandler) {
        require(!actions.containsKey(name)) { "Duplicate action '$name' in scenario '$id'" }
        actions[name] = handler
    }

    private companion object {
        const val FORM_INDEX = "i"
    }
}

/**
 * Точка входа в DSL: описать сценарий бота.
 *
 * Пример:
 * ```
 * val course = scenario("course") {
 *     startAt("menu")
 *     screen("menu") {
 *         text("Главное меню")
 *         navigate("Урок 1", "lesson1")
 *     }
 *     screen("lesson1") {
 *         text("Содержимое урока")
 *         back()
 *     }
 *     // действие-маршрут с типизированными аргументами
 *     action("complete") { /* this: ActionContext */ goto("menu") }
 * }
 * ```
 *
 * @param id логическое имя сценария.
 * @param block описание экранов, действий и точки входа.
 * @return готовый к исполнению [Scenario].
 */
fun scenario(id: String, block: ScenarioBuilder.() -> Unit): Scenario =
    ScenarioBuilder(id).apply(block).build()
