package com.gk.news_pro.data.model

import com.google.firebase.database.PropertyName

data class User(
    val id: String = "",
    val email: String = "",
    val password: String = "",
    val avatar: String = "",
    val username: String = "",
    val favoriteTopics: Map<String, Int> = emptyMap(),
    val favoriteNews: Map<String, News> = emptyMap(),
    val language: String = "vi",
    val admin: Boolean = false,
)
