package exercise

enum class TrubaEventType { UserRegistered, PriceIncreased, OrderFinished }
data class TrubaEvent(val name: String, val message: String, val type: TrubaEventType)

fun generateNextEvent() {

}

interface Truba {
    fun receiveEvent(event: TrubaEvent)
    fun forwardEventToSubscribers(event: TrubaEvent)
}

interface Obvyazka {
    fun subscribeToTruba(truba: Truba, eventType: TrubaEventType, predicate: ((TrubaEvent) -> Boolean)? = null)
}