package bot

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

class DBStateMachine : StateMachine {
    override fun loadUser(user: User) {
        transaction {
            Users.upsert(Users.telegramId) {
                it[Users.telegramId] = user.telegramId
                it[Users.state] = user.state
                it[Users.chatId] = user.chatId
                it[Users.isRegistered] = user.isRegistered
                it[Users.lastInput] = user.lastInput
                user.name?.let { name -> it[Users.name] = name }
                user.surname?.let { surname -> it[Users.surname] = surname }
                user.city?.let { city -> it[Users.city] = city }
            }
        }
    }
    
    override fun getState(user: User): String? {
        val state = transaction {
            Users.select(Users.state)
                .where { Users.telegramId eq user.telegramId }
                .map { it[Users.state] }
                .singleOrNull()
        }
        return state
    }

    override fun setState(user: User, state: String) {
        transaction {
            Users.update({ Users.telegramId eq user.telegramId }) {
                it[Users.state] = state
            }
        }
    }
}