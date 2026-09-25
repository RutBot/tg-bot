package exercise

import org.junit.jupiter.api.Test

fun sortString(s: String) : String{
    val sortedCodes = mergeSort(s.map { it.code })
    return sortedCodes.map { it.toChar() }.toString()
}

fun myReverseString(s: String) : String {
    val chars = s.toCharArray()
    val range =  if (chars.size % 2 != 0) chars.size/2 else chars.size/2 - 1
    for (i in 0..range) {
        val temp = chars[i]
        chars[i] = chars[chars.size - 1 - i]
        chars[chars.size - 1 - i] = temp
    }
    return chars.joinToString("")
}

