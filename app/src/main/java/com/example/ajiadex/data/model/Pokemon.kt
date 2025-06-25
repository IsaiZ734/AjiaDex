package com.example.ajiadex.data.model

data class Pokemon(
    val id: Int, // You can get it from the URL or if the API provides it directly
    val name: String,
    val imageUrl: String,
    var isCaptured: Boolean = false // For the checkbox
)

// Classes to parse the response from the PokeAPI API (example)
// The exact structure will depend on the response of the API you use.
// This is a common structure for the list of Pokémon from PokeAPI.
data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokemonResult>
)

data class PokemonResult(
    val name: String,
    val url: String // URL to get Pokémon details, including the image
)

// Class for the details of a specific Pokémon (to get the image URL)
data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val sprites: PokemonSprites
)

data class PokemonSprites(
    val front_default: String // URL of the front image
)