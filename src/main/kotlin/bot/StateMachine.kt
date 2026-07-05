package bot

interface StateMachine {
    fun loadUser(user: User)
    fun getState(user: User): String?
    fun setState(user: User, state: String)
}