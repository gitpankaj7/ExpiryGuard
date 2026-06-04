package com.expiryguard.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.expiryguard.app.data.local.ProductEntity
import java.io.File
import java.io.FileWriter

object CsvExporter {

    /**
     * Exports a list of products to a CSV file and returns a shareable FileProvider URI.
     */
    fun export(context: Context, products: List<ProductEntity>): Uri {
        val timestamp = System.currentTimeMillis()
        val file = File(context.cacheDir, "expiryguard_export_$timestamp.csv")

        FileWriter(file).use { writer ->
            // Write header
            writer.appendLine("Name,Category,Quantity,Purchase Price,Expiry Date,Status,Barcode")

            // Write each product row
            for (product in products) {
                val name = escapeCsv(product.name)
                val category = escapeCsv(product.category)
                val quantity = product.quantity.toString()
                val price = product.purchasePrice.toString()
                val expiryDate = DateUtils.formatDate(product.expiryDate)
                val status = DateUtils.getExpiryStatus(product.expiryDate).name
                val barcode = escapeCsv(product.barcode ?: "")

                writer.appendLine("$name,$category,$quantity,$price,$expiryDate,$status,$barcode")
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Escapes a CSV value by wrapping it in quotes if it contains a comma, quote, or newline.
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
