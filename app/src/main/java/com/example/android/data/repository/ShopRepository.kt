package com.example.android.data.repository

import com.example.android.data.local.CartDao
import com.example.android.data.local.CartItemEntity
import com.example.android.data.model.Product
import com.example.android.data.model.ProductResponse
import com.example.android.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val cartDao: CartDao) {
    private val api = ApiClient.apiService

    suspend fun getProducts(limit: Int = 100): ProductResponse = api.getProducts(limit)
    suspend fun getProductById(id: Int): Product = api.getProductById(id)
    suspend fun searchProducts(query: String): ProductResponse = api.searchProducts(query)
    suspend fun getCategoryList(): List<String> = api.getCategoryList()
    suspend fun getProductsByCategory(category: String): ProductResponse = api.getProductsByCategory(category)

    val cartItems: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()

    suspend fun addToCart(product: Product, quantity: Int = 1) {
        val existing = cartDao.getCartItemsList().find { it.productId == product.id }
        val newQty = if (existing != null) existing.quantity + quantity else quantity
        val item = CartItemEntity(
            productId = product.id,
            title = product.title,
            price = product.price,
            discountPercentage = product.discountPercentage,
            thumbnail = product.thumbnail,
            quantity = newQty
        )
        cartDao.insertCartItem(item)
    }

    suspend fun updateQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteCartItem(productId)
        } else {
            val items = cartDao.getCartItemsList()
            val item = items.find { it.productId == productId }
            if (item != null) {
                item.quantity = quantity
                cartDao.insertCartItem(item)
            }
        }
    }

    suspend fun removeFromCart(productId: Int) {
        cartDao.deleteCartItem(productId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}
