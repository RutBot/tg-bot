package exercise.birja

import java.util.UUID

interface UserRepository {
    fun registerUser(user: BirjaUser)
    fun getUser(predicate: (BirjaUser) -> Boolean): BirjaUser?
}

interface BirjaUser {
    val id: String
    val Actives: MutableSet<BirjaActive>
    val Orders: MutableSet<BirjaOrder>
}

class MyStockUser(val name: String) : BirjaUser {
    override val id: String = UUID.randomUUID().toString()
    override val Actives: MutableSet<BirjaActive> = mutableSetOf()
    override val Orders: MutableSet<BirjaOrder> = mutableSetOf()
}

class InMemoryUserRepository : UserRepository {
    private val users: MutableSet<BirjaUser> = mutableSetOf()

    override fun registerUser(user: BirjaUser) {
        users.add(user)
    }

    override fun getUser(predicate: (BirjaUser) -> Boolean): BirjaUser? {
        return users.find(predicate)
    }
}