package dev.ruizj.appnasa.apod

import android.text.Html
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.ruizj.appnasa.R
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun ImagesFeature(viewModel: ApodViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "apod/list", modifier = modifier) {
        composable("apod/list") {
            ApodListScreen(
                title = stringResource(R.string.images_title),
                items = state.items,
                favorites = state.favorites,
                isLoading = state.isLoading,
                errorMessage = state.apodErrorMessage,
                incompleteRecords = state.incompleteRecords,
                onRefresh = viewModel::refresh,
                onShowFavorites = { navController.navigate("apod/favorites") },
                onSelectItem = { item -> navController.navigate("apod/detail/${item.date}") },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
        composable("apod/favorites") {
            ApodListScreen(
                title = stringResource(R.string.favorites_title),
                items = state.favorites,
                favorites = state.favorites,
                isLoading = state.isFavoritesLoading,
                errorMessage = state.favoritesErrorMessage,
                incompleteRecords = 0,
                onRefresh = null,
                onShowFavorites = null,
                onSelectItem = { item -> navController.navigate("apod/detail/${item.date}") },
                onToggleFavorite = viewModel::toggleFavorite,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "apod/detail/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { entry ->
            val date = entry.arguments?.getString("date")
            val item = state.items.firstOrNull { it.date == date }
                ?: state.favorites.firstOrNull { it.date == date }
            if (item != null) {
                ApodDetailScreen(
                    item = item,
                    isFavorite = state.favorites.any { it.date == item.date },
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                    errorMessage = state.favoritesErrorMessage,
                )
            }
        }
    }
}

@Composable
private fun ApodListScreen(
    title: String,
    items: List<ApodItem>,
    favorites: List<ApodItem>,
    isLoading: Boolean,
    errorMessage: String?,
    incompleteRecords: Int,
    onRefresh: (() -> Unit)?,
    onShowFavorites: (() -> Unit)?,
    onSelectItem: (ApodItem) -> Unit,
    onToggleFavorite: (ApodItem) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val backDescription = stringResource(R.string.back_to_images)
    val refreshDescription = stringResource(R.string.refresh)
    val favoritesDescription = stringResource(R.string.open_favorites)

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
                    modifier = Modifier.semantics {
                        contentDescription = backDescription
                    },
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
                    modifier = Modifier.semantics {
                        contentDescription = refreshDescription
                    },
                ) {
                    Text(
                        text = "↻",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (onShowFavorites != null) {
                IconButton(
                    onClick = onShowFavorites,
                    modifier = Modifier.semantics {
                        contentDescription = favoritesDescription
                    },
                ) {
                    Text(
                        text = stringResource(R.string.favorite_icon),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (errorMessage != null) {
            Text(
                text = stringResource(
                    if (onBack != null) {
                        R.string.favorites_load_error
                    } else {
                        R.string.apod_load_error
                    },
                    errorMessage,
                ),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (incompleteRecords > 0) {
            Text(
                text = stringResource(R.string.apod_incomplete_records, incompleteRecords),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when {
            isLoading && items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            items.isEmpty() && isLoading.not() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (onBack != null) {
                        stringResource(R.string.no_favorites)
                    } else {
                        stringResource(R.string.no_images)
                    },
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
                items(items, key = ApodItem::date) { item ->
                    ApodListItem(
                        item = item,
                        isFavorite = favorites.any { it.date == item.date },
                        onClick = { onSelectItem(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApodListItem(
    item: ApodItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Column {
            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.video_content),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.image_unavailable))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
            }
        }
    }
}

@Composable
private fun ApodDetailScreen(
    item: ApodItem,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    errorMessage: String?,
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
                text = stringResource(R.string.back),
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
                    text = stringResource(R.string.favorites_load_error, errorMessage),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.video_content),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            } else if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.image_unavailable))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = item.explanation,
                style = MaterialTheme.typography.bodyLarge,
            )
            item.credit?.let { credit ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.credit,
                        Html.fromHtml(credit, Html.FROM_HTML_MODE_COMPACT).toString().trim(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val description = stringResource(
        if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = description
        },
    ) {
        Text(
            text = stringResource(
                if (isFavorite) R.string.favorite_icon else R.string.not_favorite_icon,
            ),
            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
