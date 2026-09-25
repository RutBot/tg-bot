package сurriculum.schedule.engine

import сurriculum.schedule.exceptions.*
import java.time.LocalDate
import сurriculum.schedule.model.LessonSchedule
import сurriculum.schedule.model.Student
import сurriculum.schedule.model.StudentGroup
import сurriculum.schedule.model.Teacher
import сurriculum.schedule.model.Classroom
import java.util.concurrent.ConcurrentHashMap

class InMemoryScheduleRegistry : ScheduleRegistry {
    private val teachers = ConcurrentHashMap<String, Teacher>()
    private val students = ConcurrentHashMap<String, Student>()
    private val studentGroups = ConcurrentHashMap<String, StudentGroup>()
    private val classrooms = ConcurrentHashMap<String, Classroom>()
    private val schedules = mutableListOf<LessonSchedule>()

    override fun getTeacher(name: String): Teacher {
        return teachers[name] ?: throw TeacherNotFoundException(name)
    }

    override fun getStudent(name: String): Student {
        return students[name] ?: throw StudentNotFoundException(name)
    }

    override fun getStudentGroup(name: String): StudentGroup {
        return studentGroups[name] ?: throw StudentGroupNotFoundException(name)
    }

    override fun getClassroom(number: String): Classroom {
        return classrooms[number] ?: throw ClassroomNotFoundException(number)
    }

    override fun findSchedule(date: LocalDate): LessonSchedule? {
        return schedules.find { it.date == date }
    }

    override fun saveSchedule(schedule: LessonSchedule, overWrite: Boolean) {
        val existingIndex = schedules.find { it.date == schedule.date }
        if (existingIndex != null && !overWrite) throw ScheduleAlreadyExistsException(schedule.date)
        schedules.remove(existingIndex)
        schedules.add(schedule)
    }

    fun addTeacher(teacher: Teacher) {
        teachers[teacher.name] = teacher
    }

    fun addStudent(student: Student) {
        students[student.name] = student
    }

    fun addStudentGroup(group: StudentGroup) {
        if (group.name.isNullOrBlank() || group.students.isNullOrEmpty()) {
            throw InvalidGroupException()
        }
        studentGroups[group.name] = group
    }

    fun addClassroom(classroom: Classroom) {
        classrooms[classroom.number] = classroom
    }
}
