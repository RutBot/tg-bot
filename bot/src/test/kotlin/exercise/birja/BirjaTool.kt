package exercise.birja

interface BirjaTool {
    val base: BirjaActive
    val quote: BirjaActive
    val asks: MutableList<BirjaOrder>
    val bids: MutableList<BirjaOrder>
    val code: String
}

data class BirjaToolInfo(
    val base: BirjaActive,
    val quote: BirjaActive,
    val code: String,
    val asks: MutableList<BirjaOrder>,
    val bids: MutableList<BirjaOrder>
) {
    companion object {
        fun from(tool: BirjaTool): BirjaToolInfo =
            BirjaToolInfo(
                base = tool.base,
                quote = tool.quote,
                code = "${tool.base.code}-${tool.quote.code}",
                asks = tool.asks,
                bids = tool.bids
            )
    }
}