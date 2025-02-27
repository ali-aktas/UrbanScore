package com.aliaktas.urbanscore.data.model

data class UserModel(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val visitedCities: List<String> = emptyList(),
    val wishlistCities: List<String> = emptyList()
)
