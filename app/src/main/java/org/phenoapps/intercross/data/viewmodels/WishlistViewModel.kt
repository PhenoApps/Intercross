package org.phenoapps.intercross.data.viewmodels

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.phenoapps.intercross.data.WishlistRepository
import org.phenoapps.intercross.data.models.Wishlist
import org.phenoapps.intercross.data.models.WishlistView

class WishlistViewModel internal constructor(private val repo: WishlistRepository)
    : BaseViewModel<Wishlist>(repo) {

    private val viewModelJob = Job()

    //coroutines do not complete until all launched children complete
    private val viewModelScope = CoroutineScope(Main + viewModelJob)

    val wishlist: LiveData<List<Wishlist>> = repo.getAll()

    val wishes: LiveData<List<WishlistView>> = repo.getWishes()

    val commutativeWishes = repo.getAllCommutativeWishCounts()

    val crossblock = repo.getCrossblock()

    val commutativeCrossblock = repo.getCommutativeCrossblock()

    fun getByParents(femaleId: String, maleId: String) = repo.getByParents(femaleId, maleId)

    fun deleteAll() {

        viewModelScope.launch {

            repo.drop()
        }
    }

    fun dropAndInsert(wishes: List<Wishlist>) {

        viewModelScope.launch {

            repo.drop()

            repo.insert(*wishes.toTypedArray())
        }
    }
}