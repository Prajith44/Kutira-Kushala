package com.example.kutira_kushala

object FavoritesRepository {
    private val favoriteProductIds = mutableSetOf<String>()
    private var onFavoritesChanged: (() -> Unit)? = null

    fun setOnFavoritesChangedListener(listener: () -> Unit) {
        onFavoritesChanged = listener
    }

    fun toggleFavorite(productId: String) {
        if (favoriteProductIds.contains(productId)) {
            favoriteProductIds.remove(productId)
        } else {
            favoriteProductIds.add(productId)
        }
        onFavoritesChanged?.invoke()
    }

    fun isFavorite(productId: String): Boolean = favoriteProductIds.contains(productId)

    fun getFavoriteProducts(): List<ProductItem> {
        return ProductRepository.getAllProducts().filter { favoriteProductIds.contains(it.name) }
    }
}
