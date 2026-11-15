package dev.ruizj.appnasa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.ruizj.appnasa.ui.theme.AppNASATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNASATheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.Images) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedSection == AppSection.Images,
                    onClick = { selectedSection = AppSection.Images },
                    icon = { Text(text = stringResource(R.string.images_icon)) },
                    label = { Text(text = stringResource(R.string.images_tab)) },
                )
                NavigationBarItem(
                    selected = selectedSection == AppSection.Asteroids,
                    onClick = { selectedSection = AppSection.Asteroids },
                    icon = { Text(text = stringResource(R.string.asteroids_icon)) },
                    label = { Text(text = stringResource(R.string.asteroids_tab)) },
                )
            }
        },
    ) { innerPadding ->
        SectionPlaceholder(
            title = stringResource(selectedSection.titleRes),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun SectionPlaceholder(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title)
    }
}

private enum class AppSection(val titleRes: Int) {
    Images(R.string.images_tab),
    Asteroids(R.string.asteroids_tab),
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationPreview() {
    AppNASATheme {
        AppNavigation()
    }
}