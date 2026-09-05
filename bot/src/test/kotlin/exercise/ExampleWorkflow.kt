package exercise

import my.workflows.workflow
import my.workflows.Action

import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

class Test {
    @Test
    fun myTest() {
        class Foundation(val square: Int, val material: String)
        class Ruins()

        val myWorkflow = workflow("Building a house") {
            val first = action("Lay foundation", argTypes = listOf(typeOf<String>()), args = listOf("Concrete"), resultType = typeOf<Foundation>()) { args ->
                Ruins()
            }

            action("Build walls", prerequisiteActions = listOf(actions.find { it.name == "Lay foundation"}!!), argTypes = listOf(typeOf<Int>()), args = listOf(10), resultType = typeOf<Int>()) { args ->
                println("Building walls with ${args[0]} bricks")
                10
            }

            action("Notify customer", argTypes = listOf(typeOf<List<Action>>()), args = listOf(actions), resultType = typeOf<() -> String>()) { args ->
                for (action in args[0] as List<Action>) {
                    println("Hey customer we finished ${action.name}")
                }
                { 123 }
            }
        }

        myWorkflow.start()
    }
}