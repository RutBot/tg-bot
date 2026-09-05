package features.tasks.workflow

interface Workflow {
    fun <T> addAction(action: Action<T>): T
    fun showActions(): List<Action>
}

interface Action<T> {
    fun execute(context: Context): T
}

abstract class GeneralActionWithStep<T> : Action<T> {
    abstract fun preAction(context: Context)
    abstract fun doAction(context: Context): T
    abstract fun postAction(context: Context, resultOfAction: T)

    override fun execute(context: Context): T {
        preAction(context)
        return postAction(context, doAction(context))
    }
}

data class Context(
    val properties: WorkflowProperty
    val accumulator: MutableMap<String, Any> // Почему ключ именно строковый?
    var isSuccessful: Boolean = true
)

class WorkflowProperty {
    var timeout: Long = TimeUnit.SECONDS(10).toMillis()
    var retryCount: Int = 5
}

class WorkflowGeneral : Workflow {
    val actions: MutableList<Action<T>> = mutableListOf()
}