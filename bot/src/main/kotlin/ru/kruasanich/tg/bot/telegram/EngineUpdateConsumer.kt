package ru.kruasanich.tg.bot.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.kruasanich.telegram.bot.engine.core.port.IncomingUpdate
import kotlin.coroutines.CoroutineContext

/**
 * Потребитель Telegram long-polling апдейтов (API telegrambots 7.x).
 *
 * Класс намеренно «тонкий»: принимает «сырой» апдейт, мапит его в доменный
 * [IncomingUpdate] и асинхронно передаёт обработчику [processor].
 *
 * Обработчик устанавливается после конструирования ([processor]) — это разрывает
 * циклическую зависимость consumer → engine → sender.
 */
class EngineUpdateConsumer : LongPollingSingleThreadUpdateConsumer, CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default

    /** Обработчик доменных апдейтов; задаётся конфигурацией после сборки графа. */
    var processor: (suspend (IncomingUpdate) -> Unit)? = null

    /** Принять апдейт и асинхронно передать обработчику. */
    override fun consume(update: Update) {
        val incoming = UpdateMapper.map(update) ?: return
        val handler = processor ?: return
        launch { handler(incoming) }
    }
}
