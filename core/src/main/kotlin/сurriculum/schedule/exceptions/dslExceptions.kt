package сurriculum.schedule.exceptions

import java.time.LocalDate

abstract class ScheduleDslException(message: String) : IllegalStateException(message)

class DuplicatedClassroomException : ScheduleDslException("Duplicated Classroom definition. To assign multiple classrooms to a lesson, list them separated by commas.")

class TooManyClassroomsException : ScheduleDslException("Assigning more than 3 classrooms to a lesson is not supported. Please split the lesson into multiple lessons.")

class DuplicatedSubjectException(name: String, existing: String?) : ScheduleDslException("Duplicated Subject definition: $name after $existing")

class DuplicatedTeacherException : ScheduleDslException("Duplicated Teacher definition. To assign multiple teachers to a lesson, list them separated by commas.")

class TimeAlreadySetException : ScheduleDslException("Time is already set")

class InvalidTimeFormatException(timeRange: String) : ScheduleDslException("Invalid time format: $timeRange. Expected format: HH:mm - HH:mm")

class DuplicatedGroupException : ScheduleDslException("Duplicated Group definition. To assign multiple groups to a lesson, list them separated by commas.")

class ClassroomMissingException : ScheduleDslException("Classroom must be specified")

class SubjectMissingException : ScheduleDslException("Subject must be specified")

class TeacherMissingException : ScheduleDslException("Teacher must be specified")

class StartTimeMissingException : ScheduleDslException("Start time must be specified")

class EndTimeMissingException : ScheduleDslException("End time must be specified")

class MultipleGroupsWithIndividualStudentsException : ScheduleDslException("Multiple group names is unsupported when students are specified. Please assign only one group name to the provided students.")

class StudentsMissingException : ScheduleDslException("Student group or students must be specified for a lesson")

class CapacityExceededException(totalStudents: Int, totalCapacity: Int, classroomNumbers: String) : 
    ScheduleDslException("Student count ($totalStudents) exceeds total capacity ($totalCapacity) of classrooms $classroomNumbers. Use ignoreConstraints() to bypass this check.")

class TeacherSpecialityMissingException(teacherName: String, subject: String, specialities: List<String>) : 
    ScheduleDslException("Teacher $teacherName does not have speciality for subject $subject. Known specialities: ${specialities.joinToString()}. Use ignoreConstraints() to bypass this check.")

class TeacherNotFoundException(name: String) : ScheduleDslException("Teacher not found: $name. You can ignore it by using ignoreMissingTeacher() in your lesson definition.")

class StudentNotFoundException(name: String) : ScheduleDslException("Student not found: $name")

class StudentGroupNotFoundException(name: String) : ScheduleDslException("Student group not found: $name")

class ClassroomNotFoundException(number: String) : ScheduleDslException("Classroom not found: $number")

class ClassroomOverlapException(commonClassrooms: String, subject1: String, subject2: String) : 
    ScheduleDslException("Overlap in classroom(s) $commonClassrooms: $subject1 and $subject2")

class ScheduleAlreadyExistsException(date: LocalDate) : ScheduleDslException("Schedule for date $date already exists. Use overWrite = true to overwrite.")

class InvalidGroupException : ScheduleDslException("Group name and students must be specified to save a student group")
