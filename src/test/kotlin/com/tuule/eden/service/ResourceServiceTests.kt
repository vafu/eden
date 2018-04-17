package com.tuule.eden.service

import io.mockk.mockk
import io.mockk.spyk
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

private const val validUrl = "http://something.com/"
private const val invalidUrl = "df/com.tuule.eden.service.invalidUrl.com/"

private const val path1 = "some"
private const val path2 = "validPath"
private const val validPath = "${path1}/${path2}"
private const val invalidPath = "someßƒ©∂/sdf943049EFD:h"

class ResourceCreationTest {

    private val service = spyk(ResourceService(validUrl, mockk()))

    @Test
    fun `successfully creates resource with valid absolute path`() {
        val resource = service.resourceFromAbsoluteURL<Any>(validUrl)
        assertNotNull(resource)
    }

    @Test
    fun `throws exception with invalid absolute path`() {
        assertThrows<IllegalArgumentException> {
            service.resourceFromAbsoluteURL<Any>(invalidUrl)
        }
    }

    @Test
    fun `successfully creates resource with valid relative path`() {
        val resource = service.resource<Any>(validPath)
        assertNotNull(resource)
    }

    @Test
    fun `throws throw ERROR with invalid relative path`() {
        assertThrows<IllegalArgumentException> {
            service.resource<Any>(invalidPath)
        }
    }

    @Test
    fun `creates resource with valid absolute url and relative path`() {
        val resource = service.resource<Any>(validUrl, validPath)
        assertNotNull(resource)
    }

    @Test
    fun `should throw ERROR with invalid absolute(relative) url(path)`() {
        assertThrows<IllegalArgumentException> {
            service.resource<Any>(invalidUrl, validPath)
        }
        assertThrows<IllegalArgumentException> {
            service.resource<Any>(validUrl, invalidPath)
        }
    }

    @Test
    fun `return the same resource for all methods`() {
        val relative = service.resource<Any>(validPath)
        val child = service.resource<Any>(path1).child<Any>(path2)
        val absolute = service.resource<Any>(validUrl, validPath)

        assertEquals(relative, child)
        assertEquals(child, absolute)
    }
}