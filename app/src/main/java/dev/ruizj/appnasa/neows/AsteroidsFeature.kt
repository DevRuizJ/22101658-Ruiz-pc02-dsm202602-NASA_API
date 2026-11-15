package dev.ruizj.appnasa.neows

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ruizj.appnasa.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private enum class AsteroidsScreen {
    List,
    Favorites,
    Detail,
}

@Composable
fun AsteroidsFeature(viewModel: NeoWsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var screen by rememberSaveable { mutableStateOf(AsteroidsScreen.List) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAsteroid by remember { mutableStateOf<NearEarthObject?>(null) }
    var detailFromFavorites by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = screen != AsteroidsScreen.List) {
        screen = when (screen) {
            AsteroidsScreen.Detail -> if (detailFromFavorites) {
                AsteroidsScreen.Favorites
            } else {
                AsteroidsScreen.List
            }

            AsteroidsScreen.Favorites -> AsteroidsScreen.List
            AsteroidsScreen.List -> AsteroidsScreen.List
        }
    }

    when (screen) {
        AsteroidsScreen.List -> AsteroidListScreen(
            title = stringResource(R.string.asteroids_title),
            asteroids = state.asteroids.filterHazardousOnly(state.showHazardousOnly),
            favorites = state.favorites,
            isLoading = state.isLoading,
            isFavoritesLoading = state.isFavoritesLoading,
            errorMessage = state.feedErrorMessage,
            favoritesErrorMessage = state.favoritesErrorMessage,
            incompleteRecords = state.incompleteRecords,
            showHazardousFilter = true,
            hazardousOnly = state.showHazardousOnly,
            onHazardousOnlyChanged = viewModel::setHazardousOnly,
            onRefresh = viewModel::refresh,
            onShowFavorites = { screen = AsteroidsScreen.Favorites },
            onSelectAsteroid = { asteroid ->
                selectedId = asteroid.id
                selectedAsteroid = asteroid
                detailFromFavorites = false
                screen = AsteroidsScreen.Detail
            },
            onToggleFavorite = viewModel::toggleFavorite,
            modifier = modifier,
        )

        AsteroidsScreen.Favorites -> AsteroidListScreen(
            title = stringResource(R.string.my_asteroids_title),
            asteroids = state.favorites,
            favorites = state.favorites,
            isLoading = state.isFavoritesLoading,
            isFavoritesLoading = state.isFavoritesLoading,
            errorMessage = state.favoritesErrorMessage,
            favoritesErrorMessage = null,
            incompleteRecords = 0,
            showHazardousFilter = false,
            hazardousOnly = false,
            onHazardousOnlyChanged = {},
            onRefresh = null,
            onShowFavorites = null,
            onSelectAsteroid = { asteroid ->
                selectedId = asteroid.id
                selectedAsteroid = asteroid
                detailFromFavorites = true
                screen = AsteroidsScreen.Detail
            },
            onToggleFavorite = viewModel::toggleFavorite,
            onBack = { screen = AsteroidsScreen.List },
            modifier = modifier,
        )

        AsteroidsScreen.Detail -> {
            val asteroid = selectedAsteroid?.takeIf { it.id == selectedId }
                ?: state.asteroids.firstOrNull { it.id == selectedId }
                ?: state.favorites.firstOrNull { it.id == selectedId }
            if (asteroid != null) {
                AsteroidDetailScreen(
                    asteroid = asteroid,
                    isFavorite = state.favorites.any { it.id == asteroid.id },
                    errorMessage = state.favoritesErrorMessage,
                    onBack = {
                        screen = if (detailFromFavorites) {
                            AsteroidsScreen.Favorites
                        } else {
                            AsteroidsScreen.List
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(asteroid) },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun AsteroidListScreen(
    title: String,
    asteroids: List<NearEarthObject>,
    favorites: List<NearEarthObject>,
    isLoading: Boolean,
    isFavoritesLoading: Boolean,
    errorMessage: String?,
    favoritesErrorMessage: String?,
    incompleteRecords: Int,
    showHazardousFilter: Boolean,
    hazardousOnly: Boolean,
    onHazardousOnlyChanged: (Boolean) -> Unit,
    onRefresh: (() -> Unit)?,
    onShowFavorites: (() -> Unit)?,
    onSelectAsteroid: (NearEarthObject) -> Unit,
    onToggleFavorite: (NearEarthObject) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val backDescription = stringResource(R.string.back)
    val refreshDescription = stringResource(R.string.refresh_asteroids)
    val favoritesDescription = stringResource(R.string.open_asteroid_favorites)
    val hazardousFilterDescription = stringResource(R.string.hazardous_filter)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = backDescription },
                ) {
                    Text(text = "‹", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (onRefresh != null) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.semantics { contentDescription = refreshDescription },
                ) {
                    Text(text = "↻", style = MaterialTheme.typography.titleLarge)
                }
            }
            if (onShowFavorites != null) {
                IconButton(
                    onClick = onShowFavorites,
                    modifier = Modifier.semantics { contentDescription = favoritesDescription },
                ) {
                    Text(
                        text = stringResource(R.string.not_favorite_icon),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (showHazardousFilter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hazardous_filter),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = hazardousOnly,
                    onCheckedChange = onHazardousOnlyChanged,
                    modifier = Modifier.semantics {
                        contentDescription = hazardousFilterDescription
                    },
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = stringResource(
                    if (onBack != null) R.string.asteroid_favorites_error else R.string.neows_load_error,
                    errorMessage,
                ),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (favoritesErrorMessage != null) {
            Text(
                text = stringResource(R.string.asteroid_favorites_error, favoritesErrorMessage),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (incompleteRecords > 0) {
            Text(
                text = stringResource(R.string.neows_incomplete_records, incompleteRecords),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when {
            isLoading && asteroids.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            isFavoritesLoading && asteroids.isEmpty() && onBack != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            asteroids.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (showHazardousFilter && hazardousOnly) {
                            R.string.no_hazardous_asteroids
                        } else if (onBack != null) {
                            R.string.no_asteroid_favorites
                        } else {
                            R.string.no_asteroids
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    asteroids,
                    key = { asteroid -> "${asteroid.id}-${asteroid.closeApproachDate}" },
                ) { asteroid ->
                    AsteroidListItem(
                        asteroid = asteroid,
                        isFavorite = favorites.any { it.id == asteroid.id },
                        onClick = { onSelectAsteroid(asteroid) },
                        onToggleFavorite = { onToggleFavorite(asteroid) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AsteroidListItem(
    asteroid: NearEarthObject,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = asteroid.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (asteroid.isPotentiallyHazardous == true) {
                    HazardousBadge()
                }
                FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
            }
            Text(
                text = stringResource(R.string.approach_date, asteroid.closeApproachDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.asteroid_list_measurements,
                    formatMeasurement(asteroid.estimatedDiameterMaxMeters, R.string.meters_value),
                    formatMeasurement(
                        asteroid.relativeVelocityKph,
                        R.string.kilometers_per_hour_value,
                    ),
                    formatMeasurement(asteroid.missDistanceKm, R.string.kilometers_value),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AsteroidDetailScreen(
    asteroid: NearEarthObject,
    isFavorite: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDescription = stringResource(R.string.back)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = backDescription },
            ) {
                Text(text = "‹", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = stringResource(R.string.asteroid_detail_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (errorMessage != null) {
                Text(
                    text = stringResource(R.string.asteroid_favorites_error, errorMessage),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = asteroid.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (asteroid.isPotentiallyHazardous == true) {
                Spacer(modifier = Modifier.height(8.dp))
                HazardousBadge()
            }
            Spacer(modifier = Modifier.height(16.dp))
            DetailField(stringResource(R.string.asteroid_id), asteroid.id)
            DetailField(
                stringResource(R.string.approach_date_label),
                asteroid.closeApproachDate,
            )
            DetailField(
                stringResource(R.string.diameter_min),
                formatMeasurement(asteroid.estimatedDiameterMinMeters, R.string.meters_value),
            )
            DetailField(
                stringResource(R.string.diameter_max),
                formatMeasurement(asteroid.estimatedDiameterMaxMeters, R.string.meters_value),
            )
            DetailField(
                stringResource(R.string.relative_velocity),
                formatMeasurement(
                    asteroid.relativeVelocityKph,
                    R.string.kilometers_per_hour_value,
                ),
            )
            DetailField(
                stringResource(R.string.miss_distance),
                formatMeasurement(asteroid.missDistanceKm, R.string.kilometers_value),
            )
            DetailField(
                stringResource(R.string.hazardous_status),
                stringResource(
                    if (asteroid.isPotentiallyHazardous == true) {
                        R.string.potentially_hazardous
                    } else if (asteroid.isPotentiallyHazardous == false) {
                        R.string.not_potentially_hazardous
                    } else {
                        R.string.unavailable
                    },
                ),
            )
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HazardousBadge() {
    Text(
        text = stringResource(R.string.potentially_hazardous),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val description = stringResource(
        if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(
                if (isFavorite) R.string.favorite_icon else R.string.not_favorite_icon,
            ),
            color = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

internal fun formatNumber(value: Double?, unavailableValue: String = "No disponible"): String {
    if (value == null) return unavailableValue
    val formatter = DecimalFormat(
        "0.##",
        DecimalFormatSymbols.getInstance(Locale.US),
    ).apply {
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
    }

    return formatter.format(value)
}

@Composable
private fun formatMeasurement(value: Double?, unitString: Int): String =
    if (value == null) {
        stringResource(R.string.unavailable)
    } else {
        stringResource(unitString, formatNumber(value))
    }
