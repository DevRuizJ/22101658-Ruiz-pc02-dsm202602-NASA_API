package dev.ruizj.appnasa.neows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class NeoWsUiState(
    val asteroids: List<NearEarthObject> = emptyList(),
    val favorites: List<NearEarthObject> = emptyList(),
    val isLoading: Boolean = false,
    val isFavoritesLoading: Boolean = true,
    val showHazardousOnly: Boolean = false,
    val incompleteRecords: Int = 0,
    val feedErrorMessage: String? = null,
    val favoritesErrorMessage: String? = null,
)

class NeoWsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NeoWsRepository()
    private val favoritesDatabase = NeoWsFavoritesDatabase(application)
    private val favoritesMutex = Mutex()
    private val mutableUiState = MutableStateFlow(NeoWsUiState())
    val uiState = mutableUiState.asStateFlow()
    private var lastLoadedDate: LocalDate? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                favoritesMutex.withLock {
                    val favorites = withContext(Dispatchers.IO) {
                        favoritesDatabase.getFavorites()
                    }
                    mutableUiState.update {
                        it.copy(favorites = favorites, isFavoritesLoading = false)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update {
                    it.copy(
                        isFavoritesLoading = false,
                        favoritesErrorMessage = error.message ?: "Unknown database error.",
                    )
                }
            }
        }
    }

    fun refreshIfNeeded() {
        if (lastLoadedDate != LocalDate.now()) refresh()
    }

    fun refresh() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        lastLoadedDate = endDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, feedErrorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.fetch(startDate, endDate)
                }
                mutableUiState.update {
                    it.copy(
                        asteroids = result.items,
                        incompleteRecords = result.incompleteRecords,
                        isLoading = false,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastLoadedDate = null
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        feedErrorMessage = error.message ?: "Unknown NASA NeoWs error.",
                    )
                }
            }
        }
    }

    fun setHazardousOnly(enabled: Boolean) {
        mutableUiState.update { it.copy(showHazardousOnly = enabled) }
    }

    fun toggleFavorite(asteroid: NearEarthObject) {
        viewModelScope.launch {
            try {
                favoritesMutex.withLock {
                    val favorites = withContext(Dispatchers.IO) {
                        if (mutableUiState.value.favorites.any { it.id == asteroid.id }) {
                            favoritesDatabase.removeFavorite(asteroid.id)
                        } else {
                            favoritesDatabase.saveFavorite(asteroid)
                        }
                        favoritesDatabase.getFavorites()
                    }
                    mutableUiState.update {
                        it.copy(favorites = favorites, favoritesErrorMessage = null)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableUiState.update {
                    it.copy(
                        favoritesErrorMessage = error.message
                            ?: "Unknown favorites database error.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        favoritesDatabase.close()
        super.onCleared()
    }
}
