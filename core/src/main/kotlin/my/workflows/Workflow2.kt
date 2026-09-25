package my.workflows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface Workflow2 {
    fun <T> addAction(action: Action2<T>)
    fun <T> showActions(): List<Action2<T>>
}

interface Action2<T> {
    fun execute(context: Context): T
}

abstract class GeneralActionWithStep<T> : Action2<T> {
    abstract fun preAction(context: Context)
    abstract fun doAction(context: Context): T
    abstract fun postAction(context: Context, resultOfAction: T) : T

    override fun execute(context: Context) : T {
        preAction(context)
        return postAction(context, doAction(context))
    }
}

data class Context(
    val properties: WorkflowProperty,
    val accumulator: MutableMap<String, Any>, // Почему ключ именно строковый? Чтобы различать действия по именам
    var isSuccessful: Boolean = true
)

class WorkflowProperty {
    var timeout: Long = 10.seconds.inWholeMilliseconds
    var retryCount: Int = 5
}

class WorkflowGeneral(properties: WorkflowProperty = WorkflowProperty()) : Workflow2 {
    val actions: MutableList<Action2<*>> = mutableListOf()

    val context = Context(properties, mutableMapOf())

    fun start() {
        context.isSuccessful = true
        context.accumulator.clear()
        actions.forEach {
            it.execute(context)
        }
    }

    fun getWorkTime(): Long {
        return actions.sumOf {
            if (it is MyAction<*>) {
                it.executionTime
            } else {
                0
            }
        }
    }

    fun getLaunchCount(): Int {
        return 1 + actions.sumOf {
            if (it is MyAction<*>) {
                it.launchCount - 1
            } else {
                0
            }
        }
    }

    fun swapActions(actionName: String, otherActionName: String){
        val index1 = actions.indexOfFirst { it is MyAction<*> && it.name == actionName }
        val index2 = actions.indexOfFirst { it is MyAction<*> && it.name == otherActionName }

        if (index1 != -1 && index2 != -1) {
            val temp = actions[index1]
            actions[index1] = actions[index2]
            actions[index2] = temp
        }
    }

    override fun <T> addAction(action: Action2<T>) {
        actions.add(action)
    }
    override fun <T> showActions(): List<Action2<T>> {
        return actions as List<Action2<T>>
    }

    fun action(name: String, preActionCode: (suspend () -> Any)? = null, postActionCode: (suspend () -> Any)? = null, actionCode: suspend () -> Any): Any {
        val action = MyAction(name, actionCode, preActionCode, postActionCode)
        return addAction(action)
    }

}

class MyAction<T : Any>(val name: String, val actionCode: suspend () -> T, val preActionCode: (suspend () -> T)? = null, val postActionCode: (suspend () -> T)? = null ) : GeneralActionWithStep<T>() {
    var launchCount = 0
    var executionTime : Long = 0

    override fun preAction(context: Context) {
        if (!context.isSuccessful) {
            throw Exception("Pre-action failed due to unsuccessful context")
        }
        runBlocking { preActionCode?.invoke() }
        println("Action $name ready to execute")
    }

    override fun doAction(context: Context): T {
        return runBlocking {
            launchCount++
            val startTime = System.currentTimeMillis()
            for (i in 1..context.properties.retryCount) {
                try {
                    val result = withTimeout(context.properties.timeout.milliseconds) {
                        actionCode()
                    }
                     executionTime = System.currentTimeMillis() - startTime
                    return@runBlocking result
                } catch (e: TimeoutCancellationException) {
                    println("Action $name timed out: ${e.message}")
                }
            }
            executionTime = System.currentTimeMillis() - startTime
            context.isSuccessful = false
            throw Exception("Action $name failed after retries")
        }
    }

    override fun postAction(context: Context, resultOfAction: T): T {
        runBlocking { postActionCode?.invoke() }
        println("Action $name executed successfully")
        context.accumulator[name] = resultOfAction
        return resultOfAction
    }
}

fun workflow2(properties: WorkflowProperty? = null, code: WorkflowGeneral.() -> Unit): WorkflowGeneral {
    val workflow = if (properties == null) WorkflowGeneral() else WorkflowGeneral(properties)
    workflow.code()
    return workflow
}
