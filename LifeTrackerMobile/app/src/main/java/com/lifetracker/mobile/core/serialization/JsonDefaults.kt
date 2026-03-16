package com.lifetracker.mobile.core.serialization

import kotlinx.serialization.json.Json

val JsonDefaults = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
