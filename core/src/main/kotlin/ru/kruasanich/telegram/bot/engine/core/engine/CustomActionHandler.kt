package ru.kruasanich.telegram.bot.engine.core.engine

import ru.kruasanich.telegram.bot.engine.core.model.ActionContext
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId

/**
 * Обработчик пользовательских действий ([ru.kruasanich.telegram.bot.engine.core.model.ButtonAction.Custom]).
 *
 * Точка расширения движка для действий, не описанных в DSL `action("name") { }`.
 * Движок сначала ищет обработчик в сценарии, и только потом обращается сюда —
 * это запасной канал для глобальной/динамической логики.
 */
fun interface CustomActionHandler {

    /**
     * Обработать именованное действие.
     *
     * @param ctx контекст действия: имя, сессия и аргументы кнопки.
     * @return экран для перехода после обработки или `null`, чтобы остаться.
     */
    suspend fun handle(ctx: ActionContext): ScreenId?
}
