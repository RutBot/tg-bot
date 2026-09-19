package exercise.birja

import java.math.BigDecimal
import java.util.UUID


interface StockExchange {
    fun isOpen(): Boolean
    fun showTickers(): List<String>
    fun showOrders(ticker: String): List<Order>
    fun placeOrder(orderAmount: Int, executionType: OrderExecutionType, ticker: String, type: OrderType, owner: Account)
}

interface StockExchangeAccount {
    fun createAccount(fio: String, amount: BigDecimal): Account
    fun showAccounts(): List<String>
    fun showBalance(accountId: UUID): BigDecimal
}

data class Account(val id: UUID, val fio: String, val amount: BigDecimal)
enum class OrderExecutionType { MARKET, LIMIT }
enum class OrderType { BID, ASK }
data class Order(val amount: Int, val executionType: OrderExecutionType, val ticker: String, val type: OrderType, val owner : Account, val price : Int? = null)
data class InstrumentBit(val ticker: String, val bid: BigDecimal, val ask: BigDecimal)

interface StockExchangeApi {
    fun getStockExchange(): StockExchange
    fun getStockExchangeAccount(): StockExchangeAccount
}

interface StockExchangeAggregator {
    fun showTickers(): List<String>
    fun showOrders(ticker: String): List<InstrumentBit>
    fun placeOrder(orderAmount: Int, executionType: OrderExecutionType, ticker: String, type: OrderType, owner: Account)
}

class MyStockExchangeAccounter : StockExchangeAccount {
    private val accounts: MutableList<Account> = mutableListOf()

    override fun createAccount(fio: String, amount: BigDecimal): Account {
        val account = Account(UUID.randomUUID(), fio, amount)
        accounts.add(account)
        return account
    }

    override fun showAccounts(): List<String> {
        return accounts.map { it.fio }
    }

    override fun showBalance(accountId: UUID): BigDecimal {
        return accounts.find { it.id == accountId }?.amount ?: BigDecimal.ZERO
    }
}

class MyStockExchange : StockExchange {
    val instruments: MutableList<InstrumentBit> = mutableListOf()
    val orders = mutableListOf<Order>()

    fun executeOrder(order: Order) {
        //todo
        val oppositeOrders = if (order.type == OrderType.ASK) orders.filter { it.ticker == order.ticker && it.type == OrderType.BID }.toMutableList().sortedByDescending { it.price } else orders.filter { it.ticker == order.ticker && it.type == OrderType.ASK }.toMutableList().sortedBy { it.price }
        val account = order.owner
        while (account.amount > BigDecimal.ZERO && order.amount > 0) {
            val curOrd = oppositeOrders.first()
            if (curOrd.amount > order.amount) {
                orders.add(Order(curOrd.amount - order.amount, curOrd.executionType, curOrd.ticker, curOrd.type, curOrd.owner))
            } else {

            }
        }
    }

    override fun isOpen(): Boolean {
        //return Instant.now().atZone(ZoneOffset.UTC).hour in 9..21
        return true
    }

    override fun showTickers(): List<String> {
        return instruments.map { it.ticker }
    }

    override fun showOrders(ticker: String): List<Order> = orders.filter { it.ticker == ticker }


    override fun placeOrder(orderAmount: Int, executionType: OrderExecutionType, ticker: String, type: OrderType, owner: Account) {
        if (executionType == OrderExecutionType.LIMIT) {
            orders.add(Order(orderAmount, executionType, ticker, type, owner))
        } else {
            //executeOrder(Order(orderAmount, executionType, ticker, type))
        }
    }

    fun updateTickerOrderBook(){
        //todo
    }

    fun getTickerPrice(ticker: String): BigDecimal? {
        val instrument = instruments.find { it.ticker == ticker }
        return instrument?.let { (it.bid + it.ask) / BigDecimal(2) }
    }
}

class MyStockExchangeApi : StockExchangeApi {
    private val stockExchange = MyStockExchange()
    private val stockExchangeAccount = MyStockExchangeAccounter()

    override fun getStockExchange(): StockExchange {
        return stockExchange
    }

    override fun getStockExchangeAccount(): StockExchangeAccount {
        return stockExchangeAccount
    }
}

//class MyStockExchangeAggregator : StockExchangeAggregator {
//    //todo
//}

//
//interface Birja {
//    val userRepository: UserRepository
//
//    fun getTools(): List<BirjaTool>
//    fun getToolInfo(tool: BirjaTool) : BirjaToolInfo
//    fun placeAskOrder(order: BirjaOrder)
//    fun placeBidOrder(order: BirjaOrder)
//    fun marketBuy(tool: BirjaTool, quantity: BigDecimal)
//    fun marketSell(tool: BirjaTool, quantity: BigDecimal)
//    val supportedActives: MutableList<BirjaActive>
//    val supportedTools: MutableList<BirjaTool>
//    fun openExchange()
//    fun closeExchange()
//}
//
//class MyBirja : Birja {
//    override val userRepository = InMemoryUserRepository()
//
//    override fun getTools(): List<BirjaTool> {
//        return supportedTools
//    }
//
//    override fun getToolInfo(tool: BirjaTool) : BirjaToolInfo = BirjaToolInfo.from(tool)
//
//    override fun openExchange() {
//        supportedActives.forEach { base ->
//            for (quote in supportedActives) {
//                if (base != quote) {
//                    val tool = object : BirjaTool {
//                        override val base: BirjaActive = base
//                        override val quote: BirjaActive = quote
//                        override val asks: MutableList<BirjaOrder> = mutableListOf()
//                        override val bids: MutableList<BirjaOrder> = mutableListOf()
//                        override val code: String = "${base.code}-${quote.code}"
//                    }
//                    supportedTools.add(tool)
//                }
//            }
//        }
//    }
//
//    override fun closeExchange() {
//        supportedTools.forEach {
//            it.asks.clear()
//            it.bids.clear()
//        }
//        supportedTools.clear()
//    }
//}
//
//class MyBirjaTest {
//    @Test
//    fun test1() {
//        val birja = MyBirja()
//        birja.supportedActives.add(Bitcoin())
//        birja.supportedActives.add(UnitedStatesDollar())
//        birja.openExchange()
//
//        birja.userRepository.registerUser(MyStockUser("Pavlik"))
//        birja.userRepository.registerUser(MyStockUser("John"))
//        birja.userRepository.registerUser(MyStockUser("Alice"))
//
//
//    }
//}