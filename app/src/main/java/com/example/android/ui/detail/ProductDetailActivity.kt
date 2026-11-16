package com.example.android.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.android.data.local.AppDatabase
import com.example.android.data.model.Product
import com.example.android.data.repository.ShopRepository
import com.example.android.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var repository: ShopRepository
    private var currentProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        val database = AppDatabase.getDatabase(this)
        repository = ShopRepository(database.cartDao())

        val productId = intent.getIntExtra("PRODUCT_ID", 1)
        loadProductDetails(productId)

        binding.btnAddToCart.setOnClickListener {
            currentProduct?.let { product ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.addToCart(product, 1)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProductDetailActivity, "Added to cart!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadProductDetails(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val product = repository.getProductById(id)
                currentProduct = product
                withContext(Dispatchers.Main) {
                    binding.tvDetailTitle.text = product.title
                    binding.tvDetailPrice.text = "$${product.price}"
                    binding.tvDetailRating.text = "★ ${product.rating}"
                    binding.tvDetailDiscount.text = "Discount: ${product.discountPercentage}%"
                    binding.tvDetailStock.text = "Stock: ${product.stock}"
                    binding.tvDetailDescription.text = product.description

                    val images = if (product.images.isNotEmpty()) product.images else listOf(product.thumbnail)
                    val adapter = ProductImagePagerAdapter(images)
                    binding.viewPagerImages.adapter = adapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductDetailActivity, "Error loading product: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
