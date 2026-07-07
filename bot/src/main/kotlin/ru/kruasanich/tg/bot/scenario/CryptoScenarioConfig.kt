package ru.kruasanich.tg.bot.scenario

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.kruasanich.telegram.bot.engine.core.dsl.scenario
import ru.kruasanich.telegram.bot.engine.core.model.Button
import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.InputResult
import ru.kruasanich.telegram.bot.engine.core.model.Scenario
import ru.kruasanich.telegram.bot.engine.core.model.ScreenContent
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId
import ru.kruasanich.telegram.bot.engine.core.session.scope

/**
 * Сценарий крипто-бота, описанный декларативно через DSL движка.
 *
 * Флоу:
 *  1. `/start` — информация о проекте и о работе с криптоинструментами;
 *     две кнопки: «Подробнее» и «Зарегистрироваться».
 *  2. «Подробнее» — краткая справка про крипторынок и торги + «Зарегистрироваться».
 *  3. Регистрация — пошагово: Имя → Фамилия → Возраст → Пол; на каждом шаге
 *     кнопка «Далее ➡️». Порядок шагов гарантируют декларативные guard'ы, данные
 *     копятся в области сессии `reg`.
 *  4. После регистрации — главный экран с меню (крипторынок, профиль, о проекте).
 */
@Configuration
class CryptoScenarioConfig {

    private companion object {
        /** Область сессии, в которой копятся данные регистрации. */
        const val REG = "reg"
        const val FIELD_NAME = "name"
        const val FIELD_SURNAME = "surname"
        const val FIELD_AGE = "age"
        const val FIELD_GENDER = "gender"

        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"

        const val SCREEN_MAIN = "main"
        const val SET_GENDER = "set_gender"
        const val ARG_VALUE = "v"
    }

