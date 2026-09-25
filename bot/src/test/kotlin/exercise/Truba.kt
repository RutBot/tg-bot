package exercise

enum class EventType { UserRegistered, PriceIncreased, OrderFinished }
data class GeneralEvent(val name: String, val message: String, val type: EventType)

fun generateNextEvent() {

}

interface Truba {
    fun receiveEvent(event: GeneralEvent)
    fun forwardEventToSubscribers(event: GeneralEvent)
}

interface Obvyazka {
    fun subscribeToTruba(truba: Truba, eventType: EventType, predicate: ((GeneralEvent) -> Boolean)? = null)
}


interface PubSub {
    fun fireEvent(event: GeneralEvent)
    fun subscribe(eventType: EventType, predicate: ((GeneralEvent) -> Boolean)? = null, handler: (GeneralEvent) -> Unit): Subscription
    fun onEvent(handler: (GeneralEvent) -> Unit)
    fun subscribeOnFailedEvent(handler: (Throwable) -> Unit)
    fun removeSubscription(subscription: Subscription)
}