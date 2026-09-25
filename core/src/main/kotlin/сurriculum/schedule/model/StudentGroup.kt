package сurriculum.schedule.model

data class StudentGroup (
    val id: Long?,
    val name: String? = null,
    val students: List<Student>? = null
) {
    constructor(students: List<Student>) : this(id=null, name=null, students)
}
