package exercise

import org.junit.jupiter.api.Test

interface FamilyMember {
    val familyName: String
}

class MyFamilyCollection {
    val content : MutableList<FamilyMember> = mutableListOf()

    fun add(member: FamilyMember) {
        content.add(member)
    }

    inline fun <reified T : FamilyMember> find(name: String): FamilyMember? {
        return content.find { it.familyName == name && it is T }
    }

    inline fun <reified T : FamilyMember> delete(name: String) {
        content.removeIf { it.familyName == name && it is T}
    }
}

class FamilyCollectionTest {
    @Test
    fun test(){
        val family = MyFamilyCollection()
        class Cat(override val familyName: String) : FamilyMember
        class Dog(override val familyName: String) : FamilyMember
        class Person(override val familyName: String) : FamilyMember


        val barsik = Cat("barsik")
        val bobik = Dog("Sasha")
        val friend = Person("Sasha")

        family.add(barsik)
        family.add(bobik)
        family.add(friend)
        assert(family.find<Cat>("barsik") == barsik)
        assert(family.find<Person>("Sasha") == friend)
        assert(family.find<Dog>("Sasha") == bobik)
        family.delete<Cat>("barsik")
        assert(family.find<Cat>("barsik") == null)
    }
}
