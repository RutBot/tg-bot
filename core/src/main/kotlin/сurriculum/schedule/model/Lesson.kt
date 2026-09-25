package сurriculum.schedule.model

import java.time.LocalDate
import java.time.LocalTime

data class Lesson (
    val classrooms: List<Classroom>,
    val subject: String,
    val teachers: List<Teacher>,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val studentGroups: List<StudentGroup>
)
