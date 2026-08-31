package my.workflows



@WorkflowsDsl
class Workflow(val name: String) {
    val actions: MutableList<Action> = mutableListOf()

    fun action(name: String, execute: () -> Unit) {
        val action = Action(name, execute)
        actions.add(action)
    }

    fun run() {
        println("Starting workflow $name")
        actions.forEach {
            try {
                it.run()
                println("Finished action: ${it.name}")
            } catch (e: Exception) {
                println("Failed to execute action :${it.name}")
                throw e
            }
        }
        println("Workflow $name finished successfully")
    }
}

fun workflow(name: String, code: Workflow.() -> Unit): Workflow {
    val workflow = Workflow(name)

    workflow.code()

    return workflow
}

