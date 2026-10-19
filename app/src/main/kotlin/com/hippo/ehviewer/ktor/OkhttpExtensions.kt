package com.hippo.ehviewer.ktor

import com.ehviewer.core.util.isAtLeastQ
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.android.AndroidDns

fun OkHttpConfig.configureClient() {
    config {
        if (isAtLeastQ) {
            dns(AndroidDns())
        }
    }
}
