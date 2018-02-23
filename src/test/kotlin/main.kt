import com.tuule.eden.multiplatform.LogLevel
import com.tuule.eden.multiplatform.Logger
import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.okhttpadapter.OkhttpNetworkProvider
import com.tuule.eden.service.ResourceService
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    Logger.logLevel = LogLevel.Full

    val serv = ResourceService("http://demo9699082.mockable.io", OkhttpNetworkProvider())
    val res = serv.resource<Test>("/")

    res
            .load()
            .onData(::println)

    delay(10000)
    Unit
}


data class Test(val field: String, val field2: String)