package ru.kruasanich.telegram.bot.engine.core.dsl

/**
 * Одно поле многошаговой формы.
 *
 * @property name ключ, под которым ответ сохраняется в области сессии формы.
 * @property prompt текст-приглашение, показываемый пользователю на этом шаге.
 * @property error сообщение при провале валидации.
 * @property validate опциональная проверка введённого значения.
 */
class FormField(
    val name: String,
    val prompt: String,
    val error: String,
    val validate: (suspend (String) -> Boolean)?,
)

/**
 * Builder многошаговой формы (сбор нескольких полей с валидацией).
 *
 * Поля собираются по очереди: движок показывает приглашение, проверяет ввод,
 * при ошибке повторяет шаг, при успехе переходит к следующему полю. Итог
 * складывается в область сессии `form.<id>` и доступен на экране завершения.
 */
@ScenarioDsl
class FormBuilder {

    internal val fields = mutableListOf<FormField>()

    /**
     * Описать поле формы.
     *
     * @param name ключ сохранения ответа.
     * @param prompt приглашение к вводу.
     * @param error сообщение при неуспешной валидации.
     * @param validate проверка значения (`null` — принимать любой ввод).
     */
    fun field(
        name: String,
        prompt: String,
        error: String = "Некорректное значение, попробуйте ещё раз.",
        validate: (suspend (String) -> Boolean)? = null,
    ) {
        fields += FormField(name, prompt, error, validate)
    }
}
