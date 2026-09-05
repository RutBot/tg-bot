package сurriculum.schedule.engine

import сurriculum.schedule.model.LessonSchedule

interface Notifier {
    suspend fun notify(schedule: LessonSchedule)
}
