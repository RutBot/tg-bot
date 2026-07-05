package bot

class CacheStateMachine : StateMachine {
    private val cache = mutableMapOf<Long, User>()

    override fun loadUser(user: User) {
        cache[user.telegramId] = user.copy()
    }

    override fun getState(user: User): String? {
        return cache[user.telegramId]?.state
    }

    override fun setState(user: User, state: String) {
        cache[user.telegramId] = user.copy(state = state)
    }

    fun clearCache() {
        val now = System.currentTimeMillis()
        val timeout = 5 * 60 * 1000
        cache.values.removeIf { user ->
            val lastTime = user.lastMessageTime ?: 0L
            now - lastTime > timeout
        }
    }
}