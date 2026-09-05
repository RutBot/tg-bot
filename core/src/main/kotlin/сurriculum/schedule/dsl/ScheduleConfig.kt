package сurriculum.schedule.dsl

import сurriculum.schedule.engine.ScheduleRegistry
import сurriculum.schedule.engine.Notifier
import сurriculum.schedule.engine.ConsoleNotifier
import сurriculum.schedule.engine.InMemoryScheduleRegistry

object ScheduleConfig {
    private val defaultRegistry: ScheduleRegistry = InMemoryScheduleRegistry()
    private val defaultNotifier: Notifier = ConsoleNotifier()
    private var registry: ScheduleRegistry? = null
    private var notifier: Notifier? = null

    fun setRegistry(newRegistry: ScheduleRegistry) {
        registry = newRegistry
    }

    fun setNotifier(newNotifier: Notifier) {
        notifier = newNotifier
    }

    fun getRegistry(): ScheduleRegistry {
        return registry ?: defaultRegistry
    }

    fun getNotifier(): Notifier {
        return notifier ?: defaultNotifier
    }
}
