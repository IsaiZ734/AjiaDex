package com.example.ajiadex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ajiadex.data.model.Pokemon
import com.example.ajiadex.data.remote.PokemonApiService // Import the interface
import com.example.ajiadex.data.remote.RetrofitInstance // You might need this for the default value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PokedexViewModel(private val pokemonApiService: PokemonApiService) : ViewModel() {

    private val _pokemonList = MutableStateFlow<List<Pokemon>>(emptyList())
    val pokemonList: StateFlow<List<Pokemon>> = _pokemonList.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 0
    private val itemsPerPage = 20

    init {
        fetchPokemonList() // Now fetchPokemonList will use the injected `pokemonApiService`
    }

    fun fetchPokemonList(loadMore: Boolean = false) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val offset = if (loadMore) currentPage * itemsPerPage else 0
                if (!loadMore) {
                    _pokemonList.value = emptyList()
                    currentPage = 0
                }

                // USE THE INJECTED pokemonApiService
                val response = pokemonApiService.getPokemonList(limit = itemsPerPage, offset = offset)
                val detailedPokemonList = response.results.mapNotNull { result ->
                    try {
                        // USE THE INJECTED pokemonApiService
                        val detailResponse = pokemonApiService.getPokemonDetail(result.name)
                        Pokemon(
                            id = detailResponse.id,
                            name = detailResponse.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            imageUrl = detailResponse.sprites.front_default,
                            isCaptured = false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (loadMore) {
                    _pokemonList.update { currentList -> currentList + detailedPokemonList }
                } else {
                    _pokemonList.value = detailedPokemonList
                }
                currentPage++

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    fun togglePokemonCaptured(pokemonId: Int) {
        _pokemonList.update { currentList ->
            currentList.map { pokemon ->
                if (pokemon.id == pokemonId) {
                    pokemon.copy(isCaptured = !pokemon.isCaptured)
                } else {
                    pokemon
                }
            }
        }
    }

    val filteredPokemonList: StateFlow<List<Pokemon>> = combine(
        pokemonList,
        searchText
    ) { list, text ->
        if (text.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(text, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
}