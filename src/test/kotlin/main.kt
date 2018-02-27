import com.tuule.eden.multiplatform.LogLevel
import com.tuule.eden.multiplatform.Logger
import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.okhttpadapter.OkhttpNetworkProvider
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.EntityCache
import com.tuule.eden.resource.Resource
import com.tuule.eden.service.ResourceService
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking


fun main(args: Array<String>) = runBlocking {
    //    Logger.logLevel = LogLevel.Full

    val serv1 = ResourceService("http://192.168.88.101:3000/", OkhttpNetworkProvider())
    val serv2 = ResourceService("http://192.168.88.101:3000/", OkhttpNetworkProvider())


    val res1 = serv1.resource<String>("/error/404")

    res1.load()



    delay(4000)
    val res2 = serv1.resource<String>("/error/404")
    delay(4000)
    println(res2.data)
    Unit
}


data class Test(val field: String, val field2: String)