package io.github.jude.navermap.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jude.navermap.compose.NaverMapPlaceholder

@Composable
fun App() {
    var showGreetingDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Compose Multiplatform Sample",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "This sample app is shared across Android and iOS and already depends on the KMP map library module.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { showGreetingDialog = true }) {
                    Text("Show greeting")
                }
                NaverMapPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    subtitle = "Next step: replace this placeholder with the real Android and iOS NAVER Map views.",
                )
            }
        }
    }

    if (showGreetingDialog) {
        AlertDialog(
            onDismissRequest = { showGreetingDialog = false },
            confirmButton = {
                Button(onClick = { showGreetingDialog = false }) {
                    Text("OK")
                }
            },
            title = {
                Text("Greeting")
            },
            text = {
                Text("Hi Jude")
            },
        )
    }
}
