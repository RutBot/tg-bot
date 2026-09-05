package ru.kruasanich.telegram.bot.engine.core.session

import java.util.concurrent.ConcurrentHashMap

/**
 * Потокобезопасное in-memory хранилище сессий.
 *
 * Подходит для тестов, прототипов и демо. В продакшене используется
 * персистентная реализация (PostgreSQL) из Spring-модуля.
 */
class InMemorySessionStore : SessionStore {

    private val storage = ConcurrentHashMap<Long, UserSession>()

    override suspend fun load(chatId: Long): UserSession? = storage[chatId]

    override suspend fun save(session: UserSession) {
        storage[session.chatId] = session
    }
}
