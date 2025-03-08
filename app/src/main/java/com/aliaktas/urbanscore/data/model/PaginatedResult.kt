package com.aliaktas.urbanscore.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class PaginatedResult<T>(
    val items: List<T>,
    val lastVisible: DocumentSnapshot? = null,
    val hasMoreItems: Boolean = true
)