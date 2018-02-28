import com.tuule.eden.multiplatform.LogLevel
import com.tuule.eden.multiplatform.Logger
import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.okhttpadapter.OkhttpNetworkProvider
import com.tuule.eden.pipeline.ErrorExtractor
import com.tuule.eden.pipeline.Pipeline
import com.tuule.eden.pipeline.getTypeTransformer
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.EntityCache
import com.tuule.eden.resource.Resource
import com.tuule.eden.service.ResourceService
import com.tuule.eden.service.resource
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.*


fun main(args: Array<String>) = runBlocking {
    Logger.logLevel = LogLevel.Full

    val serv1 = ResourceService("http://demo9699082.mockable.io/", OkhttpNetworkProvider())

    serv1.configure {
        copy(pipeline = pipeline.apply {
            get(Pipeline.StageKey.CLEANUP).add(ErrorExtractor(TestError::class.java))
        })
    }

    val res1 = serv1.resource<Int>("/jsonArray")

    res1.load()
            .onData(::println)
            .onFailure { it.entity?.content?.let(::println) }
    delay(4000)
}


data class Test(val field: String, val field2: String)
data class TestError(val errorMessage: String)