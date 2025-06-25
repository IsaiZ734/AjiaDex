package com.example.ajiadex.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ajiadex.data.remote.PokemonApiService
import com.example.ajiadex.data.remote.RetrofitInstance
import com.example.ajiadex.ui.viewmodel.PokedexViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ajiadex.ui.components.PokemonListItem
import com.example.ajiadex.ui.components.SearchBar
import kotlinx.coroutines.flow.collectLatest

// This is the Factory
class PokedexViewModelFactory(private val apiService: PokemonApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PokedexViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PokedexViewModel(apiService) as T // Here you pass the dependency
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
fun PokedexScreen(
    modifier: Modifier = Modifier
    // No more: pokedexViewModel: PokedexViewModel = viewModel()
) {
    // This is how the ViewModel is created using the Factory:
    val pokedexViewModel: PokedexViewModel = viewModel(
        factory = PokedexViewModelFactory(RetrofitInstance.api)
    )

    // The rest of your Composable remains the same:
    val pokemonList by pokedexViewModel.filteredPokemonList.collectAsState()
    val searchText by pokedexViewModel.searchText.collectAsState()
    val isLoading by pokedexViewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()

    // For infinite loading (load more when reaching the end)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collectLatest { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItemIndex = visibleItems.last().index
                    val totalItemCount = listState.layoutInfo.totalItemsCount
                    // Load more if the last visible item is near the end AND not already loading
                    if (lastVisibleItemIndex >= totalItemCount - 5 && !isLoading) {
                        pokedexViewModel.fetchPokemonList(loadMore = true)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { pokedexViewModel.onSearchTextChanged(it) }
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading && pokemonList.isEmpty()) { // Show CircularProgressIndicator only on initial load
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pokemonList.isEmpty() && !isLoading && searchText.isNotEmpty()) {
                Text(
                    text = "No Pokémon found with that name.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = pokemonList,
                        key = { pokemon -> pokemon.id } // Unique key for each item
                    ) { pokemon ->
                        PokemonListItem(
                            pokemon = pokemon,
                            onPokemonClicked = {
                                // Here you can navigate to a Pokémon details screen
                                // For now, we leave it empty
                            },
                            onCapturedChanged = { isCaptured ->
                                pokedexViewModel.togglePokemonCaptured(pokemon.id)
                            }
                        )
                    }
                    // Loading indicator at the end of the list if more items are being loaded
                    if (isLoading && pokemonList.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // The "fast scroller" is a bit more complex
            // to implement from scratch in LazyColumn natively like in RecyclerView.
            // There are third-party libraries that facilitate this, or you can implement a simplified version.
            // For now, we will focus on the other functionalities.
            // If the list is very long, consider implementing pagination or a "Scroll to Top" button.
        }
    }
}