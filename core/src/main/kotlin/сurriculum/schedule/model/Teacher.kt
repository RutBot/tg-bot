package сurriculum.schedule.model

data class Teacher (
    val id: Long? = null,
    val name: String,
    val specialities: List<String>? = null
)