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

@WorkflowsDsl
class Workflow(val name: String) {
    val actions: MutableList<Action> = mutableListOf()

    fun action(name: String, execute: () -> Unit) {
        val action = Action(name, execute)
        actions.add(action)
    }

    fun run() {
        actions.forEach { it.run() }
    }
}

fun workflow(name: String, code: Workflow.() -> Unit): Workflow {
    val workflow = Workflow(name)

    workflow.code()

    return workflow
}

interface FamilyMember
class cat(val name: String) : FamilyMember
class dog(val name: String) : FamilyMember

val zzz : MutableList<Any> = mutableListOf()
val barsik = cat("barsik")
var bobik = dog("bobik")
fun main(){
    zzz.add(barsik)
    zzz.add(bobik)
    println("zzz: $zzz")
    println(zzz.find{ it is cat && it.name == "barsik"} )
    zzz.removeIf { it is dog && it.name == "bobik" }
    println("zzz: $zzz")
}