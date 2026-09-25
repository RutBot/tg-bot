package exercise

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import my.workflows.WorkflowGeneral
import my.workflows.WorkflowProperty
import my.workflows.workflow2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds
class TestDatabase {
    interface OrderStream {
        fun takeIncomeOrderStream() : Flow<Sale>
    }

    class MyOrderStream : OrderStream {
        override fun takeIncomeOrderStream(): Flow<Sale> {
            return flow {
                var incId = 100
                while (true) {
                    emit(Sale(incId++,(1..20).random(), (1..80).random(), LocalDateTime.now().toString()))
                    delay(0.2.seconds)
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

class SalesAmountSolution {
    fun salesAmount(db: TestDatabase): WorkflowGeneral {
        val myWorkflow = workflow2 {
            action("get orders") {
                db.getSalesBook(1..6)
            }

            action("group and sort") {
                val counter: MutableMap<String, Int> = mutableMapOf()
                val orderList = context.accumulator["get orders"] as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, 0) + 1
                }
                counter.toList().sortedByDescending { it.second }
            }
        }
        myWorkflow.start()
        return myWorkflow
    }
    @Test
    fun answerTest() {
        val db = TestDatabase()
        val wf = salesAmount(db)
        val res = wf.context.accumulator["group and sort"]
        val expected = listOf(
            "Бытовая техника" to 10,
            "Обувь" to 9,
            "Одежда" to 8,
            "Дом" to 7,
            "Электроника" to 6,
            "Книги" to 6,
            "Красота" to 6,
            "Спорт" to 1
        )

        assertEquals(expected, res)
    }
    @Test
    fun executionTimeTest() {
        val db = TestDatabase()
        val wf = salesAmount(db)
        assert(wf.getWorkTime() < 10.seconds.inWholeMilliseconds)
    }
    @Test
    fun launchAmountTest() {
        val db = TestDatabase()
        val wf = salesAmount(db)
        assert(wf.getLaunchCount() == 1)
    }
}

class GoodsTopSolution {
    fun goodsTop(db: TestDatabase): WorkflowGeneral {
        val myWorkflow = workflow2 {
            action("get orders") {
                db.getSalesBook(1..6)
            }

            action("get answer") {
                val ans : MutableMap<String, List<Int>> = mutableMapOf()
                val counter = mutableMapOf<String, MutableMap<Int, Int>>()
                val orderList = context.accumulator["get orders"] as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter.getOrPut(category) { mutableMapOf() }[order.productId] = counter.getOrPut(category) { mutableMapOf() }.getOrDefault(order.productId, 0) + 1
                }
                counter.toList().forEach {
                    ans[it.first] = it.second.toList().sortedByDescending { it.second }.map { it.first }.take(5)
                }
                ans
            }
        }
        myWorkflow.start()
        return myWorkflow
    }

    @Test
    fun answerTest() {
        val wf = goodsTop(TestDatabase())
        val expected = mapOf(
            "Электроника" to listOf(1),
            "Бытовая техника" to listOf(2),
            "Одежда" to listOf(3),
            "Обувь" to listOf(4),
            "Книги" to listOf(5),
            "Спорт" to listOf(6),
            "Красота" to listOf(7),
            "Дом" to listOf(8)
        )
        assertEquals(expected, wf.context.accumulator["get answer"])
    }

    @Test
    fun executionTimeTest() {
        val wf = goodsTop(TestDatabase())
        assertEquals(true, wf.getWorkTime() < 10.seconds.inWholeMilliseconds)
    }

    @Test
    fun launchAmountTest() {
        val wf = goodsTop(TestDatabase())
        assertEquals(1, wf.getLaunchCount())
    }
}

class MinCategoriesSolution {
    fun minCategories(db: TestDatabase): WorkflowGeneral {
        val myWorkflow = workflow2 {
            action("get orders") {
                db.getSalesBook(1..6)
            }

            action("get answer") {
                val counter: MutableMap<String, Int> = mutableMapOf()
                val orderList = context.accumulator["get orders"] as List<*>
                for (order in orderList) {
                    if (order !is TestDatabase.Sale) continue
                    val category = db.getCategory(order.productId)?.name ?: continue
                    counter[category] = counter.getOrDefault(category, 0) + 1
                }
                counter.toList().filter { it.second == counter.values.min() }
            }
        }
        myWorkflow.start()
        return myWorkflow
    }

    @Test
    fun answerTest() {
        val wf = minCategories(TestDatabase())
        assertEquals(listOf("Спорт" to 1), wf.context.accumulator["get answer"])
    }

    @Test
    fun executionTimeTest() {
        val wf = minCategories(TestDatabase())
        assertEquals(true, wf.getWorkTime() < 10.seconds.inWholeMilliseconds)
    }

    @Test
    fun launchAmountTest() {
        val wf = minCategories(TestDatabase())
        assertEquals(1, wf.getLaunchCount())
    }
}

class DegradingCategoriesSolution {
    fun degradingCategories(db: TestDatabase): WorkflowGeneral {
        val myWorkflow = workflow2 {
            val period = 6
            action("get current orders") {
                db.getSalesBook(period)
            }
            action("get previous orders") {
                db.getSalesBook(period - 1)
            }

            action("get answer") {
                val counter: MutableMap<String, Pair<Int, Int>> = mutableMapOf()
                val orderList = context.accumulator["get current orders"] as List<*>
                val previousOrderList = context.accumulator["get previous orders"] as List<*>
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
        myWorkflow.start()
        return myWorkflow
    }

    @Test
    fun answerTest() {
        val wf = degradingCategories(TestDatabase())
        assertEquals(listOf("Бытовая техника", "Одежда"), wf.context.accumulator["get answer"])
    }

    @Test
    fun executionTimeTest() {
        val wf = degradingCategories(TestDatabase())
        assertEquals(true, wf.getWorkTime() < 10.seconds.inWholeMilliseconds)
    }

    @Test
    fun launchAmountTest() {
        val wf = degradingCategories(TestDatabase())
        assertEquals(1, wf.getLaunchCount())
    }
}

class StreamSolution {
    fun streamStat(db: TestDatabase): WorkflowGeneral {
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
                    val statMap = mutableMapOf<String, Int>()
                    val flow = context.accumulator["get flow"] as Flow<TestDatabase.Sale>
                    flow.takeWhile { statMap.size < 3 }
                        .collect {
                        val category = db.getCategory(it.productId)?.name ?: "unknown"
                        statMap[category] = statMap.getOrDefault(category, 0) + 1
                    }
                    statMap
                },
                postActionCode = {
                    val res = context.accumulator["read flow with statistics"] as Map<*, *>?
                    if (res.isNullOrEmpty()) {
                        throw IllegalStateException("No statMap received")
                    }
                }
            )
        }
        sellWithStatWorkflow.start()
        return sellWithStatWorkflow
    }

    @Test
    fun answerTest() {
        val db = TestDatabase()

        val wf = streamStat(db)
        val statistics = wf.context.accumulator["read flow with statistics"] as Map<*, *>

        assert(3 == statistics.size)
        assert(!statistics.containsKey("unknown"))
    }

    @Test
    fun executionTimeTest() {
        val wf = streamStat(TestDatabase())
        assertEquals(true, wf.getWorkTime() < 30.seconds.inWholeMilliseconds)
    }

    @Test
    fun launchAmountTest() {
        val wf = streamStat(TestDatabase())
        assertEquals(1, wf.getLaunchCount())
    }
}
