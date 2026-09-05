package exercise.birja

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID


interface StockExchange {
    fun isOpen(): Boolean
    fun showTickers(): List<String>
    fun showOrders(ticker: String): List<InstrumentBit>
    fun placeOrder(orderAmount: Int, type: OrderType, ticker: String)
}

interface StockExchangeAccount {
    fun createAccount(fio: String, amount: BigDecimal): Account
    fun showAccounts(): List<String>
    fun showBalance(accountId: UUID): BigDecimal
}

data class Account(val id: UUID, val fio: String, val amount: BigDecimal)
enum class OrderType { MARKET, LIMIT }
data class InstrumentBit(val ticker: String, val bid: BigDecimal, val ask: BigDecimal)

interface StockExchangeApi {
    fun getStockExchange(): StockExchange
    fun getStockExchangeAccount(): StockExchangeAccount
}

interface StockExchangeAggregator {
    fun showTickers(): List<String>
    fun showOrders(ticker: String): List<InstrumentBit>
    fun placeOrder(orderAmount: Int, type: OrderType, ticker: String)
}

interface Birja {
    val userRepository: UserRepository

    fun getTools(): List<BirjaTool>
    fun getToolInfo(tool: BirjaTool) : BirjaToolInfo
    fun placeAskOrder(order: BirjaOrder)
    fun placeBidOrder(order: BirjaOrder)
    fun marketBuy(tool: BirjaTool, quantity: BigDecimal)
    fun marketSell(tool: BirjaTool, quantity: BigDecimal)
    val supportedActives: MutableList<BirjaActive>
    val supportedTools: MutableList<BirjaTool>
    fun openExchange()
    fun closeExchange()
}

class MyBirja : Birja {
    override val userRepository = InMemoryUserRepository()

    override fun getTools(): List<BirjaTool> {
        return supportedTools
    }

    override fun getToolInfo(tool: BirjaTool) : BirjaToolInfo = BirjaToolInfo.from(tool)

    override fun openExchange() {
        supportedActives.forEach { base ->
            for (quote in supportedActives) {
                if (base != quote) {
                    val tool = object : BirjaTool {
                        override val base: BirjaActive = base
                        override val quote: BirjaActive = quote
                        override val asks: MutableList<BirjaOrder> = mutableListOf()
                        override val bids: MutableList<BirjaOrder> = mutableListOf()
                        override val code: String = "${base.code}-${quote.code}"
                    }
                    supportedTools.add(tool)
                }
            }
        }
    }

    override fun closeExchange() {
        supportedTools.forEach {
            it.asks.clear()
            it.bids.clear()
        }
        supportedTools.clear()
    }
}

class MyBirjaTest {
    @Test
    fun test1() {
        val birja = MyBirja()
        birja.supportedActives.add(Bitcoin())
        birja.supportedActives.add(UnitedStatesDollar())
        birja.openExchange()

        birja.userRepository.registerUser(MyStockUser("Pavlik"))
        birja.userRepository.registerUser(MyStockUser("John"))
        birja.userRepository.registerUser(MyStockUser("Alice"))


    }
}