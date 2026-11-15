package dev.ruizj.appnasa.apod

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
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class ApodUiState(
    val items: List<ApodItem> = emptyList(),
    val favorites: List<ApodItem> = emptyList(),
    val isLoading: Boolean = false,
    val isFavoritesLoading: Boolean = true,
    val incompleteRecords: Int = 0,
    val apodErrorMessage: String? = null,
    val favoritesErrorMessage: String? = null,
)

class ApodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ApodRepository()
    private val favoritesDatabase = ApodFavoritesDatabase(application)
    private val mutableUiState = MutableStateFlow(ApodUiState())
    val uiState = mutableUiState.asStateFlow()
    private var lastLoadedDate: LocalDate? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val favorites = withContext(Dispatchers.IO) {
                    favoritesDatabase.getFavorites()
                }
                mutableUiState.update {
                    it.copy(favorites = favorites, isFavoritesLoading = false)
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
        if (lastLoadedDate != LocalDate.now()) {
            refresh()
        }
    }

    fun refresh() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        lastLoadedDate = endDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, apodErrorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.fetch(startDate, endDate)
                }
                mutableUiState.update {
                    it.copy(
                        items = result.items,
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
                        apodErrorMessage = error.message ?: "Unknown NASA APOD error.",
                    )
                }
            }
        }
    }

    fun toggleFavorite(item: ApodItem) {
        viewModelScope.launch {
            try {
                val favorites = withContext(Dispatchers.IO) {
                    if (mutableUiState.value.favorites.any { it.date == item.date }) {
                        favoritesDatabase.removeFavorite(item.date)
                    } else {
                        favoritesDatabase.saveFavorite(item)
                    }
                    favoritesDatabase.getFavorites()
                }
                mutableUiState.update {
                    it.copy(favorites = favorites, favoritesErrorMessage = null)
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
