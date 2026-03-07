package com.ghost.krop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ghost.krop.ui.screen.ImageScreen

@Composable
@Preview
fun App() {
    Scaffold { contentPadding ->
        ImageScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
    }

}