package com.tuule.eden.resource

import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath

class Resource<T>(val service: ResourceService,
                  val url: String) {


    fun <T> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))
}