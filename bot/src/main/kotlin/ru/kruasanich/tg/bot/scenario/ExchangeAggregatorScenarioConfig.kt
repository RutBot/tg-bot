package ru.kruasanich.tg.bot.scenario

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.kruasanich.telegram.bot.engine.core.dsl.scenario
import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent

/**
 * Сценарий бота-агрегатора бирж, созданный на основе описания из файла "text idea".
 */
@Configuration
@Profile("exchange-aggregator")
class ExchangeAggregatorScenarioConfig {

    private companion object {
        const val SCREEN_MAIN = "main"
        const val SCREEN_STOCK_SETTINGS = "stk_set"
        const val SCREEN_AVAILABLE_STOCKS = "stk_av"
        const val SCREEN_USER_STOCKS = "stk_usr"
        const val SCREEN_PAIR_SETTINGS = "pair_set"
        const val SCREEN_ADD_PAIR = "pair_add"
        const val SCREEN_REMOVE_PAIR = "pair_rem"

        const val ACTION_GET_RATES = "rates"
        const val ACTION_ADD_STOCK = "stk_add"
        const val ACTION_REMOVE_STOCK = "stk_rem"
    }

    @Bean
    fun stockAggregatorScenario(): Scenario = scenario("stock-aggregator") {
        startAt("start")

        // Экран 0: Приветствие
        screen("start") {
            text(
                """
                Здравствуйте я бот-аггрегатор. 
                Используя кнопку "Получить курсы" вы можете получить информацию про текущие курсы указанных вами пар на указанных вами биржах. 
                Используя кнопку "настройка бирж" вы можете добавить/удалить биржи, информацию с которых вы хотели бы получать. 
                Используя кнопку "настройка пар" вы можете указать пары, по которым бы хотели получать информацию.
                """.trimIndent()
            )
            navigate("🏠 Главное меню", SCREEN_MAIN)
        }

        // Экран 1: Главное меню
        screen(SCREEN_MAIN) {
            text("Главное меню")
            
            action("Получить курсы", ACTION_GET_RATES)
            navigate("⚙️ Настройка бирж", SCREEN_STOCK_SETTINGS)
            navigate("⚙️ Настройка пар", SCREEN_PAIR_SETTINGS)
        }

        // Экран 2: Настройка бирж
        screen(SCREEN_STOCK_SETTINGS) {
            content {
                ScreenContent.Text("Текущие подключенные биржи: [Биржа1, Биржа2...]")
            }
            navigate("➕ Добавить биржу", SCREEN_AVAILABLE_STOCKS)
            navigate("❌ Удалить биржу", SCREEN_USER_STOCKS)
            back("⬅️ Назад")
        }

        // Экран 3: Список доступных бирж
        screen(SCREEN_AVAILABLE_STOCKS) {
            text("Список поддерживаемых ботом бирж:")
            
            // Кнопки бирж
            action("Binance", ACTION_ADD_STOCK)
            action("Bybit", ACTION_ADD_STOCK)
            action("Bitfinex", ACTION_ADD_STOCK)
            
            back("⬅️ Назад")
        }

        // Экран 4: Список бирж пользователя
        screen(SCREEN_USER_STOCKS) {
            text("Ваши биржи (нажмите, чтобы удалить):")
            
            // Кнопки удаления
            action("Binance 🗑", ACTION_REMOVE_STOCK)
            action("Bybit 🗑", ACTION_REMOVE_STOCK)
            
            back("⬅️ Назад")
        }

        // Экран 5: Настройка пар
        screen(SCREEN_PAIR_SETTINGS) {
            content {
                ScreenContent.Text("Текущие подключенные пары: [BTC-USDT, ETH-USDT...]")
            }
            navigate("➕ Добавить пару", SCREEN_ADD_PAIR)
            navigate("❌ Удалить пару", SCREEN_REMOVE_PAIR)
            back("⬅️ Назад")
        }

        // Экран 6: Добавление пары
        screen(SCREEN_ADD_PAIR) {
            text("Введите пару, которую хотели бы добавить через тире английскими буквами, формат: cur1-cur2")
            
            onText { _, text ->
                if (text.contains("-")) {
                    InputResult(reply = "Пара $text успешно добавлена! ✅")
                } else {
                    InputResult(reply = "Чтобы добавить пару, введите пару, которую хотели бы добавить через тире английскими буквами, формат: cur1-cur2")
                }
            }
            back("⬅️ Назад")
        }

        // Экран 7: Удаление пары
        screen(SCREEN_REMOVE_PAIR) {
            text("Введите пару, которую хотели бы удалить через тире английскими буквами, формат: cur1-cur2")
            
            onText { _, text ->
                if (text.contains("-")) {
                    InputResult(reply = "Пара $text успешно удалена! 🗑")
                } else {
                    InputResult(reply = "Чтобы удалить пару, введите пару, которую хотели бы удалить через тире английскими буквами, формат: cur1-cur2")
                }
            }
            back("⬅️ Назад")
        }

        // --- Действия (Actions) ---

        action(ACTION_GET_RATES) {
            stay 
        }

        action(ACTION_ADD_STOCK) {
            stay
        }

        action(ACTION_REMOVE_STOCK) {
            stay
        }
    }
}
