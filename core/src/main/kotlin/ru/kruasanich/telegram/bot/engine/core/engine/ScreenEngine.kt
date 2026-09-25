package ru.kruasanich.telegram.bot.engine.core.engine

import org.slf4j.LoggerFactory
import ru.kruasanich.telegram.bot.engine.core.model.ActionContext
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.model.Screen
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.port.BotSender
import ru.kruasanich.telegram.bot.engine.core.port.IncomingUpdate
import ru.kruasanich.telegram.bot.engine.core.session.SessionStore
import ru.kruasanich.telegram.bot.engine.core.session.UserSession

/**
 * Конечный автомат движка: исполняет [Scenario] для входящих апдейтов.
 *
 * Это центральный класс ядра. Он ничего не знает про Telegram и хранилище —
 * работает через порты [BotSender] и [SessionStore]. Логика разбита на
 * маленькие приватные методы по типам апдейтов и действий.
 *
 * @property scenario исполняемый сценарий.
 * @property sessions хранилище сессий.
 * @property sender порт отправки сообщений.
 * @property customActions запасной обработчик действий, не описанных в сценарии.
 */
class ScreenEngine(
    private val scenario: Scenario,
    private val sessions: SessionStore,
    private val sender: BotSender,
    private val customActions: CustomActionHandler = CustomActionHandler { null },
) {

    private val log = LoggerFactory.getLogger(ScreenEngine::class.java)

    /**
     * Обработать входящий апдейт от пользователя.
     *
     * @param update транспортно-независимый апдейт.
     */
    suspend fun handle(update: IncomingUpdate) {
        val session = loadOrStart(update.chatId)
        when (update) {
            is IncomingUpdate.Start -> restart(session)
            is IncomingUpdate.ButtonPressed -> onButton(session, update.payload)
            is IncomingUpdate.TextMessage -> onText(session, update.text)
        }
        sessions.save(session)
    }

    /** Загрузить существующую сессию или создать новую на стартовом экране. */
    private suspend fun loadOrStart(chatId: Long): UserSession =
        sessions.load(chatId) ?: UserSession(chatId = chatId, current = scenario.start)

    /** Сбросить навигацию и показать стартовый экран. */
    private suspend fun restart(session: UserSession) {
        session.history.clear()
        session.current = scenario.start
        enter(session, scenario.start)
        render(session, scenario.start)
    }

    /** Обработать нажатие кнопки: декодировать действие и применить переход. */
    private suspend fun onButton(session: UserSession, payload: String) {
        val action = ActionCodec.decode(payload)
        if (action == null) {
            log.warn("Unknown callback payload '{}' in scenario '{}'", payload, scenario.id)
            return
        }
        applyAction(session, action)
    }

    /** Применить декодированное действие кнопки к сессии. */
    private suspend fun applyAction(session: UserSession, action: ButtonAction) {
        when (action) {
            is ButtonAction.Navigate -> navigate(session, action.target)
            ButtonAction.Back -> renderCurrent(session, session.back() ?: scenario.start)
            ButtonAction.Home -> restart(session)
            is ButtonAction.Custom -> dispatchCustom(session, action)
        }
    }

    /** Выполнить пользовательское действие: сценарный обработчик или запасной. */
    private suspend fun dispatchCustom(session: UserSession, action: ButtonAction.Custom) {
        val ctx = ActionContext(action.name, session, action.args)
        val handler = scenario.action(action.name)
        val next = if (handler != null) handler.invoke(ctx) else customActions.handle(ctx)
        if (next != null) navigate(session, next) else renderCurrent(session, session.current)
    }

    /**
     * Перейти на экран с записью текущего в историю.
     *
     * Перед переходом применяется guard экрана назначения: при запрете движок
     * перенаправляет на указанный экран.
     */
    private suspend fun navigate(session: UserSession, target: ScreenId) {
        val destination = resolveAccessible(session, target)
        session.moveTo(destination)
        enter(session, destination)
        render(session, destination)
    }

    /** Перерисовать конкретный экран без изменения истории. */
    private suspend fun renderCurrent(session: UserSession, target: ScreenId) {
        session.current = target
        render(session, target)
    }

    /** Делегировать текстовый ввод текущему экрану. */
    private suspend fun onText(session: UserSession, text: String) {
        val screen = requireScreen(session.current)
        val result = screen.onText(session, text)
        if (result == null) {
            sender.sendText(session.chatId, "Пожалуйста, используйте кнопки для навигации.")
            return
        }
        result.reply?.let { sender.sendText(session.chatId, it) }
        result.next?.let { navigate(session, it) }
    }

    /**
     * Разрешить целевой экран через цепочку guard'ов.
     *
     * Следует за перенаправлениями guard'ов (с защитой от зацикливания), чтобы
     * вернуть фактически доступный экран.
     */
    private suspend fun resolveAccessible(session: UserSession, target: ScreenId): ScreenId {
        var current = target
        repeat(MAX_GUARD_HOPS) {
            val redirect = requireScreen(current).guard(session)
            if (redirect == null || redirect == current) return current
            current = redirect
        }
        return current
    }

    /** Вызвать хук входа на экран. */
    private suspend fun enter(session: UserSession, id: ScreenId) {
        requireScreen(id).onEnter(session)
    }

    /** Отрисовать экран: построить view и отправить через порт. */
    private suspend fun render(session: UserSession, id: ScreenId) {
        val screen = requireScreen(id)
        sender.send(session.chatId, screen.view(session))
    }

    /** Получить экран по id или упасть с понятной ошибкой конфигурации сценария. */
    private fun requireScreen(id: ScreenId): Screen =
        scenario.screen(id)
            ?: error("Screen '$id' is not registered in scenario '${scenario.id}'")

    private companion object {
        /** Предел перенаправлений guard'ов — защита от циклов в конфигурации. */
        const val MAX_GUARD_HOPS = 5
    }
}