    /** Сценарий крипто-бота — единственное, что нужно описать для нового бота. */
    @Bean
    fun cryptoScenario(): Scenario = scenario("crypto-bot") {
        startAt("start")

        // 1. Приветствие: информация о проекте/боте.
        screen("start") {
            text(
                """
                *CryptoStart* 🪙

                Привет! Это бот для тех, кто начинает *работу с криптоинструментами*:
                биржи, кошельки, спот и деривативы, базовые стратегии и управление риском.

                Здесь вы познакомитесь с рынком и заведёте профиль, чтобы получать
                подобранные материалы и сигналы.

                Выберите, что дальше:
                """.trimIndent(),
            )
            navigate("🔎 Подробнее", "details")
            navigate("📝 Зарегистрироваться", "reg_name")
        }

        // 2. Подробнее: немного про крипторынок и торги.
        screen("details") {
            text(
                """
                *Крипторынок и торги* 📈

                • Рынок работает 24/7, без выходных — цены двигаются постоянно.
                • Основные инструменты: *спот* (покупка актива) и *деривативы*
                  (фьючерсы, опционы) с плечом и повышенным риском.
                • Ликвидность и волатильность у монет разные: BTC/ETH спокойнее,
                  альткоины — резче.
                • Главное правило новичка: риск-менеджмент и позиции, которые не жалко.

                Готовы попробовать? Зарегистрируйтесь — и перейдём к меню.
                """.trimIndent(),
            )
            navigate("📝 Зарегистрироваться", "reg_name")
            back("⬅️ Назад")
        }

        // 3. Регистрация: Имя → Фамилия → Возраст → Пол (на каждом шаге «Далее»).

        // Шаг 1/4 — Имя. Вход на экран начинает регистрацию заново.
        screen("reg_name") {
            onEnter { session -> session.scope(REG).clear() }
            content { session ->
                val current = session.scope(REG).get(FIELD_NAME)
                ScreenContent.Text(step(1, "Введите ваше *имя*:", current))
            }
            onText { session, text ->
                val name = text.trim()
                if (name.isBlank()) {
                    InputResult(reply = "Имя не должно быть пустым. Введите ещё раз.")
                } else {
                    session.scope(REG).put(FIELD_NAME, name)
                    InputResult(reply = "Имя принято ✅. Нажмите «Далее ➡️».")
                }
            }
            navigate("Далее ➡️", "reg_surname")
            back("⬅️ Назад")
        }

        // Шаг 2/4 — Фамилия. Guard не пускает дальше без имени.
        screen("reg_surname") {
            guard("reg_name") { it.scope(REG).get(FIELD_NAME)?.isNotBlank() == true }
            content { session ->
                val current = session.scope(REG).get(FIELD_SURNAME)
                ScreenContent.Text(step(2, "Введите вашу *фамилию*:", current))
            }
            onText { session, text ->
                val surname = text.trim()
                if (surname.isBlank()) {
                    InputResult(reply = "Фамилия не должна быть пустой. Введите ещё раз.")
                } else {
                    session.scope(REG).put(FIELD_SURNAME, surname)
                    InputResult(reply = "Фамилия принята ✅. Нажмите «Далее ➡️».")
                }
            }
            navigate("Далее ➡️", "reg_age")
            back("⬅️ Назад")
        }

        // Шаг 3/4 — Возраст. Guard требует заполненной фамилии.
        screen("reg_age") {
            guard("reg_surname") { it.scope(REG).get(FIELD_SURNAME)?.isNotBlank() == true }
            content { session ->
                val current = session.scope(REG).int(FIELD_AGE)?.toString()
                ScreenContent.Text(step(3, "Введите ваш *возраст* (число):", current))
            }
            onText { session, text ->
                val age = text.trim().toIntOrNull()
                if (age == null || age !in 1..120) {
                    InputResult(reply = "Возраст — целое число от 1 до 120. Попробуйте ещё раз.")
                } else {
                    session.scope(REG).put(FIELD_AGE, age)
                    InputResult(reply = "Возраст принят ✅. Нажмите «Далее ➡️».")
                }
            }
            navigate("Далее ➡️", "reg_gender")
            back("⬅️ Назад")
        }

        // Шаг 4/4 — Пол. Выбор кнопками + «Далее» для завершения.
        screen("reg_gender") {
            guard("reg_age") { it.scope(REG).int(FIELD_AGE) != null }
            content { session ->
                val current = genderLabel(session.scope(REG).get(FIELD_GENDER))
                ScreenContent.Text(step(4, "Выберите ваш *пол* и нажмите «Далее ➡️»:", current))
            }
            keyboard { session ->
                val selected = session.scope(REG).get(FIELD_GENDER)
                fun mark(label: String, code: String) = if (selected == code) "✅ $label" else label
                listOf(
                    listOf(
                        Button(mark("👨 Мужской", GENDER_MALE), ButtonAction.Custom(SET_GENDER, mapOf(ARG_VALUE to GENDER_MALE))),
                        Button(mark("👩 Женский", GENDER_FEMALE), ButtonAction.Custom(SET_GENDER, mapOf(ARG_VALUE to GENDER_FEMALE))),
                    ),
                    listOf(Button("Далее ➡️", ButtonAction.Navigate(ScreenId(SCREEN_MAIN)))),
                    listOf(Button("⬅️ Назад", ButtonAction.Back)),
                )
            }
        }

        // 4. Главный экран с меню (доступен только после регистрации).
        screen(SCREEN_MAIN) {
            guard("reg_gender") { it.scope(REG).get(FIELD_GENDER)?.isNotBlank() == true }
            content { session ->
                val reg = session.scope(REG)
                val name = reg.get(FIELD_NAME).orEmpty()
                val surname = reg.get(FIELD_SURNAME).orEmpty()
                ScreenContent.Text(
                    """
                    *Готово, $name $surname!* 🎉

                    Регистрация завершена. Это ваше главное меню — выбирайте раздел:
                    """.trimIndent(),
                )
            }
            navigate("💹 Крипторынок", "market")
            navigate("👤 Мой профиль", "profile")
            navigate("ℹ️ О проекте", "about")
        }

        screen("market") {
            text(
                """
                *Крипторынок* 💹

                • *Спот* — покупаете актив и держите его в кошельке.
                • *Фьючерсы* — торговля с плечом; прибыль и убыток кратно растут.
                • *Стейкинг* — доход за блокировку монет в сети.

                Совет: начните со спота на BTC/ETH и небольших сумм.
                """.trimIndent(),
            )
            navigate("🏠 Меню", SCREEN_MAIN)
            back("⬅️ Назад")
        }

        screen("profile") {
            content { session ->
                val reg = session.scope(REG)
                ScreenContent.Text(
                    """
                    *Мой профиль* 👤

                    • Имя: ${reg.get(FIELD_NAME).orEmpty()}
                    • Фамилия: ${reg.get(FIELD_SURNAME).orEmpty()}
                    • Возраст: ${reg.int(FIELD_AGE) ?: "—"}
                    • Пол: ${genderLabel(reg.get(FIELD_GENDER)) ?: "—"}
                    """.trimIndent(),
                )
            }
            navigate("🏠 Меню", SCREEN_MAIN)
            back("⬅️ Назад")
        }

        screen("about") {
            text(
                """
                *О проекте* ℹ️

                CryptoStart — учебный бот-навигатор по криптоинструментам.
                Помогает разобраться в рынке, торгах и управлении риском.

                Дисклеймер: материалы носят образовательный характер и не являются
                индивидуальной инвестиционной рекомендацией.
                """.trimIndent(),
            )
            navigate("🏠 Меню", SCREEN_MAIN)
            back("⬅️ Назад")
        }

        // Действие выбора пола: сохранить и перерисовать шаг с отметкой выбора.
        action(SET_GENDER) {
            scope(REG).put(FIELD_GENDER, requireArg(ARG_VALUE))
            stay
        }
    }

    /** Единый формат приглашения шага регистрации с показом уже введённого значения. */
    private fun step(number: Int, prompt: String, current: String?): String {
        val progress = "*Регистрация — шаг $number/4*"
        val entered = current?.takeIf { it.isNotBlank() }?.let { "\n\nТекущее значение: _${it}_" } ?: ""
        return "$progress\n\n$prompt$entered"
    }

    /** Человекочитаемая подпись пола по коду (`male`/`female`). */
    private fun genderLabel(code: String?): String? = when (code) {
        GENDER_MALE -> "Мужской"
        GENDER_FEMALE -> "Женский"
        else -> null
    }
}
