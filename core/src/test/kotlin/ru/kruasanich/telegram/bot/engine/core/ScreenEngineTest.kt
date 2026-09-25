package ru.kruasanich.telegram.bot.engine.core

import kotlinx.coroutines.test.runTest
import ru.kruasanich.telegram.bot.engine.core.dsl.scenario
import ru.kruasanich.telegram.bot.engine.core.engine.ActionCodec
import ru.kruasanich.telegram.bot.engine.core.engine.ScreenEngine
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.model.ScreenView
import ru.kruasanich.telegram.bot.engine.core.port.BotSender
import ru.kruasanich.telegram.bot.engine.core.port.IncomingUpdate
import ru.kruasanich.telegram.bot.engine.core.session.InMemorySessionStore
import ru.kruasanich.telegram.bot.engine.core.session.UserSession
import ru.kruasanich.telegram.bot.engine.core.session.boolAttr
import ru.kruasanich.telegram.bot.engine.core.session.putAttribute
import ru.kruasanich.telegram.bot.engine.core.session.scope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Тесты ядра движка: навигация, ввод, кодек, аргументы, guard, шаги, формы. */
class ScreenEngineTest {

    /** Сборщик отправленных представлений для проверки рендеринга. */
    private class RecordingSender : BotSender {
        val views = mutableListOf<ScreenView>()
        val texts = mutableListOf<String>()
        override suspend fun send(chatId: Long, view: ScreenView) {
            views.add(view)
        }
        override suspend fun sendText(chatId: Long, text: String) {
            texts.add(text)
        }
    }

    private val course = scenario("test") {
        startAt("menu")
        screen("menu") {
            text("Меню")
            navigate("Урок", "lesson")
            action("Привет", "greet", "name" to "Иван")
        }
        screen("lesson") {
            text("Урок")
            back()
        }
        screen("quiz") {
            text("Сколько будет 2+2?")
            onText { _, t -> if (t.trim() == "4") InputResult(reply = "Верно!", next = ScreenId("menu")) else null }
        }
        screen("locked") { text("Закрыто") }
        screen("secret") {
            guard("locked") { it.boolAttr("allowed") == true }
            text("Секрет")
        }
        screen("theory") {
            steps(
                items = { listOf("a", "b", "c") },
                render = { item, i, total -> ScreenContent.Text("$item ${i + 1}/$total") },
                onFinish = "menu",
            )
        }
        form("signup", onComplete = "menu", completion = "Спасибо") {
            field("name", "Имя?")
            field("age", "Возраст?", error = "Число!") { it.toIntOrNull() != null }
        }
        action("greet") { if (arg("name") == "Иван") goto("lesson") else stay }
    }

    private fun engine(sender: BotSender, store: InMemorySessionStore) = ScreenEngine(course, store, sender)

    @Test
    fun `start shows start screen`() = runTest {
        val sender = RecordingSender()
        engine(sender, InMemorySessionStore()).handle(IncomingUpdate.Start(chatId = 1))
        assertEquals(ScreenContent.Text("Меню"), sender.views.first().content)
    }

    @Test
    fun `navigate then back returns to menu`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        val e = engine(sender, store)
        e.handle(IncomingUpdate.Start(1))
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Navigate(ScreenId("lesson")))))
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Back)))
        assertEquals(ScreenContent.Text("Меню"), sender.views.last().content)
        assertEquals(ScreenId("menu"), store.load(1)!!.current)
    }

    @Test
    fun `text input is handled by screen`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        store.save(UserSession(1, ScreenId("quiz")))
        engine(sender, store).handle(IncomingUpdate.TextMessage(1, "4"))
        assertTrue(sender.texts.contains("Верно!"))
        assertEquals(ScreenId("menu"), store.load(1)!!.current)
    }

    @Test
    fun `action codec round trips including args`() {
        val actions = listOf(
            ButtonAction.Navigate(ScreenId("x")),
            ButtonAction.Back,
            ButtonAction.Home,
            ButtonAction.Custom("done"),
            ButtonAction.Custom("open", mapOf("lessonId" to "42", "pos" to "2")),
        )
        actions.forEach { assertEquals(it, ActionCodec.decode(ActionCodec.encode(it))) }
    }

    @Test
    fun `custom action receives typed args and navigates`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        val e = engine(sender, store)
        e.handle(IncomingUpdate.Start(1))
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Custom("greet", mapOf("name" to "Иван")))))
        assertEquals(ScreenContent.Text("Урок"), sender.views.last().content)
        assertEquals(ScreenId("lesson"), store.load(1)!!.current)
    }

    @Test
    fun `guard redirects when access denied and allows when granted`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        val e = engine(sender, store)
        e.handle(IncomingUpdate.Start(1))
        // нет прав — перенаправление на locked
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Navigate(ScreenId("secret")))))
        assertEquals(ScreenContent.Text("Закрыто"), sender.views.last().content)
        assertEquals(ScreenId("locked"), store.load(1)!!.current)
        // выдаём права — доступ открыт
        val s = store.load(1)!!.also { it.putAttribute("allowed", true) }
        store.save(s)
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Navigate(ScreenId("secret")))))
        assertEquals(ScreenContent.Text("Секрет"), sender.views.last().content)
    }

    @Test
    fun `stepped screen pages through items and finishes`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        val e = engine(sender, store)
        e.handle(IncomingUpdate.Start(1))
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Navigate(ScreenId("theory")))))
        assertEquals(ScreenContent.Text("a 1/3"), sender.views.last().content)
        val next = ActionCodec.encode(ButtonAction.Custom("theory.__step_next"))
        e.handle(IncomingUpdate.ButtonPressed(1, next))
        assertEquals(ScreenContent.Text("b 2/3"), sender.views.last().content)
        e.handle(IncomingUpdate.ButtonPressed(1, next))
        assertEquals(ScreenContent.Text("c 3/3"), sender.views.last().content)
        e.handle(IncomingUpdate.ButtonPressed(1, next)) // последний шаг → onFinish
        assertEquals(ScreenContent.Text("Меню"), sender.views.last().content)
        assertEquals(ScreenId("menu"), store.load(1)!!.current)
    }

    @Test
    fun `form collects fields with validation and stores values`() = runTest {
        val sender = RecordingSender()
        val store = InMemorySessionStore()
        val e = engine(sender, store)
        e.handle(IncomingUpdate.Start(1))
        e.handle(IncomingUpdate.ButtonPressed(1, ActionCodec.encode(ButtonAction.Navigate(ScreenId("signup")))))
        assertEquals(ScreenContent.Text("Имя?"), sender.views.last().content)
        e.handle(IncomingUpdate.TextMessage(1, "Боб"))
        assertTrue(sender.texts.contains("Возраст?"))
        e.handle(IncomingUpdate.TextMessage(1, "abc")) // невалидно
        assertTrue(sender.texts.contains("Число!"))
        e.handle(IncomingUpdate.TextMessage(1, "30"))
        assertTrue(sender.texts.contains("Спасибо"))
        assertEquals(ScreenId("menu"), store.load(1)!!.current)
        val form = store.load(1)!!.scope("form.signup")
        assertEquals("Боб", form.get("name"))
        assertEquals(30, form.int("age"))
    }
}
