package com.example.ajiadex.data.remote

import com.example.ajiadex.data.model.PokemonDetailResponse
import com.example.ajiadex.data.model.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokemonApiService {
    // Get a list of Pokémon with pagination
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 20, // How many Pokémon per page
        @Query("offset") offset: Int = 0  // From which Pokémon to start
    ): PokemonListResponse

    // Get details of a specific Pokémon by its name or ID
    @GET("pokemon/{name}")
    suspend fun getPokemonDetail(@Path("name") name: String): PokemonDetailResponse
}
