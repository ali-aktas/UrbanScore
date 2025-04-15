package com.aliaktas.urbanscore.data.model

data class CountryModel(
    val id: String,        // Firestore'daki country id ile eşleşecek
    val name: String,      // Ülke adı
    val flagUrl: String    // Ülke bayrağı URL (opsiyonel)
) {
    companion object {
        fun getAll(): List<CountryModel> = listOf(
            CountryModel(id = "turkey", name = "Turkey", flagUrl = "https://example.com/flags/turkey.png"),
            CountryModel(id = "usa", name = "United States", flagUrl = "https://example.com/flags/usa.png"),
            CountryModel(id = "germany", name = "Germany", flagUrl = "https://example.com/flags/germany.png"),
            CountryModel(id = "france", name = "France", flagUrl = "https://example.com/flags/france.png"),
            // Tüm ülkeler eklenecek...
            CountryModel(id = "other", name = "Other", flagUrl = "")
        )
    }
}