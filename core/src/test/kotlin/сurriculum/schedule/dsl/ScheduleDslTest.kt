package сurriculum.schedule.dsl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import сurriculum.schedule.exceptions.*
import сurriculum.schedule.model.*
import сurriculum.schedule.engine.ConsoleNotifier
import сurriculum.schedule.engine.InMemoryScheduleRegistry

class ScheduleDslTest {

    private lateinit var registry: InMemoryScheduleRegistry
    private val notifier: ConsoleNotifier = ConsoleNotifier()
    private lateinit var student1: Student
    private lateinit var student2: Student

    @BeforeEach
    fun setup() {
        registry = InMemoryScheduleRegistry()
        ScheduleConfig.setRegistry(registry)
        ScheduleConfig.setNotifier(notifier)

        // Setup common data
        registry.addClassroom(Classroom("101", 30))
        registry.addClassroom(Classroom("102", 20))
        registry.addTeacher(Teacher(1, "Иванов", listOf("Математика", "Физика")))
        registry.addTeacher(Teacher(2, "Петров", listOf("Математика")))
        
        student1 = Student(1, "Алиса", 1, "А")
        student2 = Student(2, "Боб", 1, "А")
        registry.addStudentGroup(StudentGroup(1, "Группа А", listOf(student1, student2)))
    }

    // --- Basic Features ---

    @Test
    fun `should create simple schedule with one lesson`() {
        val schedule = schedule("2023-10-23") {
            lesson {
                classroom("101")
                subject("Математика")
                teacher("Иванов")
                time("09:00 - 10:30")
                group("Группа А")
            }
        }

        assertEquals(1, schedule.lessons.size)
        val lesson = schedule.lessons[0]
        assertEquals("Математика", lesson.subject)
        assertEquals("101", lesson.classrooms[0].number)
        assertEquals("Иванов", lesson.teachers[0].name)
    }

    // --- Advanced Features ---

    @Test
    fun `should support multiple classrooms, teachers and groups`() {
        registry.addTeacher(Teacher(3, "Сидоров", listOf("Математика")))
        registry.addClassroom(Classroom("103", 50))
        registry.addStudentGroup(StudentGroup(2, "Группа Б", listOf(student1)))

        val schedule = schedule("2023-10-24") {
            lesson {
                classroom("101", "103")
                subject("Математика")
                teacher("Иванов", "Сидоров")
                time("11:00 - 12:30")
                group("Группа А", "Группа Б")
            }
        }

        val lesson = schedule.lessons[0]
        assertEquals(2, lesson.classrooms.size)
        assertEquals(2, lesson.teachers.size)
        assertEquals(2, lesson.studentGroups.size)
    }

    @Test
    fun `should support ignoreConstraints for capacity and specialities`() {
        // Teacher Petrov doesn't have Physics speciality
        // Capacity of 102 is 20, we will try to put more students if needed (though Group A only has 2)
        
        assertThrows<TeacherSpecialityMissingException> {
            schedule("2023-10-25") {
                lesson {
                    classroom("101")
                    subject("Физика")
                    teacher("Петров") // Petrov only knows Math
                    time("09:00 - 10:00")
                    group("Группа А")
                }
            }
        }

        val schedule = schedule("2023-10-25", overWrite = true) {
            lesson {
                ignoreConstraints()
                classroom("101")
                subject("Физика")
                teacher("Петров")
                time("09:00 - 10:00")
                group("Группа А")
            }
        }
        assertEquals(1, schedule.lessons.size)
    }

