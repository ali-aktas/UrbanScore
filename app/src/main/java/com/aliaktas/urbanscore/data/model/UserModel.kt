package com.aliaktas.urbanscore.data.model

data class UserModel(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val visited_cities: Map<String, Double> = emptyMap(),
    val wishlist_cities: List<String> = emptyList()
)