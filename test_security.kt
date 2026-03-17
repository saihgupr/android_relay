import org.json.JSONObject

fun main() {
    val payload = JSONObject().apply {
        put("status", "test")
        put("message", "Hello from Android Relay UI")
    }.toString()
    println(payload)
}
