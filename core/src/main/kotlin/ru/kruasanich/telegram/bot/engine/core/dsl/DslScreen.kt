package ru.kruasanich.telegram.bot.engine.core.dsl

import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.Screen
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.session.UserSession

/**
 * Универсальная реализация [Screen], создаваемая DSL.
 *
 * Поведение экрана задаётся не наследованием, а набором лямбд: построение
 * представления, обработка текста, guard входа и хук входа. Это позволяет
 * описать любой тип экрана (меню, статья, видео, вопрос, шаг, форма) данными,
 * сохраняя ядро компактным.
 *
 * @property id идентификатор экрана.
 * @property viewProvider функция построения представления по сессии.
 * @property textHandler опциональный обработчик текстового ввода.
 * @property guardProvider опциональная проверка права входа (см. [Screen.guard]).
 * @property enterHandler опциональный хук входа (см. [Screen.onEnter]).
 */
internal class DslScreen(
    override val id: ScreenId,
    private val viewProvider: suspend (UserSession) -> ScreenView,
    private val textHandler: (suspend (UserSession, String) -> InputResult?)? = null,
    private val guardProvider: (suspend (UserSession) -> ScreenId?)? = null,
    private val enterHandler: (suspend (UserSession) -> Unit)? = null,
) : Screen {

    override suspend fun view(session: UserSession): ScreenView = viewProvider(session)

    override suspend fun onText(session: UserSession, text: String): InputResult? =
        textHandler?.invoke(session, text)

    override suspend fun guard(session: UserSession): ScreenId? = guardProvider?.invoke(session)

    override suspend fun onEnter(session: UserSession) {
        enterHandler?.invoke(session)
    }
}
