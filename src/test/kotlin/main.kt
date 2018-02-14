import com.tuule.eden.multiplatform.LogLevel
import com.tuule.eden.multiplatform.Logger
import com.tuule.eden.okhttpadapter.OkhttpNetworkProvider
import com.tuule.eden.service.ResourceService

fun main(args: Array<String>) {
    Logger.logLevel = LogLevel.Full

    val serv = ResourceService("http://demo9699082.mockable.io", OkhttpNetworkProvider())
    val res = serv.resource<Test>("/")

    res.request()
            .apply {
                onSuccess { println(it.content) }
            }
            .start()


    readLine()
}

data class Test(val field: String, val field2: String)