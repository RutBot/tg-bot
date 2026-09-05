package exercise.birja


interface BirjaActive {
    val name: String
    val code: String
}

interface CompanyShares : BirjaActive {
    val companyName: String
}

class Bitcoin : BirjaActive {
    override val name: String = "Bitcoin"
    override val code: String = "btc"
}

class UnitedStatesDollar : BirjaActive {
    override val name: String = "$"
    override val code: String = "usd"
}

class AppleShares : CompanyShares {
    override val name: String = "Apple Shares"
    override val code: String = "aapl"
    override val companyName: String = "Apple Inc."
}

