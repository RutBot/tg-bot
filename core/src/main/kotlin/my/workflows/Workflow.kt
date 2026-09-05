package my.workflows

import kotlin.reflect.KType

/**
 * WARNING: Workflow and it's actions support type checking for standard types (Int, String, List, Map, etc.)
 * but does not guarantee type validation for custom types or complex generics
 */
@WorkflowsDsl
class Workflow(val name: String, val executionStrategy: ExecutionStrategy = ExecutionStrategy.ONCE) {
    enum class ExecutionStrategy { ONCE, REPETITIVE }
    enum class WorkflowStatus { SUCCESS, FAILURE, PENDING, RUNNING }

    val actions: MutableList<Action> = mutableListOf()
    var status: WorkflowStatus = WorkflowStatus.PENDING
        private set
    var timeStart : Long? = null
        private set
    var timeEnd: Long? = null
        private set
    var launchCount: Int = 0
        private set

    fun action(name: String, prerequisiteActions: List<Action> = emptyList(), args: List<Any?> = emptyList(), argTypes: List<KType> = emptyList(), resultType: KType, codeBlock: (List<Any?>) -> Any?) : Action {
        val action = Action(name, prerequisiteActions, argTypes, args, resultType, codeBlock)
        actions.add(action)
        return action
    }

    fun start() {
        timeStart = System.currentTimeMillis()
        loop@ while ((status == WorkflowStatus.FAILURE && executionStrategy == ExecutionStrategy.REPETITIVE) || (status == WorkflowStatus.PENDING)) {
            println("${if (status == WorkflowStatus.FAILURE && executionStrategy == ExecutionStrategy.REPETITIVE) "Res" else "S"}tarting workflow $name")
            status = WorkflowStatus.RUNNING
            launchCount++
            actions.forEach {
                it.run()
                if (it.status == Action.ActionStatus.FAILURE) {
                    status = WorkflowStatus.FAILURE
                    println("Workflow $name failed due to action ${it.name} failure")
                    continue@loop
                } else if (it.status == Action.ActionStatus.SUCCESS) {
                    println("Action ${it.name} finished successfully")
                }
            }
            status = WorkflowStatus.SUCCESS
            println("Workflow $name finished successfully")
            }
        timeEnd = System.currentTimeMillis()
    }
}

fun workflow(name: String, code: Workflow.() -> Unit): Workflow {
    val workflow = Workflow(name)

    workflow.code()

    return workflow
}

