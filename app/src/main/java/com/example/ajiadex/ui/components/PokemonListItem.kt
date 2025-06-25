package com.example.ajiadex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ajiadex.data.model.Pokemon

@Composable
fun PokemonListItem(
    pokemon: Pokemon,
    onPokemonClicked: () -> Unit, // For future interactions (e.g., go to detail screen)
    onCapturedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onPokemonClicked() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(pokemon.imageUrl)
                    .crossfade(true) // Smooth animation when loading the image
                    .build(),
                contentDescription = pokemon.name,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f) // So that the text occupies the available space
            )
            Checkbox(
                checked = pokemon.isCaptured,
                onCheckedChange = onCapturedChanged,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}