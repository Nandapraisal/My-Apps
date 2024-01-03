package com.example.kaafkaproject

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.kaafkaproject.databinding.ActivityMainBinding
import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale
import android.widget.EditText
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : AppCompatActivity(), TotalPriceCallback {
  //declare element for use in this activity

    private lateinit var listViewMenu: ListView
    private lateinit var btnCheckout: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var tvTotalPrice: TextView
    private lateinit var etSearch: EditText

    private lateinit var menuAdapter: MenuAdapter
    private val menuViewModel: MenuViewModel by viewModels()

    private val MENU_ITEMS_KEY = "menu_items"

    private val originalMenuItems = ListMenu.menuItems.toMutableList()
    private val filteredMenuItems = ArrayList(originalMenuItems)
    var previousSelectedItems: Map<MenuItem, Int> = emptyMap()


    private val PAYMENT_METHOD_REQUEST_CODE = 1
    enum class PaymentMethod {
        QRIS,
        CASH,
        TRANSFER,

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        menuAdapter = MenuAdapter(this, ListMenu.menuItems)
//      Variable element
        listViewMenu = binding.listViewMenu
        btnCheckout = binding.btnCheckout
        btnDelete = binding.btnDelete
        tvTotalPrice = binding.tvTotalPrice
        menuAdapter = MenuAdapter(this, ListMenu.menuItems)
        menuAdapter.setTotalPriceCallback(this)
        listViewMenu.adapter = menuAdapter
        etSearch = binding.etSearch

        btnCheckout.setOnClickListener {
            showPaymentMethodDialog()
        }
        btnDelete.setOnClickListener {
            val deleteTotalPrice = 0
            menuAdapter.resetSelectedItems()
            updateTotalPrice(deleteTotalPrice)

        }




        // Restore selected items if savedInstanceState is not null
        savedInstanceState?.let {
            val selectedItems = it.getSerializable(MENU_ITEMS_KEY) as Map<MenuItem, Int>?
            selectedItems?.let {
                menuAdapter.setSelectedItems(selectedItems)


            }
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Tidak digunakan dalam contoh ini
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Tidak digunakan dalam contoh ini
            }

            override fun afterTextChanged(s: Editable?) {
                filterMenuItems(s.toString())
            }
        })
        filterMenuItems("")
    }

    private fun showPaymentMethodDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.payment_method)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Find views in the dialog layout
        val btnCash = dialog.findViewById<ImageButton>(R.id.btnCash)
        val btnTransfer = dialog.findViewById<ImageButton>(R.id.btnTransfer)
        val btnQRIS = dialog.findViewById<ImageButton>(R.id.btnQris)
        val nominalCash = dialog.findViewById<EditText>(R.id.nominalCash)
        val btnClick = dialog.findViewById<LinearLayout>(R.id.btnClick)
        val inputNominal = dialog.findViewById<LinearLayout>(R.id.inputNominal)

        btnCash.setOnClickListener {
            // Handle btncash click
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.payment_method) // Ganti dengan layout dialog yang sesuai

            val btnClick = dialog.findViewById<LinearLayout>(R.id.btnClick)
            val inputNominal = dialog.findViewById<LinearLayout>(R.id.inputNominal)
            val nominalCash = dialog.findViewById<EditText>(R.id.nominalCash)
            val confirmNominal = dialog.findViewById<Button>(R.id.confirmNominal)

            btnClick.visibility = View.GONE
            inputNominal.visibility = View.VISIBLE

            confirmNominal.setOnClickListener {
                val nominal = nominalCash.text.toString().toIntOrNull()

                if (nominal != null && nominal > 0) {
                    // Valid nominal, proceed
                    inputNominal.visibility = View.GONE
                    dialog.dismiss()
                    navigateToDetailActivity(PaymentMethod.CASH, nominal)
                } else {
                    // Invalid or zero nominal, show a message
                    Toast.makeText(this, "Belum masukkan nominal", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }





        btnTransfer.setOnClickListener {
            // Handle Transfer payment method
            dialog.dismiss()
            navigateToDetailActivity(PaymentMethod.TRANSFER)
        }

        btnQRIS.setOnClickListener {
            // Handle QRIS payment method
            dialog.dismiss()
            navigateToDetailActivity(PaymentMethod.QRIS)
        }

        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save selected items when the activity is about to be destroyed
        val selectedItems = menuAdapter.getSelectedItems()
        outState.putSerializable(MENU_ITEMS_KEY, selectedItems as Serializable)
        super.onSaveInstanceState(outState)
    }

    override fun onTotalPriceChanged(totalPrice: Int) {
        val formattedTotalPrice =
            NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalPrice.toLong())
                .replace("Rp", "Rp ")
        tvTotalPrice.text = "Total: $formattedTotalPrice"
    }

    private fun updateTotalPrice(deleteTotalPrice: Int) {
        tvTotalPrice.text = "Total: Rp $deleteTotalPrice"
    }

    private fun navigateToDetailActivity(paymentMethod: PaymentMethod, nominal: Int? = null) {
        menuViewModel.selectedItems = menuAdapter.getSelectedItems()

        val selectedItems = menuAdapter.getSelectedItems()

        val totalPrice = selectedItems.map { (menuItem, quantity) ->
            menuItem.price * quantity
        }.sum()

        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("selectedItems", selectedItems as Serializable)
        intent.putExtra("totalPrice", totalPrice)
        intent.putExtra("paymentMethod", paymentMethod)
        if (paymentMethod == PaymentMethod.CASH) {
            // Add the nominal value if the payment method is CASH
            if (nominal != null) {
                intent.putExtra("nominal", nominal)
            }
        }
        startActivity(intent)
    }


    private fun filterMenuItems(query: String) {
        val filteredItems = originalMenuItems.filter { menuItem ->
            menuItem.name.contains(query, ignoreCase = true)
        }

        // Perbarui item yang ditampilkan dan item yang sudah terpilih
        menuAdapter.updateDisplayedItemsAndSelectedItems(filteredItems, previousSelectedItems)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_METHOD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Handle the result if needed
        }
    }
}
