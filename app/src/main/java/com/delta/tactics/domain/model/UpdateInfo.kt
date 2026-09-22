package com.delta.tactics.domain.model

import java.io.File

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val changelog: String,
    val apkUrl: String,
    val browserUrl: String,
    val forceUpdate: Boolean = false,
    val hasUpdate: Boolean = false
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val currentBytes: Long, val totalBytes: Long) : UpdateDownloadState()
    data class Success(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}
