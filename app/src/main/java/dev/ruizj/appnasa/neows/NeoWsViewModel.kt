package dev.ruizj.appnasa.neows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ruizj.appnasa.NasaApplication
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NeoWsUiState(
    val asteroids: List<NearEarthObject> = emptyList(),
    val favorites: List<NearEarthObject> = emptyList(),
    val isLoading: Boolean = false,
    val isFavoritesLoading: Boolean = false,
    val showHazardousOnly: Boolean = false,
    val incompleteRecords: Int = 0,
    val feedErrorMessage: String? = null,
    val favoritesErrorMessage: String? = null,
)

class NeoWsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as NasaApplication).neoWsRepository
    private val mutableUiState = MutableStateFlow(NeoWsUiState())
    val uiState = mutableUiState.asStateFlow()
    private var lastLoadedDate: LocalDate? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(repository.observeItems(), repository.observeFavorites()) { items, favorites ->
                items to favorites
            }.collect { (items, favorites) ->
                mutableUiState.update { it.copy(asteroids = items, favorites = favorites) }
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
                val incompleteRecords = withContext(Dispatchers.IO) {
                    repository.refresh(startDate, endDate)
                }
                mutableUiState.update {
                    it.copy(incompleteRecords = incompleteRecords, isLoading = false)
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
                repository.toggleFavorite(asteroid.id)
                mutableUiState.update { it.copy(favoritesErrorMessage = null) }
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
}
