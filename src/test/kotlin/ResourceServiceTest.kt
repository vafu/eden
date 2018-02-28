import com.tuule.eden.resource.child
import com.tuule.eden.service.ResourceService
import com.tuule.eden.service.resource
import com.tuule.eden.service.resourceFromAbsoluteUrl
import io.mockk.mockk
import io.mockk.spyk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull


private const val validUrl = "http://something.com/"
private const val invalidUrl = "df/invalidUrl.com/"

private const val path1 = "some"
private const val path2 = "validPath"
private const val validPath = "$path1/$path2"
private const val invalidPath = "someßƒ©∂/sdf943049EFD:h"


class ResourceServiceTest : Spek({
    given("ResourceService") {
        val service: ResourceService = spyk(ResourceService(validUrl, mockk()))

        on("creating resources") {
            it("successfully creates resource when valid absolute path") {
                val resource = service.resourceFromAbsoluteUrl<Any>(validUrl)
                assertNotNull(resource)
            }

            it("throws ERROR when invalid absolute path ") {
                assertFails {
                    service.resourceFromAbsoluteUrl<Any>(invalidUrl)
                }
            }

            it("successfully creates resource with valid relative path") {
                val resource = service.resource<Any>(validPath)
                assertNotNull(resource)
            }

            it("should throw ERROR with invalid relative path") {
                assertFails {
                    service.resource<Any>(invalidPath)
                }
            }

            it("should create resource with valid absolute url and relative path") {
                val resource = service.resource<Any>(validUrl, validPath)
                assertNotNull(resource)
            }

            it("should throw ERROR with invalid absolute/relative url/path") {
                assertFails {
                    service.resource<Any>(invalidUrl, validPath)
                }
                assertFails {
                    service.resource<Any>(validUrl, invalidPath)
                }
            }
        }

        on("retrieving resources") {
            it("returns equal resources for all methods") {
                val relative = service.resource<Any>(validPath)
                val child = service.resource<Any>(path1).child<Any>(path2)
                val absolute = service.resource<Any>(validUrl, validPath)

                assertEquals(relative, child)
                assertEquals(child, absolute)
            }
        }
    }
})