package com.example.kaafkaproject


import android.os.Bundle
import android.os.Handler
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.viewModels

class DetailActivity : AppCompatActivity() {
    private val menuViewModel: MenuViewModel by viewModels()


    private lateinit var listViewDetail: ListView
    private lateinit var textViewTotal: TextView
    private lateinit var textViewPajak: TextView
    private lateinit var textViewBayar: TextView
    private lateinit var Tanggal: TextView
    private lateinit var btnPrint: Button
    private lateinit var btnBack: Button
    private lateinit var bluetoothPrinter: BluetoothPrinter
    private lateinit var outputStream: OutputStream

    private var selectedItems: Map<MenuItem, Int>? = null
    private var totalPrice: Double = 0.0
    private val pajak: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)

        // Inisialisasi komponen UI
        listViewDetail = findViewById(R.id.listViewDetail)
        textViewTotal = findViewById(R.id.textViewTotal)
        textViewPajak = findViewById(R.id.textViewPajak)
        textViewBayar = findViewById(R.id.textViewBayar)
        Tanggal = findViewById(R.id.Tanggal)
        btnPrint = findViewById(R.id.btnPrint)
        btnBack = findViewById(R.id.btnBack)

        // Inisialisasi BluetoothPrinter
        val bluetoothHandler = Handler { message ->
            // Handle Bluetooth printer messages here if needed
            // For example, show a toast message based on the message received
            val messageText = message.data.getString("message")
            showToast(messageText ?: "Unknown message")
            true
        }
        bluetoothPrinter = BluetoothPrinter(this, bluetoothHandler)


        // Inisialisasi selectedItems (misalnya dari intent)
        selectedItems = intent.getSerializableExtra("selectedItems") as Map<MenuItem, Int>

        // Set the current date to the Tanggal TextView
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        Tanggal.text = "Transaction Date: $currentDate"

        totalPrice = selectedItems?.map { (menuItem, quantity) ->
            menuItem.price * quantity.toDouble()
        }?.sum() ?: 0.0



        val orderItems = selectedItems
            ?.filter { it.value > 0 }
            ?.map { (menuItem, quantity) ->
                val itemName = menuItem.name
                val qtyText = "x $quantity"
                val totalText = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(menuItem.price * quantity.toDouble())
                    .replace(",00", "")
                    .replace(". ", ".")
                    .replace(Regex("[^0-9.]"), "")

                "${itemName.padEnd(25)}${qtyText.padEnd(8)}$totalText\n"

            } ?: emptyList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, orderItems)
        listViewDetail.adapter = adapter

        val totalBayar = totalPrice + pajak

        textViewTotal.text =
            "Total: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalPrice)
                .replace(",00", "")
                .replace("Rp","Rp ")}"
        textViewPajak.text =
            "Tax: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(pajak)
                .replace(",00", "")
                .replace("Rp","Rp ")}"
        textViewBayar.text =
            "Total Bayar: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalBayar)
                .replace(",00", "")
                .replace("Rp","Rp ")             }"

        // Set event listener untuk tombol back
        btnBack.setOnClickListener {
            finish()
        }

        // Set event listener untuk tombol print
        btnPrint.setOnClickListener {
            printBill()
        }
    }
    private fun printBill() {

            try {
                if(totalPrice>0) {
                // Connect to the Bluetooth printer
            bluetoothPrinter.connectPrinter("RPP02")

            // Get the output stream from BluetoothPrinter
            val outputStream = bluetoothPrinter.getOutputStream()
            val resto = "She Kembar Resto"
            val alamat = "Jl. Taman Palem Raya No 27, Sindang Jaya, Talaga Bestari"
            val greetings = "Thank you for Order!"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // Print header information
            val escPosCommands = byteArrayOf(
                0x1B, 0x07, 0x01,  // Bold text
                0x1B, 0x20, 0x00,  // Cancel bold text
                0x1B, 0x44, 0x02, 0x10,  // Set horizontal tab positions (right-aligned)
//                0x1B, 0x44, 0x05, 0x10, 0x15, 0x20,
                0x1D, 0x07, 0x01  // Double height text
            )
//            val escPosCommands = byteArrayOf(
//                0x1B, 0x21, 0x02,  // Set text size to double height and double width
//                0x1B, 0x44, 0x05, 0x10, 0x15, 0x20,  // Set horizontal tab positions (right-aligned)
//                0x1D, 0x21, 0x00  // Set text size to normal
//            )
            outputStream.write(escPosCommands)

            val billContent = """
                      $resto
                      
            $alamat
            
            Date: $currentDate
            ------------------------------
            Menu             Qty      Total
            ------------------------------
            
        """.trimIndent()
            bluetoothPrinter.printText(billContent)

            selectedItems?.forEach {  (menuItem, quantity) ->
                if (quantity > 0) {
                    val itemName = menuItem.name
                    val qtyText = "x$quantity"
                    val totalText = (NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        .format(menuItem.price * quantity.toDouble())).replace("Rp", "")
                        .replace(",00", "")
                        .replace(". ", ".")
                        .replace(Regex("[^0-9.]"), "")


                    // Variabel itemText dideklarasikan di luar blok if
                    val itemText: String = if (totalText.length == 5) {
                        "${itemName.padEnd(20)}${qtyText.padEnd(7)}$totalText\n"
                    } else if (totalText.length == 6) {
                        "${itemName.padEnd(20)}${qtyText.padEnd(6)}$totalText\n"
                    } else if (totalText.length == 7) {
                        "${itemName.padEnd(20)}${qtyText.padEnd(5)}$totalText\n"
                    } else {
                        "${itemName.padEnd(20)}${qtyText.padEnd(5)}$totalText\n"
                    }

                    bluetoothPrinter.printText(itemText)
                }
            }



            // Print total, tax, payment, and greetings
            val totalPriceText = "${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format( totalPrice)}"
                .replace("Rp","Rp ")
            val taxText = "${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format( pajak)}"
                .replace("Rp","Rp ")
            val totalBayarText = "${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format( totalPrice + pajak)}"
                .replace("Rp","Rp ")

            val summary = """
            ------------------------------
            Total:             ${totalPriceText.replace(",00","").replace(". ",".")}
            Tax:               ${taxText.replace(",00","").replace(". ",".")}
            Payment:           ${totalBayarText.replace(",00","").replace(". ",".")}
            ------------------------------
                  $greetings
            
            
            
            
            ------------------------------
        """.trimIndent()
            bluetoothPrinter.printText(summary)

            // Cut paper (if supported by the printer)
            val cutPaper = byteArrayOf(0x1D, 0x56, 0x01)
            outputStream.write(cutPaper)

            // Flush the output stream
            outputStream.flush()

            // Close the Bluetooth connection
            bluetoothPrinter.closeConnection()
                }
                else {
                    // Tampilkan pesan jika totalPrice sama dengan 0
                    // Misalnya:
                    showToast("Anda Belum Input Apapun")
                }
            }
            catch (e: Exception) {
            e.printStackTrace()

        }
    }

    private fun showToast(message: String) {
        // Show a toast message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
