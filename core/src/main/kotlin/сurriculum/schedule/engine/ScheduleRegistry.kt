package сurriculum.schedule.engine

import java.time.LocalDate
import сurriculum.schedule.model.LessonSchedule
import сurriculum.schedule.model.Student
import сurriculum.schedule.model.StudentGroup
import сurriculum.schedule.model.Teacher
import сurriculum.schedule.model.Classroom

interface ScheduleRegistry {
    /**
     * Returns a teacher with the specified [name].
     *
     * @throws TeacherNotFoundException if no teacher with the given name is found.
     */
    fun getTeacher(name: String): Teacher

    /**
     * Returns a student with the specified [name].
     *
     * @throws IllegalArgumentException if no student with the given name is found.
     */
    fun getStudent(name: String): Student

    /**
     * Returns a student group with the specified [name].
     *
     * @throws IllegalArgumentException if no student group with the given name is found.
     */
    fun getStudentGroup(name: String): StudentGroup

    /**
     * Returns a classroom with the specified [number].
     *
     * @throws IllegalArgumentException if no classroom with the given number is found.
     */
    fun getClassroom(number: String): Classroom

    fun findSchedule(date: LocalDate): LessonSchedule?
    fun saveSchedule(schedule: LessonSchedule, overWrite: Boolean = false)
}