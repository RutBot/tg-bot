package ru.kruasanich.telegram.bot.engine.core.session

/**
 * Хранилище сессий пользователей.
 *
 * Абстрагирует движок от конкретного бэкенда (PostgreSQL, Redis, in-memory).
 * Интерфейс асинхронный, чтобы не блокировать обработку апдейтов на I/O.
 */
interface SessionStore {

    /**
     * Загрузить сессию пользователя.
     *
     * @param chatId идентификатор чата.
     * @return сессия или `null`, если пользователь ещё не начинал диалог.
     */
    suspend fun load(chatId: Long): UserSession?

    /**
     * Сохранить (создать или обновить) сессию.
     *
     * @param session сессия для сохранения.
     */
    suspend fun save(session: UserSession)
}
