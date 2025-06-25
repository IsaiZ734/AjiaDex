package com.example.ajiadex

import android.util.Log
import com.example.ajiadex.data.model.Pokemon
import com.example.ajiadex.data.model.PokemonDetailResponse
import com.example.ajiadex.data.model.PokemonListResponse
import com.example.ajiadex.data.model.PokemonResult
import com.example.ajiadex.data.model.PokemonSprites
import com.example.ajiadex.data.remote.PokemonApiService
import com.example.ajiadex.ui.viewmodel.PokedexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn // More explicit for suspend functions
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub // For stubbing in @Before
import org.mockito.kotlin.whenever // Still useful

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    // --- Original tests ---
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun addition_inCorrect() {
        assertNotEquals(3, 2 + 2)
    }

    // --- Start of tests for PokedexViewModel ---

    private lateinit var viewModel: PokedexViewModel
    private lateinit var mockApiService: PokemonApiService
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakePokemonResult1 = PokemonResult("bulbasaur", "url1")
    private val fakePokemonResult2 = PokemonResult("ivysaur", "url2")
    private val fakePokemonListResponse = PokemonListResponse(2, null, null, listOf(fakePokemonResult1, fakePokemonResult2))
    private val emptyPokemonListResponse = PokemonListResponse(0, null, null, emptyList()) // For @Before

    private val fakeBulbasaurDetail = PokemonDetailResponse(1, "bulbasaur", PokemonSprites("image_url_bulbasaur"))
    private val fakeIvysaurDetail = PokemonDetailResponse(2, "ivysaur", PokemonSprites("image_url_ivysaur"))

    private val expectedPokemon1 = Pokemon(1, "Bulbasaur", "image_url_bulbasaur", false)
    private val expectedPokemon2 = Pokemon(2, "Ivysaur", "image_url_ivysaur", false)

    @Before
    fun setUpViewModelTests() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mock()

        // *** CRUCIAL CHANGE HERE ***
        // Set up base behavior for calls occurring in the ViewModel's init block.
        // This prevents NPE when the ViewModel is instantiated.
        // We use 'stub' for setup in @Before, it's a good practice.
        // For suspend functions, `doReturn` inside `runTest` or `coWhenever` are options,
        // but for @Before which is not a coroutine, stubbing like this works for the mock.
        // If getPokemonList is not suspend, `whenever(...).thenReturn(...)` is fine.
        // If getPokemonList IS suspend, stubbing becomes more complex for @Before
        // and it's better to handle it with coWhenever and runBlockingTest (deprecated) or ensure
        // that the testDispatcher is already active.
        // With UnconfinedTestDispatcher, the coroutine in init will run, so we need the mock.

        // Assuming `getPokemonList` and `getPokemonDetail` are suspend functions:
        // We need a way for the mock to respond during the ViewModel init.
        // `stub` is good for configuring the initial mock state.
        mockApiService.stub {
            // Behavior for getPokemonList call that happens in init.
            // If init calls with specific parameters (e.g. offset 0), mock that.
            // Here we use `any()` to cover the initial call.
            // onBlocking { getPokemonList(any(), any()) } doReturn emptyPokemonListResponse // Or an empty list to start safely
            // onBlocking { getPokemonDetail(any()) } doReturn fakeBulbasaurDetail // A generic detail if needed
            // Since API functions are suspend, mocking them outside runTest is tricky.
            // The simplest solution is to ensure EACH test configures the mocks it NEEDS
            // BEFORE the ViewModel logic depending on those mocks executes.
            // For init, this means the ViewModel is created AFTER the whenever.
            //
            // WE WILL LEAVE THIS and let each test configure what it needs, and @Before just creates the mock.
            // viewModel = PokedexViewModel(mockApiService) // WE WILL MOVE this or adjust it
        }
        // ViewModel creation will be done in each test or after a general mock setup in @Before if possible
    }

    @After
    fun tearDownViewModelTests() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PokedexViewModel - fetchPokemonList success - loads pokemon and updates list`() = runTest(testDispatcher) {
        // Set up mock BEFORE instantiating the ViewModel or the relevant call
        whenever(mockApiService.getPokemonList(any(), any())).thenReturn(fakePokemonListResponse)
        whenever(mockApiService.getPokemonDetail("bulbasaur")).thenReturn(fakeBulbasaurDetail)
        whenever(mockApiService.getPokemonDetail("ivysaur")).thenReturn(fakeIvysaurDetail)

        viewModel = PokedexViewModel(mockApiService) // Now init will use the mocks configured above

        val pokemons = viewModel.pokemonList.first { it.isNotEmpty() }

        assertEquals(false, viewModel.isLoading.value)
        assertEquals(2, pokemons.size)
        assertTrue(pokemons.any { it.name == "Bulbasaur" && it.id == 1 })
        assertTrue(pokemons.any { it.name == "Ivysaur" && it.id == 2 })
    }

    @Test
    fun `PokedexViewModel - onSearchTextChanged - updates searchText correctly`() = runTest(testDispatcher) {
        // This test doesn't rely on API calls for its main action,
        // but the ViewModel init does.
        // Therefore, we need a basic mock for init.
        whenever(mockApiService.getPokemonList(any(), any())).thenReturn(emptyPokemonListResponse) // Mock for init
        // We don't need getPokemonDetail for init if only getPokemonList is called and details aren't processed immediately.
        // Adjust based on your exact init logic.

        viewModel = PokedexViewModel(mockApiService) // Create viewModel after the whens

        val testSearch = "Pikachu"
        viewModel.onSearchTextChanged(testSearch)
        assertEquals(testSearch, viewModel.searchText.value)
    }

    @Test
    fun `PokedexViewModel - togglePokemonCaptured - updates isCaptured correctly`() = runTest(testDispatcher) {
        // Set up mock BEFORE instantiating the ViewModel
        whenever(mockApiService.getPokemonList(any(), any())).thenReturn(fakePokemonListResponse)
        whenever(mockApiService.getPokemonDetail("bulbasaur")).thenReturn(fakeBulbasaurDetail)
        whenever(mockApiService.getPokemonDetail("ivysaur")).thenReturn(fakeIvysaurDetail)

        viewModel = PokedexViewModel(mockApiService) // Now init will use the mocks configured above

        val initialList = viewModel.pokemonList.first { it.isNotEmpty() }
        assertTrue(initialList.isNotEmpty())

        val pokemonToToggle = initialList.first { it.name == "Bulbasaur" }
        assertFalse(pokemonToToggle.isCaptured)

        viewModel.togglePokemonCaptured(pokemonToToggle.id)

        val updatedList = viewModel.pokemonList.value
        val toggledPokemonInList = updatedList.find { it.id == pokemonToToggle.id }

        assertNotNull(toggledPokemonInList)
        assertTrue(toggledPokemonInList!!.isCaptured)

        viewModel.togglePokemonCaptured(pokemonToToggle.id)
        val revertedList = viewModel.pokemonList.value
        val revertedPokemonInList = revertedList.find { it.id == pokemonToToggle.id }

        assertNotNull(revertedPokemonInList)
        assertFalse(revertedPokemonInList!!.isCaptured)
    }

    @Test
    fun `PokedexViewModel - searchText - filters pokemon list correctly`() = runTest(testDispatcher) {
        Log.d("PokedexSearchTest", "Test starting...")

        // 1. Set up mocks
        whenever(mockApiService.getPokemonList(any(), any())).thenReturn(fakePokemonListResponse)
        whenever(mockApiService.getPokemonDetail("bulbasaur")).thenReturn(fakeBulbasaurDetail)
        whenever(mockApiService.getPokemonDetail("ivysaur")).thenReturn(fakeIvysaurDetail)
        Log.d("PokedexSearchTest", "Mocks configured.")

        // 2. Instantiate ViewModel
        viewModel = PokedexViewModel(mockApiService)
        Log.d("PokedexSearchTest", "ViewModel instantiated. Init should be running/completed.")

        // 3. Wait for pokemonList (source StateFlow) to be populated
        Log.d("PokedexSearchTest", "Waiting for pokemonList to have 2 items...")
        val initialPokemonListSource = viewModel.pokemonList.first {
            Log.d("PokedexSearchTest", "pokemonList current size: ${it.size}")
            it.size == 2
        }
        Log.d("PokedexSearchTest", "pokemonList has ${initialPokemonListSource.size} items. Proceeding.")

        // 4. Wait for filteredPokemonList (derived StateFlow) to reflect initial load.
        //    This is KEY because filteredPokemonList depends on pokemonList AND searchText.
        //    With an empty searchText, it should have the same size.
        Log.d("PokedexSearchTest", "Waiting for filteredPokemonList to have 2 items (initial state)...")
        val initialFilteredList = viewModel.filteredPokemonList.first {
            Log.d("PokedexSearchTest", "filteredPokemonList (initial) current size: ${it.size}, searchText: '${viewModel.searchText.value}'")
            it.size == 2 // Assuming searchText is empty and pokemonList has 2
        }
        Log.d("PokedexSearchTest", "filteredPokemonList (initial) has ${initialFilteredList.size} items.")

        // 5. First Assertion (Should now be more robust!)
        assertEquals("Initially, filtered list should contain all Pokémon", 2, initialFilteredList.size)
        Log.d("PokedexSearchTest", "Initial assertion passed.")

        // --- Search Tests ---

        Log.d("PokedexSearchTest", "Testing search for 'Bulb'...")
        viewModel.onSearchTextChanged("Bulb")
        // Wait for filter to apply and filteredPokemonList to emit the new state
        val bulbFilteredList = viewModel.filteredPokemonList.first {
            Log.d("PokedexSearchTest", "filteredPokemonList ('Bulb') current size: ${it.size}")
            it.size == 1 && it.any { p -> p.name == "Bulbasaur" }
        }
        assertEquals("Should find 1 Pokémon with 'Bulb'", 1, bulbFilteredList.size)
        assertEquals("Bulbasaur", bulbFilteredList[0].name)
        Log.d("PokedexSearchTest", "Search for 'Bulb' passed.")


        Log.d("PokedexSearchTest", "Testing search for 'Pikachu'...")
        viewModel.onSearchTextChanged("Pikachu")
        val pikachuFilteredList = viewModel.filteredPokemonList.first {
            Log.d("PokedexSearchTest", "filteredPokemonList ('Pikachu') current size: ${it.size}")
            it.isEmpty()
        }
        assertTrue("Should not find any Pokémon with 'Pikachu'", pikachuFilteredList.isEmpty())
        Log.d("PokedexSearchTest", "Search for 'Pikachu' passed.")

        Log.d("PokedexSearchTest", "Testing empty search...")
        viewModel.onSearchTextChanged("")
        val emptySearchFilteredList = viewModel.filteredPokemonList.first {
            Log.d("PokedexSearchTest", "filteredPokemonList (empty search) current size: ${it.size}")
            it.size == 2
        }
        assertEquals("With an empty search, the filtered list should show all Pokémon again", 2, emptySearchFilteredList.size)
        Log.d("PokedexSearchTest", "Empty search test passed.")

        Log.d("PokedexSearchTest", "Test finished.")
    }

    @Test
    fun `PokedexViewModel - fetchPokemonList error - handles API error`() = runTest(testDispatcher) {
        // Configure the mock to throw an exception
        whenever(mockApiService.getPokemonList(any(), any())).thenThrow(RuntimeException("Network error"))
        // We don't need to mock getPokemonDetail here if getPokemonList fails first.

        // Create the ViewModel AFTER configuring the mock to fail.
        viewModel = PokedexViewModel(mockApiService) // The init will fail here

        val isLoadingValue = viewModel.isLoading.first { !it } // Wait for isLoading to become false
        assertFalse(isLoadingValue)
        assertTrue(viewModel.pokemonList.value.isEmpty())
    }
}