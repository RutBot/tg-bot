package my.workflows

import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * WARNING: Workflow and it's actions support type checking for standard types (Int, String, List, Map, etc.)
 * but does not guarantee type validation for custom types or complex generics
 */
@WorkflowsDsl
class Action(val name: String, private val prerequisiteActions: List<Action> = emptyList(), private val argTypes :List<KType> = emptyList(), private val args: List<Any?> = emptyList(), private val resultType: KType, private val codeBlock: (List<Any?>) -> Any?) {

    enum class ActionStatus { SUCCESS, FAILURE, PENDING, RUNNING }
    var timeStart : Long? = null
        private set
    var timeEnd: Long? = null
        private set
    var actionResult : Any? = null
        private set
    var launchCount: Int = 0
        private set
    var status: ActionStatus = ActionStatus.PENDING
        private set

    private fun isSameStandardType(value: Any?, expectedType: KType): Boolean {
        if (value == null) {
            return expectedType.isMarkedNullable
        }
        val expectedKClass = expectedType.classifier as? KClass<*> ?: return false
        if (!expectedKClass.isInstance(value)) {
            println("Expected type: ${expectedKClass.simpleName}, but got: ${value::class.simpleName}")
            return false
        }
        if (value is Collection<*>) {
            val elementType = expectedType.arguments.firstOrNull()?.type
                ?: return true

            if (!value.all { element -> isSameStandardType(element, elementType) }) {
                println("Collection element type mismatch")
                return false
            }
        }
        if (value is Map<*, *>) {
            val keyType = expectedType.arguments.getOrNull(0)?.type
            val valueType = expectedType.arguments.getOrNull(1)?.type

            if (keyType != null && !value.keys.all { key -> isSameStandardType(key, keyType) }) {
                println("Map key type mismatch")
                return false
            }
            if (valueType != null && !value.values.all { v -> isSameStandardType(v, valueType) }) {
                println("Map value type mismatch")
                return false
            }
        }
        if (value is Pair<*, *>) {
            val firstType = expectedType.arguments.getOrNull(0)?.type
            val secondType = expectedType.arguments.getOrNull(1)?.type

            if (firstType != null && !isSameStandardType(value.first, firstType)) {
                println("Pair first type mismatch")
                return false
            }
            if (secondType != null && !isSameStandardType(value.second, secondType)) {
                println("Pair second type mismatch")
                return false
            }
        }
        return true
    }
    private fun validate() : Boolean {
        for (i in prerequisiteActions.indices) {
            if (prerequisiteActions[i].status != ActionStatus.SUCCESS) {
                println("Prerequisite action ${prerequisiteActions[i].name} for action $name is not successful. Action $name cannot be executed.")
                return false
            }
        }

        if (argTypes.size != args.size) {
            println("Action $name expects ${argTypes.size} arguments, but got ${args.size}")
            return false
        }
        for (i in args.indices) {
            if (!isSameStandardType(args[i], argTypes[i])) {
                println("Action $name argument type mismatch at index $i")
                return false
            }
        }
        return true
    }
    private fun setResult() : Boolean {
        if (isSameStandardType(actionResult, resultType)) {
            return true
        }
        println("Action $name result type mismatch")
        return false
    }
    fun run() : ActionStatus {
        status = ActionStatus.RUNNING
        launchCount++
        timeStart = System.currentTimeMillis()
        if (!validate()) {
            status = ActionStatus.FAILURE
            return status
        }
        println("Action $name validated")

        try {
            actionResult = codeBlock(args)
        } catch (e: Exception) {
            println("Error occurred while running action $name: ${e.message}")
            status = ActionStatus.FAILURE
            return status
        }
        println("Action $name executed")

        if(!setResult()) {
            status = ActionStatus.FAILURE
            return status
        }
        println("Action $name result set successfully")

        timeEnd = System.currentTimeMillis()
        status = ActionStatus.SUCCESS
        return status
    }
}