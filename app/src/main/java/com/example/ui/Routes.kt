package com.example.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable object Home
    @Serializable object Vault
    @Serializable object Scan
    @Serializable object Editor
    @Serializable object History
}
