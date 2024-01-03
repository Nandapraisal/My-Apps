// MenuAdapter.kt

package com.example.kaafkaproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale


class MenuAdapter(context: Context, private var originalItems: List<MenuItem>) : ArrayAdapter<MenuItem>(context, 0, originalItems), Filterable {

    private var displayedItems: List<MenuItem> = listOf() // Declare displayedItems here
    private var previousSelectedItems: Map<MenuItem, Int> = mapOf() // Declare previousSelectedItems here

    private val selectedItems = mutableMapOf<MenuItem, Int>()
    private var totalPriceCallback: TotalPriceCallback? = null

    override fun getCount(): Int {
        return displayedItems.size
    }

    override fun getItem(position: Int): MenuItem {
        return displayedItems[position]
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val queryString = constraint?.toString()?.toLowerCase(Locale.getDefault())

                displayedItems = if (queryString.isNullOrBlank()) {
                    originalItems
                } else {
                    originalItems.filter { it.name.toLowerCase(Locale.getDefault()).contains(queryString) }
                }

                filterResults.values = displayedItems
                filterResults.count = displayedItems.size
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }


    fun setTotalPriceCallback(callback: TotalPriceCallback) {
        this.totalPriceCallback = callback
    }

    private fun notifyTotalPriceChanged() {
        totalPriceCallback?.onTotalPriceChanged(calculateTotalPrice())
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.menu_list, parent, false)
        }

        val menuItem = getItem(position)
        // belement from xml
        val imageViewMenu: ImageView = view!!.findViewById(R.id.imageViewMenu)
        val textViewMenuName: TextView = view.findViewById(R.id.textViewMenuName)
        val textViewMenuPrice: TextView = view.findViewById(R.id.textViewMenuPrice)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
        val btnSubtract: Button = view.findViewById(R.id.btnSubtract)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)

        menuItem?.let {
            imageViewMenu.setImageResource(it.imageResId)
            textViewMenuName.text = it.name
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            numberFormat.maximumFractionDigits = 0
            val formattedPrice = numberFormat.format(it.price.toLong()).replace("Rp", "Rp ")
            textViewMenuPrice.text = formattedPrice

            var quantity = selectedItems[menuItem] ?: 0
            tvQuantity.text = quantity.toString()

            btnAdd.setOnClickListener {
                quantity++
                selectedItems[menuItem] = quantity
                tvQuantity.text = quantity.toString()
                notifyDataSetChanged()
                notifyTotalPriceChanged()
            }

            btnSubtract.setOnClickListener {
                if (quantity > 0) {
                    quantity--
                    selectedItems[menuItem] = quantity
                    tvQuantity.text = quantity.toString()
                    notifyDataSetChanged()
                    notifyTotalPriceChanged()
                }
            }
        }

        return view
    }

    private fun calculateTotalPrice(): Int {
        return selectedItems.map { (menuItem, quantity) ->
            menuItem.price * quantity
        }.sum()
    }

    fun resetSelectedItems() {
        selectedItems.clear()
        notifyDataSetChanged()
        notifyTotalPriceChanged()
    }

    fun getSelectedItems(): Map<MenuItem, Int> {
        return selectedItems.toMap()
    }
    fun setSelectedItems(items: Map<MenuItem, Int>) {
        selectedItems.clear()
        selectedItems.putAll(items)
        notifyDataSetChanged()
        notifyTotalPriceChanged()
    }
    fun updateDisplayedItemsAndSelectedItems(newItems: List<MenuItem>, previousSelectedItems: Map<MenuItem, Int>) {
        displayedItems = ArrayList(newItems)
        notifyDataSetChanged()
    }

}



