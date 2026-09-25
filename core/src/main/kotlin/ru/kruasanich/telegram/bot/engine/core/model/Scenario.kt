package ru.kruasanich.telegram.bot.engine.core.model

/**
 * Сценарий бота — корневая структура движка.
 *
 * Сценарий полностью описывает поведение конкретного бота: набор экранов
 * и точку входа. Один и тот же движок исполняет любой сценарий, поэтому
 * создание нового бота сводится к описанию нового [Scenario] через DSL.
 *
 * @property id логическое имя сценария (используется в метриках и логах).
 * @property start экран, показываемый при старте (`/start`).
 * @property screens реестр всех экранов сценария по их идентификаторам.
 * @property actions реестр обработчиков кнопочных действий (DSL `action { }`,
 *  а также служебные действия пошаговых экранов).
 */
class Scenario(
    val id: String,
    val start: ScreenId,
    val screens: Map<ScreenId, Screen>,
    val actions: Map<String, ActionHandler> = emptyMap(),
) {
    init {
        require(screens.containsKey(start)) {
            "Start screen '$start' is not registered in scenario '$id'"
        }
    }

    /**
     * Найти экран по идентификатору.
     *
     * @param id идентификатор искомого экрана.
     * @return экран или `null`, если такого экрана в сценарии нет.
     */
    fun screen(id: ScreenId): Screen? = screens[id]

    /**
     * Найти обработчик действия по имени.
     *
     * @param name имя действия.
     * @return обработчик или `null`, если в сценарии нет такого действия.
     */
    fun action(name: String): ActionHandler? = actions[name]
}
