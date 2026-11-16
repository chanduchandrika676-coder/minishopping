package com.example.android.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.R
import com.example.android.data.local.AppDatabase
import com.example.android.data.repository.ShopRepository
import com.example.android.databinding.ActivityMainBinding
import com.example.android.ui.auth.LoginActivity
import com.example.android.ui.cart.CartActivity
import com.example.android.ui.detail.ProductDetailActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ShopRepository
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val database = AppDatabase.getDatabase(this)
        repository = ShopRepository(database.cartDao())

        setupRecyclerView()
        setupSearch()
        setupScanner()

        loadCategories()
        loadProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewProducts.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchProducts(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadProducts()
                } else {
                    searchProducts(newText)
                }
                return true
            }
        })
    }

    private fun setupScanner() {
        binding.fabScan.setOnClickListener {
            val options = GmsBarcodeScannerOptions.Builder()
                .build()
            val scanner = GmsBarcodeScanning.getClient(this, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    val productId = rawValue?.toIntOrNull()
                    if (productId != null && productId in 1..194) {
                        val intent = Intent(this, ProductDetailActivity::class.java).apply {
                            putExtra("PRODUCT_ID", productId)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Scanned Product ID: $rawValue (Opening ID 1)", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ProductDetailActivity::class.java).apply {
                            putExtra("PRODUCT_ID", productId ?: 1)
                        }
                        startActivity(intent)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Scan failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categories = repository.getCategoryList()
                withContext(Dispatchers.Main) {
                    binding.chipGroupCategories.removeAllViews()
                    // All Chip
                    val allChip = Chip(this@MainActivity).apply {
                        text = "All"
                        isCheckable = true
                        isChecked = true
                        setOnCheckedChangeListener { _, checked ->
                            if (checked) loadProducts()
                        }
                    }
                    binding.chipGroupCategories.addView(allChip)

                    categories.forEach { cat ->
                        val chip = Chip(this@MainActivity).apply {
                            text = cat
                            isCheckable = true
                            setOnCheckedChangeListener { _, checked ->
                                if (checked) loadProductsByCategory(cat)
                            }
                        }
                        binding.chipGroupCategories.addView(chip)
                    }
                }
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getProducts(100)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    productAdapter.submitList(response.products)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error loading products: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProductsByCategory(category: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getProductsByCategory(category)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    productAdapter.submitList(response.products)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun searchProducts(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = repository.searchProducts(query)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    productAdapter.submitList(response.products)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
