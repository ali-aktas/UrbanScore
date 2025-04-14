package com.aliaktas.urbanscore.ui.profile

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.base.BaseViewModel
import com.aliaktas.urbanscore.data.model.CityModel
import com.aliaktas.urbanscore.data.repository.CityRepository
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.aliaktas.urbanscore.util.ImageUtils
import com.aliaktas.urbanscore.util.NetworkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ProfileState ve ilgili veri sınıfları
sealed class ProfileState {
    data object Loading : ProfileState()
    data class Success(
        val displayName: String,
        val photoUrl: String,
        val visitedCities: List<VisitedCityItem>,
        val wishlistCities: List<WishlistCityItem>
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

data class VisitedCityItem(
    val id: String,
    val name: String,
    val country: String,
    val flagUrl: String,
    val userRating: Double,
    val position: Int = 0
)

data class WishlistCityItem(
    val id: String,
    val name: String,
    val country: String,
    val flagUrl: String
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val cityRepository: CityRepository,
    private val networkUtil: NetworkUtil,
    private val imageUtils: ImageUtils
) : BaseViewModel() {

    // UI state'i için tek bir flow
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // Paylaşım intent'i için ayrı flow (tek kullanımlık)
    private val _shareIntent = MutableSharedFlow<Intent?>(replay = 0)
    val shareIntent: SharedFlow<Intent?> = _shareIntent.asSharedFlow()

    // Şehir verilerini cache'lemek için map
    private val cityCache = mutableMapOf<String, CityModel>()

    init {
        setupDataStreams()
    }

    private fun loadUserProfile() {
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            try {
                // İnternet bağlantısı kontrolü
                if (!networkUtil.isNetworkAvailable()) {
                    _profileState.value = ProfileState.Error("Internet connection required")
                    return@launch
                }

                // Paralel asenkron işlemler ile hem kullanıcı verilerini hem de şehir verilerini alalım
                coroutineScope {
                    // Kullanıcının temel verilerini al
                    val userDeferred = async { userRepository.getCurrentUser().first() }

                    // Kullanıcının ziyaret ettiği şehirleri al
                    val visitedCitiesDeferred = async { userRepository.getUserVisitedCities().first() }

                    // Kullanıcının bucket list'ini al
                    val wishlistDeferred = async { userRepository.getUserWishlist().first() }

                    // Tüm asenkron işlemlerin tamamlanmasını bekle
                    val user = userDeferred.await() ?: run {
                        _profileState.value = ProfileState.Error("User not authenticated")
                        return@coroutineScope
                    }

                    val visitedCitiesMap = visitedCitiesDeferred.await()
                    val wishlistIds = wishlistDeferred.await()

                    // Ziyaret edilen şehirler için detaylı verileri getir
                    val visitedCities = fetchCityDetails(visitedCitiesMap)

                    // Wishlist için detaylı verileri getir
                    val wishlistCities = fetchWishlistCityDetails(wishlistIds)

                    // Başarılı durumu güncelle
                    _profileState.value = ProfileState.Success(
                        displayName = user.displayName.ifEmpty { "Traveler" },
                        photoUrl = user.photoUrl,
                        visitedCities = visitedCities,
                        wishlistCities = wishlistCities
                    )
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("Failed to load profile: ${e.localizedMessage}")
                logError("Profile loading error", e)
            }
        }
    }

    private fun setupDataStreams() {
        viewModelScope.launch {
            try {
                // İnternet bağlantısı kontrolü
                if (!networkUtil.isNetworkAvailable()) {
                    _profileState.value = ProfileState.Error("Internet connection required")
                    return@launch
                }

                // Loading state göster
                _profileState.value = ProfileState.Loading

                // Üç Flow'u birleştirerek tek bir Flow oluşturuyoruz
                combine(
                    userRepository.getCurrentUser(),                // Kullanıcı verileri
                    userRepository.getUserVisitedCities(),          // Ziyaret edilen şehirler
                    userRepository.getUserWishlist()                // Bucket list
                ) { user, visitedCitiesMap, wishlistIds ->
                    Triple(user, visitedCitiesMap, wishlistIds)
                }.collectLatest { (user, visitedCitiesMap, wishlistIds) ->
                    // Kullanıcı giriş yapmamış olabilir
                    if (user == null) {
                        _profileState.value = ProfileState.Error("User not authenticated")
                        return@collectLatest
                    }

                    // Şehir detaylarını yükleme işlemlerini başlat
                    val visitedCities = fetchCityDetails(visitedCitiesMap)
                    val wishlistCities = fetchWishlistCityDetails(wishlistIds)

                    // Success state güncelle
                    _profileState.value = ProfileState.Success(
                        displayName = user.displayName.ifEmpty { "Traveler" },
                        photoUrl = user.photoUrl,
                        visitedCities = visitedCities,
                        wishlistCities = wishlistCities
                    )
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error("Failed to load profile: ${e.localizedMessage}")
                logError("Profile loading error", e)
            }
        }
    }

    // Yenileme için - Mevcut state'i korur, arka planda günceller
    fun refreshUserProfile() {
        // Eğer halihazırda loading state'indeyse, işlemi tekrarlama
        if (_profileState.value is ProfileState.Loading) return

        viewModelScope.launch {
            try {
                // İnternet bağlantısı kontrolü - sessizce başarısız ol
                if (!networkUtil.isNetworkAvailable()) {
                    return@launch
                }

                // Paralel asenkron işlemler ile hem kullanıcı verilerini hem de şehir verilerini alalım
                coroutineScope {
                    // Kullanıcının temel verilerini al
                    val userDeferred = async { userRepository.getCurrentUser().first() }

                    // Kullanıcının ziyaret ettiği şehirleri al
                    val visitedCitiesDeferred = async { userRepository.getUserVisitedCities().first() }

                    // Kullanıcının bucket list'ini al
                    val wishlistDeferred = async { userRepository.getUserWishlist().first() }

                    // Tüm asenkron işlemlerin tamamlanmasını bekle
                    val user = userDeferred.await() ?: return@coroutineScope

                    val visitedCitiesMap = visitedCitiesDeferred.await()
                    val wishlistIds = wishlistDeferred.await()

                    // Ziyaret edilen şehirler için detaylı verileri getir
                    val visitedCities = fetchCityDetails(visitedCitiesMap)

                    // Wishlist için detaylı verileri getir
                    val wishlistCities = fetchWishlistCityDetails(wishlistIds)

                    // Başarılı durumu güncelle
                    _profileState.value = ProfileState.Success(
                        displayName = user.displayName.ifEmpty { "Traveler" },
                        photoUrl = user.photoUrl,
                        visitedCities = visitedCities,
                        wishlistCities = wishlistCities
                    )
                }
            } catch (e: Exception) {
                // Sessizce başarısız ol, UI'ı bozma
                logError("Profile refresh error", e)
            }
        }
    }

    // Ziyaret edilen şehirler için detayları getir
    private suspend fun fetchCityDetails(
        visitedCitiesMap: Map<String, Double>
    ): List<VisitedCityItem> = withContext(Dispatchers.Default) {
        // Performans için paralel sorgular
        val deferredCities = visitedCitiesMap.map { (cityId, rating) ->
            async {
                try {
                    // Cache'den kontrol et
                    val cachedCity = cityCache[cityId]
                    if (cachedCity != null) {
                        createVisitedCityItem(cachedCity, rating)
                    } else {
                        // Cache'de yoksa, API'den getir
                        val city = cityRepository.getCityById(cityId).first()
                        if (city != null) {
                            cityCache[cityId] = city
                            createVisitedCityItem(city, rating)
                        } else null
                    }
                } catch (e: Exception) {
                    logError("Error fetching city details: $cityId", e)
                    null
                }
            }
        }

        // Tüm asenkron işlemlerin sonuçlarını topla
        val results = deferredCities.awaitAll()

        // Null olmayanları filtrele ve puana göre sırala
        results.filterNotNull()
            .sortedByDescending { it.userRating }
            .mapIndexed { index, item -> item.copy(position = index + 1) }
    }

    // Wishlist şehirleri için detayları getir
    private suspend fun fetchWishlistCityDetails(
        wishlistIds: List<String>
    ): List<WishlistCityItem> = withContext(Dispatchers.Default) {
        // Performans için paralel sorgular
        val deferredCities = wishlistIds.map { cityId ->
            async {
                try {
                    // Cache'den kontrol et
                    val cachedCity = cityCache[cityId]
                    if (cachedCity != null) {
                        createWishlistCityItem(cachedCity)
                    } else {
                        // Cache'de yoksa, API'den getir
                        val city = cityRepository.getCityById(cityId).first()
                        if (city != null) {
                            cityCache[cityId] = city
                            createWishlistCityItem(city)
                        } else null
                    }
                } catch (e: Exception) {
                    logError("Error fetching wishlist city: $cityId", e)
                    null
                }
            }
        }

        // Tüm asenkron işlemlerin sonuçlarını topla
        val results = deferredCities.awaitAll()

        // Null olmayanları filtrele ve ada göre sırala
        results.filterNotNull()
            .sortedBy { it.name }
    }

    // Helper method to create VisitedCityItem
    private fun createVisitedCityItem(city: CityModel, rating: Double): VisitedCityItem {
        return VisitedCityItem(
            id = city.id,
            name = city.cityName,
            country = city.country,
            flagUrl = city.flagUrl,
            userRating = rating
        )
    }

    // Helper method to create WishlistCityItem
    private fun createWishlistCityItem(city: CityModel): WishlistCityItem {
        return WishlistCityItem(
            id = city.id,
            name = city.cityName,
            country = city.country,
            flagUrl = city.flagUrl
        )
    }

    // Paylaşım fonksiyonları
    fun shareVisitedCities() {
        viewModelScope.launch {
            val currentState = _profileState.value
            if (currentState !is ProfileState.Success || currentState.visitedCities.isEmpty()) {
                emitEvent(UiEvent.Error("No cities to share"))
                return@launch
            }

            try {
                // ImageUtils sınıfını kullanarak bitmap oluştur ve intent al
                val cities = currentState.visitedCities
                val bitmap = imageUtils.createVisitedCitiesImage(cities, cities.size)
                val intent = imageUtils.createShareImageIntent(bitmap)

                // Intent'i Fragment'a ilet
                _shareIntent.emit(intent)
            } catch (e: Exception) {
                logError("Error creating share image", e)
                emitEvent(UiEvent.Error("Failed to create image: ${e.localizedMessage}"))
            }
        }
    }

    fun shareWishlist() {
        viewModelScope.launch {
            val currentState = _profileState.value
            if (currentState !is ProfileState.Success || currentState.wishlistCities.isEmpty()) {
                emitEvent(UiEvent.Error("No cities in your bucket list to share"))
                return@launch
            }

            try {
                val cities = currentState.wishlistCities
                val shareText = buildString {
                    append("My city bucket list on UrbanRate:\n\n")
                    cities.forEachIndexed { index, city ->
                        append("${index + 1}. ${city.name}, ${city.country}\n")
                    }
                    append("\nDownload UrbanRate to create your own bucket list!")
                }

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "My UrbanRate Bucket List")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                _shareIntent.emit(intent)
            } catch (e: Exception) {
                logError("Error sharing wishlist", e)
                emitEvent(UiEvent.Error("Failed to share: ${e.localizedMessage}"))
            }
        }
    }

    fun removeFromWishlist(cityId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.removeFromWishlist(cityId)
                result.fold(
                    onSuccess = {
                        emitEvent(UiEvent.Success("City removed from bucket list"))
                    },
                    onFailure = { e ->
                        logError("Error removing from wishlist", e)
                        emitEvent(UiEvent.Error("Failed to remove: ${e.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                logError("Error removing from wishlist", e)
                emitEvent(UiEvent.Error("Failed to remove: ${e.localizedMessage}"))
            }
        }
    }

    fun requestAccountDeletion(reason: String? = null) {
        viewModelScope.launch {
            try {
                val result = userRepository.requestAccountDeletion(reason)
                result.fold(
                    onSuccess = {
                        emitEvent(UiEvent.Success("Account deletion request received. Your account will be processed soon."))
                    },
                    onFailure = { e ->
                        emitEvent(UiEvent.Error("Failed to submit deletion request: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                emitEvent(UiEvent.Error("Error: ${e.message}"))
            }
        }
    }

    // Hata ayıklama için yardımcı fonksiyon
    private fun logError(message: String, error: Throwable) {
        Log.e(TAG, message, error)
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}