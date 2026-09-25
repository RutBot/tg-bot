package ru.kruasanich.telegram.bot.engine.core.model

import ru.kruasanich.telegram.bot.engine.core.session.SessionScope
import ru.kruasanich.telegram.bot.engine.core.session.UserSession
import ru.kruasanich.telegram.bot.engine.core.session.scope

/**
 * Контекст обработки кнопочного действия.
 *
 * Передаётся в обработчики действий (DSL `action("name") { ... }` и
 * `CustomActionHandler`) и даёт всё нужное в одном месте: имя действия,
 * сессию пользователя и типобезопасный доступ к аргументам кнопки. Это убирает
 * ручной разбор `name.split(":")`, который раньше дублировался в каждом боте.
 *
 * @property name имя действия.
 * @property session сессия пользователя (её можно изменять).
 * @property args аргументы, переданные кнопкой.
 */
class ActionContext(
    val name: String,
    val session: UserSession,
    val args: Map<String, String> = emptyMap(),
) {

    /** Аргумент действия или `null`. */
    fun arg(key: String): String? = args[key]

    /** Обязательный аргумент действия; бросает, если его нет. */
    fun requireArg(key: String): String =
        args[key] ?: error("Action '$name' requires argument '$key'")

    /** Аргумент как [Long]. */
    fun argLong(key: String): Long? = args[key]?.toLongOrNull()

    /** Аргумент как [Int]. */
    fun argInt(key: String): Int? = args[key]?.toIntOrNull()

    /** Аргумент как [Boolean]. */
    fun argBoolean(key: String): Boolean? = args[key]?.toBooleanStrictOrNull()

    /** Открыть именованную область сессии (см. [scope]). */
    fun scope(name: String): SessionScope = session.scope(name)

    /** Перейти на экран `id` после обработки. */
    fun goto(id: String): ScreenId = ScreenId(id)

    /** Остаться на текущем экране (перерисовать его). */
    val stay: ScreenId? get() = null
}

/**
 * Обработчик кнопочного действия в DSL.
 *
 * Возвращает экран для перехода или `null`, чтобы перерисовать текущий экран.
 */
typealias ActionHandler = suspend ActionContext.() -> ScreenId?
