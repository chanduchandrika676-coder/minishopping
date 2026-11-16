package com.example.android.ui.cart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.data.local.AppDatabase
import com.example.android.data.local.CartItemEntity
import com.example.android.data.repository.ShopRepository
import com.example.android.databinding.ActivityCartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var repository: ShopRepository
    private lateinit var cartAdapter: CartAdapter
    private var currentCartItems: List<CartItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarCart)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCart.setNavigationOnClickListener { finish() }

        val database = AppDatabase.getDatabase(this)
        repository = ShopRepository(database.cartDao())

        setupRecyclerView()
        observeCart()

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { productId, newQty ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.updateQuantity(productId, newQty)
                }
            },
            onItemRemoved = { productId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.removeFromCart(productId)
                }
            }
        )
        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun observeCart() {
        lifecycleScope.launch {
            repository.cartItems.collectLatest { items ->
                currentCartItems = items
                cartAdapter.submitList(items)
                calculateTotal(items)
            }
        }
    }

    private fun calculateTotal(items: List<CartItemEntity>) {
        var total = 0.0
        for (item in items) {
            val discountedPrice = item.price * (1 - item.discountPercentage / 100.0)
            total += discountedPrice * item.quantity
        }
        binding.tvCartTotal.text = "$${String.format("%.2f", total)}"
    }

    private fun placeOrder() {
        if (currentCartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        var total = 0.0
        for (item in currentCartItems) {
            val discountedPrice = item.price * (1 - item.discountPercentage / 100.0)
            total += discountedPrice * item.quantity
        }

        // Clear cart and show notification
        lifecycleScope.launch(Dispatchers.IO) {
            repository.clearCart()
            withContext(Dispatchers.Main) {
                showOrderNotification(total)
                Toast.makeText(this@CartActivity, "Order placed successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showOrderNotification(total: Double) {
        val channelId = "order_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Order Placed Successfully!")
            .setContentText("Total amount: $${String.format("%.2f", total)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