    @Test
    fun `should support temporary teacher with ignoreMissingTeacher`() {
        // "Unknown" is not in registry
        assertThrows<TeacherNotFoundException> {
            schedule("2023-10-26") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    teacher("Неизвестный")
                    time("09:00 - 10:00")
                    group("Группа А")
                }
            }
        }

        val schedule = schedule("2023-10-26") {
            lesson {
                ignoreMissingTeacher()
                ignoreConstraints() // Because temporary teacher has no specialities
                classroom("101")
                subject("Математика")
                teacher("Неизвестный")
                time("09:00 - 10:00")
                group("Группа А")
            }
        }
        assertEquals("Неизвестный", schedule.lessons[0].teachers[0].name)
        assertNull(schedule.lessons[0].teachers[0].id)
    }

    // --- Exception Handling ---

    @Test
    fun `should throw error on time overlap in same classroom`() {
        assertThrows<ClassroomOverlapException> {
            schedule("2023-10-27") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    teacher("Иванов")
                    time("09:00 - 10:30")
                    group("Группа А")
                }
                lesson {
                    classroom("101")
                    subject("Физика")
                    teacher("Иванов")
                    time("10:00 - 11:30") // Overlap
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error on duplicated parameter calls`() {
        assertThrows<DuplicatedClassroomException> {
            schedule("2023-10-28") {
                lesson {
                    classroom("101")
                    classroom("102") // Second call
                }
            }
        }
    }

    @Test
    fun `should throw error if more than 3 classrooms are specified`() {
        assertThrows<TooManyClassroomsException> {
            schedule("2023-10-31") {
                lesson {
                    classroom("101", "102", "103", "104")
                }
            }
        }
    }

    @Test
    fun `should throw error if mandatory fields are missing`() {
        assertThrows<TeacherMissingException> {
            schedule("2023-10-29") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    // Missing teacher and time
                }
            }
        }
    }

    @Test
    fun `should throw error if schedule already exists and overWrite is false`() {
        schedule("2023-10-30") {
            lesson {
                classroom("101")
                subject("Математика")
                teacher("Иванов")
                time("09:00 - 10:00")
                group("Группа А")
            }
        }

        assertThrows<ScheduleAlreadyExistsException> {
            schedule("2023-10-30") {
                lesson {
                    classroom("101")
                    subject("Физика")
                    teacher("Иванов")
                    time("11:00 - 12:00")
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error on duplicated subject`() {
        assertThrows<DuplicatedSubjectException> {
            schedule("2023-10-31") {
                lesson {
                    subject("Algebra")
                    subject("Physics")
                }
            }
        }
    }

    @Test
    fun `should throw error on duplicated teacher call`() {
        assertThrows<DuplicatedTeacherException> {
            schedule("2023-10-31") {
                lesson {
                    teacher("Иванов")
                    teacher("Петров")
                }
            }
        }
    }

    @Test
    fun `should throw error on duplicated group call`() {
        assertThrows<DuplicatedGroupException> {
            schedule("2023-10-31") {
                lesson {
                    group("Группа А")
                    group("Группа Б")
                }
            }
        }
    }

    @Test
    fun `should throw error on invalid time format`() {
        assertThrows<InvalidTimeFormatException> {
            schedule("2023-10-31") {
                lesson {
                    time("09:00 10:00") // Missing hyphen
                }
            }
        }
    }

    @Test
    fun `should throw error if classroom is missing`() {
        assertThrows<ClassroomMissingException> {
            schedule("2023-11-01") {
                lesson {
                    subject("Математика")
                    teacher("Иванов")
                    time("09:00 - 10:00")
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error if subject is missing`() {
        assertThrows<SubjectMissingException> {
            schedule("2023-11-01") {
                lesson {
                    classroom("101")
                    teacher("Иванов")
                    time("09:00 - 10:00")
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error if group or students are missing`() {
        assertThrows<StudentsMissingException> {
            schedule("2023-11-01") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    teacher("Иванов")
                    time("09:00 - 10:00")
                }
            }
        }
    }

    @Test
    fun `should throw error if capacity is exceeded`() {
        // Classroom 102 has capacity 20. Let's create a group with 25 students.
        val manyStudents = (1..25).map { Student(it.toLong(), "Student $it", 1, "Б") }
        registry.addStudentGroup(StudentGroup(3, "Большая группа", manyStudents))
        
        assertThrows<CapacityExceededException> {
            schedule("2023-11-02") {
                lesson {
                    classroom("102")
                    subject("Математика")
                    teacher("Иванов")
                    time("09:00 - 10:00")
                    group("Большая группа")
                }
            }
        }
    }

    @Test
    fun `should throw error on duplicated teacher definition`() {
        assertThrows<DuplicatedTeacherException> {
            schedule("2023-11-03") {
                lesson {
                    teacher("Иванов")
                    teacher("Петров")
                }
            }
        }
    }

    @Test
    fun `should throw error on time already set`() {
        assertThrows<TimeAlreadySetException> {
            schedule("2023-11-03") {
                lesson {
                    time("09:00 - 10:00")
                    time("11:00 - 12:00")
                }
            }
        }
    }

    @Test
    fun `should throw error if teacher is missing`() {
        assertThrows<TeacherMissingException> {
            schedule("2023-11-03") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    time("09:00 - 10:00")
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error if start or end time is missing`() {
        // Since time() sets both, we'd need to bypass it or check how LessonBuilder is used.
        // But LessonBuilder.build() checks these.
        assertThrows<StartTimeMissingException> {
            schedule("2023-11-03") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    teacher("Иванов")
                    group("Группа А")
                }
            }
        }
    }

    @Test
    fun `should throw error if classroom not found`() {
        assertThrows<ClassroomNotFoundException> {
            schedule("2020-10-10") {
                lesson {
                    classroom("101")
                    subject("Physics")
                    teacher("John Smith")
                    time("09:00 - 10:00")
                    group("B132")
                }
            }
        }
    }

    @Test
    fun `should throw error if student group not found`() {
        assertThrows<StudentGroupNotFoundException> {
            schedule("2023-11-03") {
                lesson {
                    classroom("101")
                    subject("Математика")
                    teacher("Иванов")
                    time("09:00 - 10:00")
                    group("Несуществующая группа")
                }
            }
        }
    }
}
