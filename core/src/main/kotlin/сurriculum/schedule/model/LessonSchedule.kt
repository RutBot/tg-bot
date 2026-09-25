package сurriculum.schedule.model

import java.time.LocalDate

data class LessonSchedule (
    val date: LocalDate,
    val lessons: List<Lesson>
)