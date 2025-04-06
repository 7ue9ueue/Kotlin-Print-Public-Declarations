package sample

// Public function (no explicit modifier defaults to public)
fun publicFunction() {
    println("I'm public")
}

// Private function
private fun privateFunction() {
    println("I'm private")
}

// Public class (explicitly public)
public class PublicClass {
    fun greet() = println("Hello from PublicClass")
}

// Internal class (not public)
internal class InternalClass {
    fun greet() = println("Hello from InternalClass")
}
