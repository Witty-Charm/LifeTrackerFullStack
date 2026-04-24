package com.lifetracker.mobile.data.remote

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleDeviceHeaderTest {
    @Test
    fun addDeviceIdHeader_addsXDeviceIdToRequest() {
        val original = Request.Builder().url("http://localhost/test").build()

        val updated = NetworkModule.addDeviceIdHeader(original, "11111111-1111-1111-1111-111111111111")

        assertEquals("11111111-1111-1111-1111-111111111111", updated.header("X-Device-Id"))
    }
}
