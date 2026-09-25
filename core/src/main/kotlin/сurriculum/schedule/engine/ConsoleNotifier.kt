package сurriculum.schedule.engine

import java.time.format.DateTimeFormatter
import сurriculum.schedule.model.LessonSchedule

class ConsoleNotifier : Notifier {
    override suspend fun notify(schedule: LessonSchedule) {
        val dateStr = schedule.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        schedule.lessons.forEach { lesson ->
            val groupNames = lesson.studentGroups.mapNotNull { it.name }.joinToString(separator = ", ")
            val classrooms = lesson.classrooms.joinToString(separator = ", ") { it.number }
            val classroomsLabel = if (lesson.classrooms.size > 1) CLASSROOMS_MANY else CLASSROOMS_ONE
            val groupsLabel = if (lesson.studentGroups.size > 1) GROUPS_MANY else GROUPS_ONE
            val groupInfo = if (groupNames.isNotBlank()) {
                " $groupsLabel $groupNames"
            } else {
                ""
            }

            val startTimeStr = lesson.startTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
            val messageSuffix = MESSAGE_TEMPLATE.format(
                lesson.subject,
                dateStr,
                classroomsLabel,
                classrooms,
                startTimeStr,
                groupInfo
            )

            // Нотификация преподавателей
            lesson.teachers.forEach { teacher ->
                println(TEACHER_GREETING_TEMPLATE.format(teacher.name, messageSuffix))
            }

            // Нотификация студентов из всех групп
            lesson.studentGroups.flatMap { it.students ?: emptyList() }.distinctBy { it.name }.forEach { student ->
                println(STUDENT_GREETING_TEMPLATE.format(student.name, messageSuffix))
            }
        }
    }

    companion object {
        private const val CLASSROOMS_MANY = "аудиториях"
        private const val CLASSROOMS_ONE = "аудитории"
        private const val GROUPS_MANY = "c группами студентов"
        private const val GROUPS_ONE = "с группой студентов"
        
        private const val MESSAGE_TEMPLATE = "вам поставлен урок по %s на %s в %s %s в %s %s"
        private const val TEACHER_GREETING_TEMPLATE = "Уважаемый %s, %s"
        private const val STUDENT_GREETING_TEMPLATE = "%s, %s"
    }
}
