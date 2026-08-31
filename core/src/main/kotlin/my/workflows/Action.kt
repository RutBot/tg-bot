package my.workflows

@WorkflowsDsl
class Action(
    val name: String,
    private val execute: () -> Unit
) {
    fun run() {
        println("Running: $name")
        execute()
    }
}