package com.darklinvan.jsdy

data class Commands(
    val name: String,
    val params: List<String>,
    val options: Map<String, String>
)
