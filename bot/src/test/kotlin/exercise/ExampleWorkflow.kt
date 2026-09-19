package exercise

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.subscribe
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import my.workflows.WorkflowProperty
import my.workflows.workflow2
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExampleWorkflowTest {

    class TestDatabase {
        interface OrderStream {
            fun takeIncomeOrderStream() : Flow<TestDatabase.Sale>
        }

        class MyOrderStream : OrderStream {
            override fun takeIncomeOrderStream(): Flow<TestDatabase.Sale> {
                return flow {
                    var incId = 100
                    while (true) {
                        emit(TestDatabase.Sale(incId++,(1..20).random(), (1..80).random(), LocalDateTime.now().toString()))
                        delay(2.seconds)
                    }
                }
            }
        }
        data class Sale(
            val id: Int,
            val productId: Int,
            val buyerId: Int,
            val date: String
        )

        data class Product(
            val id: Int,
            val categoryId: Int,
            val name: String,
            val cost: Double
        )

        data class Category(
            val id: Int,
            val name: String
        )

        val sales = readCsv("sales.csv").map { (id, productId, buyerId, date) ->
            Sale(id.toInt(), productId.toInt(), buyerId.toInt(), date)
        }

        val products = readCsv("products.csv").map { (id, categoryId, name, cost) ->
            Product(id.toInt(), categoryId.toInt(), name, cost.toDouble())
        }

        val categories = readCsv("categories.csv").map { (id, name) ->
            Category(id.toInt(), name)
        }

        fun getSalesBook(month: Int) = sales.filter { it.date.split("-")[1].toInt() == month }
        fun getSalesBook(months: IntRange) = sales.filter { it.date.split("-")[1].toInt() in months }

        fun getCategory(productId: Int) = categories.find { it.id == productId }

        fun workDayIsOn() : Boolean {
            if (LocalDateTime.now().hour in 8..21) {
                return true
            }
            return false
        }

        companion object {
            private fun readCsv(name: String): List<List<String>> = //todo разобрать
                TestDatabase::class.java.getResourceAsStream("/$name")
                    ?.bufferedReader()
                    ?.use { it.readLines() }
                    ?.drop(1) // строка заголовка
                    ?.filter { it.isNotBlank() }
                    ?.map { it.split(",") }
                    ?: error("Ресурс /$name не найден в classpath")
        }
    }

    @Test
    fun salesAmount() {
        val db = TestDatabase()
        val myWorkflow = workflow2 {
            val orders = action("get orders") {
                db.getSalesBook(1..6)
            }

            val groupedSalesSortedDescending = action("group and sort") {
                val counter: MutableMap<String, Int> = mutableMapOf()
                val orderList = orders as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, 0) + 1
                }
                counter.toList().sortedByDescending { it.second }
            }
        }
    }

    @Test
    fun goodsTop() {
        val db = TestDatabase()
        val myWorkflow = workflow2 {
            val orders = action("get orders") {
                db.getSalesBook(1..6)
            }

            val groupedSalesSortedDescending = action("get answer") {
                val ans : MutableMap<String, List<Int>> = mutableMapOf()
                val counter = mutableMapOf<String, MutableMap<Int, Int>>()
                val orderList = orders as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter.getOrPut(category) { mutableMapOf() }[order.productId] = counter.getOrPut(category) { mutableMapOf() }.getOrDefault(order.productId, 0) + 1
                }
                counter.toList().forEach {
                     ans[it.first] = it.second.toList().sortedByDescending { it.second }.map { it.first }.slice(0..4)
                }
            }
        }
    }
    @Test
    fun minCategories() {
        val db = TestDatabase()
        val myWorkflow = workflow2 {
            val orders = action("get orders") {
                db.getSalesBook(1..6)
            }

            val ans = action("get answer") {
                val counter: MutableMap<String, Int> = mutableMapOf()
                val orderList = orders as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, 0) + 1
                }
                counter.toList().sortedBy { it.second }
                counter.toList().filter { it.second == counter.toList()[0].second }
            }
        }
    }
    @Test
    fun degradingCategories() {
        val db = TestDatabase()
        val myWorkflow = workflow2 {
            val period = 6
            val orders = action("get current orders") {
                db.getSalesBook(period)
            }
            val previousOrders = action("get previous orders") {
                db.getSalesBook(period - 1)
            }

            val ans = action("get answer") {
                val counter: MutableMap<String, Pair<Int, Int>> = mutableMapOf()
                val orderList = orders as List<*>
                val previousOrderList = previousOrders as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, Pair(0, 0)).first to counter.getOrDefault(category, Pair(0, 0)).second + 1
                }
                for (order in previousOrderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, Pair(0, 0)).first + 1 to counter.getOrDefault(category, Pair(0, 0)).second
                }
                counter.toList().filter { it.second.first > it.second.second }.map { it.first }
            }
        }
    }
    @Test
    fun streamTest() = runBlocking {
        val db = TestDatabase()
        val statMap = mutableMapOf<String, Int>()
        val properties = WorkflowProperty()
        properties.timeout = 60*60*24.seconds.inWholeMilliseconds

        val sellWithStatWorkflow = workflow2(properties) {

            action(name="get flow") { TestDatabase.MyOrderStream().takeIncomeOrderStream() }

            action(name="read flow with statistics",
                preActionCode = {
                    val flow = context.accumulator["get flow"] ?: throw IllegalStateException("Flow is not initialized")

                    if (flow !is Flow<*>) {
                        throw IllegalStateException("Flow is not of type Flow<TestDatabase.Sale>")
                    } else if ((flow as? Flow<TestDatabase.Sale>) == null) {
                        throw IllegalStateException("Flow is not of type Flow<TestDatabase.Sale>")
                    }
                },
                actionCode = {
                    val flow = context.accumulator["get flow"] as Flow<TestDatabase.Sale>
                    flow.takeWhile { db.workDayIsOn() }
                        .collect {
                        val category = db.getCategory(it.productId)?.name ?: "unknown"
                        statMap[category] = statMap.getOrDefault(category, 0) + 1
                    }
                },
                postActionCode = {
                    println("Today we sold:")
                    for ((category, count) in statMap) {
                        println("$count goods in category $category")
                    }
                }
            )
        }

    }
}

