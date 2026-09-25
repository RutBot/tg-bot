package ru.kruasanich.telegram.bot.engine.core.dsl

import ru.kruasanich.telegram.bot.engine.core.model.Button
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId

/**
 * Builder одной строки клавиатуры: несколько кнопок в ряд.
 *
 * Используется, когда нужно разместить кнопки горизонтально (например,
 * варианты ответа на вопрос или компактная сетка разделов). Набор методов
 * совпадает с [ScreenBuilder], но кнопки накапливаются в одну строку.
 */
@ScenarioDsl
class RowBuilder {

    private val buttons = mutableListOf<Button>()

    /** Кнопка перехода на другой экран. */
    fun navigate(label: String, target: String) {
        buttons.add(Button(label, ButtonAction.Navigate(ScreenId(target))))
    }

    /** Кнопка «назад». */
    fun back(label: String = "⬅️ Назад") {
        buttons.add(Button(label, ButtonAction.Back))
    }

    /** Кнопка «в начало». */
    fun home(label: String = "🏠 В меню") {
        buttons.add(Button(label, ButtonAction.Home))
    }

    /** Кнопка с пользовательским действием и типизированными аргументами. */
    fun action(label: String, name: String, vararg args: Pair<String, String>) {
        buttons.add(Button(label, ButtonAction.Custom(name, args.toMap())))
    }

    /** Собрать строку кнопок. */
    internal fun build(): List<Button> = buttons.toList()
}
