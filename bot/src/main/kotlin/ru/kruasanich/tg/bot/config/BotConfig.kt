package ru.kruasanich.tg.bot.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.kruasanich.telegram.bot.engine.core.engine.ScreenEngine
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.port.BotSender
import ru.kruasanich.telegram.bot.engine.core.session.InMemorySessionStore
import ru.kruasanich.telegram.bot.engine.core.session.SessionStore
import ru.kruasanich.tg.bot.telegram.EngineUpdateConsumer
import ru.kruasanich.tg.bot.telegram.TelegramBotSender

/**
 * Сборка графа движка из бинов приложения.
 *
 * Приложению достаточно предоставить бин [Scenario] (см. `scenario` пакета),
 * всё остальное — транспорт, хранилище сессий, движок и запуск — собирается здесь.
 *
 * Хранилище сессий — in-memory: боту-витрине не нужна БД, а состояние диалога
 * живёт в памяти процесса. Для продакшена бин [sessionStore] заменяется на
 * персистентную реализацию (PostgreSQL/Redis) без изменения остального графа.
 */
@Configuration
@EnableConfigurationProperties(BotProperties::class)
class BotConfig {

    /** In-memory хранилище сессий пользователей. */
    @Bean
    fun sessionStore(): SessionStore = InMemorySessionStore()

    /** Низкоуровневый Telegram-клиент для отправки методов. */
    @Bean
    fun telegramClient(properties: BotProperties): TelegramClient =
        OkHttpTelegramClient(properties.token)

    /** Порт отправки сообщений поверх [TelegramClient]. */
    @Bean
    fun botSender(client: TelegramClient): BotSender = TelegramBotSender(client)

    /** Потребитель Telegram long-polling апдейтов. */
    @Bean
    fun engineUpdateConsumer(): EngineUpdateConsumer = EngineUpdateConsumer()

    /** Конечный автомат движка, исполняющий сценарий. */
    @Bean
    fun screenEngine(
        scenario: Scenario,
        sessionStore: SessionStore,
        sender: BotSender,
    ): ScreenEngine = ScreenEngine(scenario, sessionStore, sender)

    /** Запуск бота: связывает движок с транспортом и регистрирует его в Telegram. */
    @Bean
    fun botRunner(
        consumer: EngineUpdateConsumer,
        engine: ScreenEngine,
        properties: BotProperties,
    ): BotRunner = BotRunner(consumer, engine, properties)
}
