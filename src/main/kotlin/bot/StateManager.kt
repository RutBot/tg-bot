package bot
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface BotState {
    val name: String
    val text: String
    val buttons: List<List<KeyboardButton>>
    val data: Map<String, Any?>?
}

object StateRegistry {
    private val states = mutableMapOf<String, BotState>()

    fun register(state: BotState) {
        states[state.name] = state
    }

    fun get(name: String?): BotState? = states[name]
}

@Serializable
data class User(
    val telegramId: Long,
    val chatId: Long,
    val name: String?,
    val surname: String?,
    val city: String?,
    val isRegistered: Boolean,
    val lastInput: String,
    val state: String,
    val lastMessageTime: Long? = null
)

object Users : Table() {
    val id = long("id").autoIncrement()
    val telegramId = long("telegramId").uniqueIndex()
    val chatId = long("chatId").nullable()
    val name = varchar("name", 255).default("")
    val surname = varchar("surname", 255).default("")
    val state = varchar("state", 255).nullable()
    val lastInput = varchar("lastInput", 255).default("")
    val city = varchar("city", 255).default("")
    val isRegistered = bool("isRegistered").default(false)

    override val primaryKey = PrimaryKey(id)
}

object StateManager : KoinComponent {
    private val bot: Bot by inject()

    fun setState(chatId: Long, userId: Long, state: BotState) {
        transaction {
            val userExists = Users.select(Users.id).where { Users.telegramId eq userId }.singleOrNull() != null
            if (userExists) {
                Users.update({ Users.telegramId eq userId }) {
                    it[Users.state] = state.name
                    it[Users.chatId] = chatId
                }
            } else {
                Users.insert {
                    it[Users.telegramId] = userId
                    it[Users.state] = state.name
                    it[Users.chatId] = chatId
                }
            }
        }
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = state.text,
            replyMarkup = KeyboardReplyMarkup(
                keyboard = state.buttons,
                resizeKeyboard = true
            )
        )
    }

    fun updateLastInput(userId: Long, input: String) {
        transaction {
            Users.update({ Users.telegramId eq userId }) {
                it[Users.lastInput] = input
            }
        }
    }

    fun getLastInput(userId: Long): String? {
        return transaction {
            Users.select(Users.lastInput)
                .where { Users.telegramId eq userId }
                .map { it[Users.lastInput] }
                .singleOrNull()
        }
    }

    fun saveUserData(userId: Long, field: String, value: String) {
        transaction {
            Users.update({ Users.telegramId eq userId }) {
                when (field) {
                    "name" -> it[Users.name] = value
                    "surname" -> it[Users.surname] = value
                    "city" -> it[Users.city] = value
                }
            }
        }
    }

    fun getUserData(userId: Long): ResultRow? {
        return transaction {
            Users.select(Users.name, Users.surname, Users.city)
                .where { Users.telegramId eq userId }
                .singleOrNull()
        }
    }

    fun deleteAccount(userId: Long) {
        transaction {
            Users.update({ Users.telegramId eq userId }) {
                it[Users.name] = ""
                it[Users.surname] = ""
                it[Users.city] = ""
                it[Users.state] = null
                it[Users.isRegistered] = false
            }
        }
    }

    fun finishRegistration(userId: Long) {
        transaction {
            Users.update({ Users.telegramId eq userId }) {
                it[Users.isRegistered] = true
            }
        }
    }

    fun sendConfirmation(chatId: Long, userId: Long, text: String, callbackData: String) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            replyMarkup = InlineKeyboardMarkup.create(
                listOf(
                    listOf(InlineKeyboardButton.CallbackData(text = "Подтвердить ✅", callbackData = callbackData))
                )
            )
        )
    }

    fun getState(userId: Long): BotState? {
        val stateName = transaction {
            Users.select(Users.state)
                .where { Users.telegramId eq userId }
                .map { it[Users.state] }
                .singleOrNull()
        }
        return StateRegistry.get(stateName)
    }
}
