import com.tuule.eden.service.ResourceService
import io.mockk.spyk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
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
        val service: ResourceService = spyk(ResourceService(validUrl))

        on("creating resources") {

            it("successfully creates resource when valid") {
                val resource = service.resourceFromAbsoluteURL<Any>(validUrl)
                assertNotNull(resource)
            }

            it("throws error when invalid") {
                assertFails {
                    service.resourceFromAbsoluteURL<Any>(invalidUrl)
                }
            }

            it("successfully creates resource with valid relative path") {
                val resource = service.resource<Any>(validPath)
                assertNotNull(resource)
            }

            it("should throw error with invalid relative path") {
                assertFails {
                    service.resource<Any>(invalidPath)
                }
            }

            it("should create resource with valid absolute url and relative path") {
                val resource = service.resource<Any>(validUrl, validPath)
                assertNotNull(resource)
            }

            it("should throw error with invalid absolute/relative url/path") {
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