package ru.kruasanich.tg.bot

import kotlinx.coroutines.test.runTest
import ru.kruasanich.telegram.bot.engine.core.engine.ActionCodec
import ru.kruasanich.telegram.bot.engine.core.engine.ScreenEngine
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.port.BotSender
import ru.kruasanich.telegram.bot.engine.core.port.IncomingUpdate
import ru.kruasanich.telegram.bot.engine.core.session.InMemorySessionStore
import ru.kruasanich.tg.bot.scenario.CryptoScenarioConfig
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Тесты флоу крипто-бота на чистом движке (без Spring/Telegram): проверяем
 * навигацию `/start` → подробнее → регистрация → главное меню, а также guard'ы.
 */
class CryptoScenarioTest {

    private class RecordingSender : BotSender {
        val views = mutableListOf<ScreenView>()
        val texts = mutableListOf<String>()
        override suspend fun send(chatId: Long, view: ScreenView) { views.add(view) }
        override suspend fun sendText(chatId: Long, text: String) { texts.add(text) }
        fun lastMarkdown(): String = (views.last().content as ScreenContent.Text).markdown
    }

    private val scenario: Scenario = CryptoScenarioConfig().cryptoScenario()

    private fun harness(): Pair<ScreenEngine, RecordingSender> {
        val sender = RecordingSender()
        val engine = ScreenEngine(scenario, InMemorySessionStore(), sender)
        return engine to sender
    }

    private fun navigate(target: String) = ActionCodec.encode(ButtonAction.Navigate(ScreenId(target)))
    private fun custom(name: String, vararg args: Pair<String, String>) =
        ActionCodec.encode(ButtonAction.Custom(name, args.toMap()))

    @Test
    fun `full happy path from start to main menu`() = runTest {
        val (engine, sender) = harness()
        val chat = 42L

        engine.handle(IncomingUpdate.Start(chat))
        assertTrue(sender.lastMarkdown().contains("CryptoStart"), "start screen shows project info")

        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("details")))
        assertTrue(sender.lastMarkdown().contains("Крипторынок и торги"), "details shows market info")

        // Зарегистрироваться → пошаговая регистрация.
        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_name")))
        assertTrue(sender.lastMarkdown().contains("шаг 1/4"), "step 1 prompt")

        engine.handle(IncomingUpdate.TextMessage(chat, "Иван"))
        assertTrue(sender.texts.last().contains("Имя принято"), "name accepted")

        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_surname")))
        assertTrue(sender.lastMarkdown().contains("шаг 2/4"), "step 2 prompt")
        engine.handle(IncomingUpdate.TextMessage(chat, "Петров"))

        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_age")))
        assertTrue(sender.lastMarkdown().contains("шаг 3/4"), "step 3 prompt")

        engine.handle(IncomingUpdate.TextMessage(chat, "не число"))
        assertTrue(sender.texts.last().contains("от 1 до 120"), "age validation rejects non-number")
        engine.handle(IncomingUpdate.TextMessage(chat, "30"))
        assertTrue(sender.texts.last().contains("Возраст принят"), "valid age accepted")

        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_gender")))
        assertTrue(sender.lastMarkdown().contains("шаг 4/4"), "step 4 prompt")
        engine.handle(IncomingUpdate.ButtonPressed(chat, custom("set_gender", "v" to "male")))

        // Далее → главное меню.
        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("main")))
        assertTrue(sender.lastMarkdown().contains("Готово, Иван Петров"), "main menu greets by name")

        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("profile")))
        val profile = sender.lastMarkdown()
        assertTrue(profile.contains("Иван") && profile.contains("Петров"), "profile shows name")
        assertTrue(profile.contains("30") && profile.contains("Мужской"), "profile shows age and gender")
    }

    @Test
    fun `guard blocks skipping steps`() = runTest {
        val (engine, sender) = harness()
        val chat = 7L

        engine.handle(IncomingUpdate.Start(chat))
        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_name")))
        // Пытаемся перескочить к возрасту без имени/фамилии — guard возвращает на имя.
        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("reg_age")))
        assertTrue(sender.lastMarkdown().contains("шаг 1/4"), "guard redirects back to name step")
    }

    @Test
    fun `main menu is not reachable before registration`() = runTest {
        val (engine, sender) = harness()
        val chat = 9L

        engine.handle(IncomingUpdate.Start(chat))
        engine.handle(IncomingUpdate.ButtonPressed(chat, navigate("main")))
        // Без пола guard уводит на шаг выбора пола, а его guard — на возраст, и т.д. до имени.
        assertTrue(sender.lastMarkdown().contains("Регистрация"), "cannot open main without registration")
    }
}
