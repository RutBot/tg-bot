package сurriculum.schedule.dsl

import сurriculum.schedule.model.Lesson
import сurriculum.schedule.model.LessonSchedule
import сurriculum.schedule.engine.ScheduleRegistry
import сurriculum.schedule.exceptions.ClassroomOverlapException
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

@CurriculumDsl
class ScheduleBuilder(private val registry: ScheduleRegistry, dateString: String) {

    private val lessons = mutableListOf<Lesson>()
    private val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)

    fun lesson(block: LessonBuilder.() -> Unit) {
        val builder = LessonBuilder(registry)
        builder.block()
        val lesson = builder.build(date)
        
        // Проверка на пересечение времени в одной аудитории
        val lessonClassrooms = lesson.classrooms.map { it.number }.toSet()

        val overlappingLesson = lessons.find { existing ->
            val hasCommonClassroom =
                existing.classrooms
                    .map { it.number }
                    .any(lessonClassrooms::contains)

            val hasTimeOverlap = !(existing.startTime>lesson.endTime || existing.endTime<lesson.startTime)

            hasCommonClassroom && hasTimeOverlap
        }

        if (overlappingLesson != null) {
            val lessonClassroomNumbers = lesson.classrooms.map { it.number }.toSet()
            val existingClassroomNumbers = overlappingLesson.classrooms.map { it.number }.toSet()
            val commonClassrooms = existingClassroomNumbers.intersect(lessonClassroomNumbers)
            throw ClassroomOverlapException(
                commonClassrooms.joinToString(),
                lesson.subject,
                overlappingLesson.subject
            )
        }
        
        lessons += lesson
    }

    fun build(): LessonSchedule {
        return LessonSchedule(
            date = date,
            lessons = lessons
        )
    }
}

fun schedule(date: String, overWrite: Boolean = false, block: ScheduleBuilder.() -> Unit): LessonSchedule = runBlocking {
    val registry = ScheduleConfig.getRegistry()
    val notifier = ScheduleConfig.getNotifier()
    val builder = ScheduleBuilder(registry, date)
    builder.block()
    val lessonSchedule = builder.build()
    registry.saveSchedule(lessonSchedule, overWrite)
    notifier.notify(lessonSchedule)
    lessonSchedule
}