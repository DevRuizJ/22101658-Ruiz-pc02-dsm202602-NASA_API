package dev.ruizj.appnasa.apod

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

data class ApodUiState(
    val items: List<ApodItem> = emptyList(),
    val favorites: List<ApodItem> = emptyList(),
    val isLoading: Boolean = false,
    val isFavoritesLoading: Boolean = false,
    val incompleteRecords: Int = 0,
    val apodErrorMessage: String? = null,
    val favoritesErrorMessage: String? = null,
)

class ApodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as NasaApplication).apodRepository
    private val mutableUiState = MutableStateFlow(ApodUiState())
    val uiState = mutableUiState.asStateFlow()
    private var lastLoadedDate: LocalDate? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(repository.observeItems(), repository.observeFavorites()) { items, favorites ->
                items to favorites
            }.collect { (items, favorites) ->
                mutableUiState.update { it.copy(items = items, favorites = favorites) }
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
            mutableUiState.update { it.copy(isLoading = true, apodErrorMessage = null) }
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
                        apodErrorMessage = error.message ?: "Unknown NASA APOD error.",
                    )
                }
            }
        }
    }

    fun toggleFavorite(item: ApodItem) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(item.date)
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
