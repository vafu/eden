import com.tuule.eden.service.ResourceService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class ResourceCreationTest {
    companion object {
        private const val validUrl = "http://something.com/"
        private const val invalidUrl = "df/invalidUrl.com/"

        private const val path1 = "some"
        private const val path2 = "validPath"
        private const val validPath = "$path1/$path2"
        private const val invalidPath = "someßƒ©∂/sdf943049EFD:h"
    }

    @Test
    fun `creating absolute resource`() {
        val service = ResourceService()

        service.resourceFromAbsoluteURL<Any>(validUrl)

        assertThrows(RuntimeException::class.java) {
            service.resourceFromAbsoluteURL<Any>(invalidUrl)
        }
    }

    @Test
    fun `creating relative resource`() {
        val service = ResourceService(validUrl)
        service.resource<Any>(validPath)

        assertThrows(RuntimeException::class.java) {
            service.resource<Any>(invalidPath)
        }
    }


    @Test
    fun `creating absolute resource with relative path`() {
        val service = ResourceService()

        service.resource<Any>(validUrl, validPath)

        assertThrows(RuntimeException::class.java) {
            service.resource<Any>(invalidUrl, validPath)
        }

        assertThrows(RuntimeException::class.java) {
            service.resource<Any>(validUrl, invalidPath)
        }
    }

    @Test
    fun `equality of resources`() {
        val service = ResourceService(validUrl)

        val relative = service.resource<Any>(validPath)
        val child = service.resource<Any>(path1).child<Any>(path2)
        val absolute = service.resource<Any>(validUrl, validPath)

        assertAll(
                Executable { assertSame(relative, child) },
                Executable { assertSame(child, absolute) }
        )
    }
}
