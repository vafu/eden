import com.tuule.eden.multiplatform.LogLevel
import com.tuule.eden.multiplatform.Logger
import com.tuule.eden.okhttpadapter.OkhttpNetworkProvider
import com.tuule.eden.service.ResourceService

fun main(args: Array<String>) {
    Logger.logLevel = LogLevel.Full
    val resourceService = ResourceService("http://google.com", OkhttpNetworkProvider())

    val index = resourceService.resource<String>("/")
    index.load()

    readLine()
}