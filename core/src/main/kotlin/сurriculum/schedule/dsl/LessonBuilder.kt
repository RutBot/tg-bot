package сurriculum.schedule.dsl

import сurriculum.schedule.engine.ScheduleRegistry
import сurriculum.schedule.exceptions.*
import сurriculum.schedule.model.Lesson
import сurriculum.schedule.model.Student
import сurriculum.schedule.model.StudentGroup
import сurriculum.schedule.model.Teacher
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@CurriculumDsl
class LessonBuilder(private val registry: ScheduleRegistry) {
    private var classrooms: MutableList<String> = mutableListOf()
    private var subject: String? = null
    private var teachers: MutableList<String> = mutableListOf()
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null
    private var students: List<Student>? = null
    private var groupNames: MutableList<String> = mutableListOf()
    private var ignoreConstraints: Boolean = false
    private var ignoreMissingTeacher: Boolean = false

    fun ignoreConstraints() {
        ignoreConstraints = true
    }

    fun ignoreMissingTeacher() {
        ignoreMissingTeacher = true
    }

    fun classroom(vararg numbers: String) {
        if (classrooms.isNotEmpty()) throw DuplicatedClassroomException()
        if (numbers.size > 3) throw TooManyClassroomsException()
        classrooms.addAll(numbers.toList())
    }

    fun subject(name: String) {
        if (subject != null) throw DuplicatedSubjectException(name, subject)
        subject = name
    }

    fun teacher(vararg names: String) {
        if (teachers.isNotEmpty()) throw DuplicatedTeacherException()
        teachers.addAll(names.toList())
    }

    fun time(timeRange: String) {
        if (startTime != null) throw TimeAlreadySetException()
        val parts = timeRange.split("-").map { it.trim() }
        if (parts.size != 2) throw InvalidTimeFormatException(timeRange)
        startTime = LocalTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_TIME)
        endTime = LocalTime.parse(parts[1], DateTimeFormatter.ISO_LOCAL_TIME)
    }

    fun group(vararg names: String, block: (LessonBuilder.() -> Unit)? = null) {
        if (groupNames.isNotEmpty()) throw DuplicatedGroupException()
        groupNames.addAll(names.toList())
        block?.invoke(this)
    }

    fun group(block: LessonBuilder.() -> Unit) {
        block()
    }

    fun students(vararg names: String) {
        students = names.map { registry.getStudent(it) }
    }

    fun build(date: LocalDate): Lesson {
        if (classrooms.isEmpty()) throw ClassroomMissingException()
        val subjectVal = subject ?: throw SubjectMissingException()
        if (teachers.isEmpty()) throw TeacherMissingException()
        val start = startTime ?: throw StartTimeMissingException()
        val end = endTime ?: throw EndTimeMissingException()

        if (!students.isNullOrEmpty() && groupNames.size > 1) throw MultipleGroupsWithIndividualStudentsException()

        val teacherObjs = teachers.map { name ->
            try {
                registry.getTeacher(name)
            } catch (e: TeacherNotFoundException) {
                if (ignoreMissingTeacher) {
                    Teacher(name = name)
                } else {
                    throw e
                }
            }
        }
        val classroomObjs = classrooms.map { registry.getClassroom(it) }
        
        val studentGroups = when {
            !students.isNullOrEmpty() -> listOf(StudentGroup(null, groupNames.firstOrNull(), students))
            groupNames.isNotEmpty() -> groupNames.map { registry.getStudentGroup(it) }
            else -> throw StudentsMissingException()
        }

        if (!ignoreConstraints) {
            val totalStudents = studentGroups.sumOf { it.students?.size ?: 0 }
            val totalCapacity = classroomObjs.sumOf { it.studentCapacity }
            if (totalStudents > totalCapacity) {
                val classroomNumbers = classroomObjs.joinToString(", ") { it.number.toString() }
                throw CapacityExceededException(totalStudents, totalCapacity, classroomNumbers)
            }

            teacherObjs.forEach { teacher ->
                val specialities = teacher.specialities ?: emptyList()
                if (subjectVal !in specialities) {
                    throw TeacherSpecialityMissingException(teacher.name, subjectVal, specialities)
                }
            }
        }

        return Lesson(
            classrooms = classroomObjs,
            subject = subjectVal,
            teachers = teacherObjs,
            date = date,
            startTime = start,
            endTime = end,
            studentGroups = studentGroups
        )
    }
}
