package exercise.birja

import java.math.BigDecimal

interface BirjaOrder {
    val tool: BirjaTool
    val price: BigDecimal
    val quantity: BigDecimal
    val maker: BirjaUser
    val creationTime: Long
}