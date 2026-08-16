package com.expiryguard.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks up product information using barcode from multiple APIs.
 * Tries multiple sources for better coverage of Indian products.
 *
 * API priority:
 * 1. Open Food Facts (global, free, large database)
 * 2. Open Beauty Facts (cosmetics/personal care)
 * 3. UPC Item DB (general products)
 */
object BarcodeProductLookup {

    private const val TAG = "BarcodeProductLookup"
    private const val TIMEOUT_MS = 10000

    data class ProductInfo(
        val name: String,
        val category: String,
        val brand: String? = null,
        val imageUrl: String? = null
    )

    // Map API categories to our app categories
    private val categoryMapping = mapOf(
        // Beverages
        "beverages" to "Beverages", "drinks" to "Beverages", "juices" to "Beverages",
        "sodas" to "Beverages", "waters" to "Beverages", "tea" to "Beverages",
        "coffee" to "Beverages", "energy" to "Beverages",
        // Dairy
        "dairy" to "Dairy", "milk" to "Dairy", "cheese" to "Dairy",
        "yogurt" to "Dairy", "curd" to "Dairy", "butter" to "Dairy",
        "ghee" to "Dairy", "paneer" to "Dairy", "lassi" to "Dairy",
        // Snacks
        "snacks" to "Snacks", "chips" to "Snacks", "biscuits" to "Snacks",
        "cookies" to "Snacks", "namkeen" to "Snacks", "chocolate" to "Snacks",
        "candy" to "Snacks", "sweets" to "Snacks", "wafer" to "Snacks",
        // Cosmetics
        "cosmetic" to "Cosmetics", "beauty" to "Cosmetics", "skin" to "Cosmetics",
        "hair" to "Cosmetics", "makeup" to "Cosmetics", "cream" to "Cosmetics",
        "lotion" to "Cosmetics", "face" to "Cosmetics", "serum" to "Cosmetics",
        // Personal Care
        "shampoo" to "Personal Care", "soap" to "Personal Care",
        "toothpaste" to "Personal Care", "deodorant" to "Personal Care",
        "hygiene" to "Personal Care", "personal" to "Personal Care",
        "body wash" to "Personal Care", "conditioner" to "Personal Care",
        // Household
        "detergent" to "Household", "cleaner" to "Household",
        "cleaning" to "Household", "household" to "Household",
        // Medicine
        "medicine" to "Medicine", "pharmaceutical" to "Medicine",
        "health" to "Medicine", "supplement" to "Medicine",
        "vitamin" to "Medicine", "ayurved" to "Medicine",
        // Grocery (default fallback)
        "cereal" to "Grocery", "rice" to "Grocery", "flour" to "Grocery",
        "atta" to "Grocery", "dal" to "Grocery", "lentil" to "Grocery",
        "oil" to "Grocery", "spice" to "Grocery", "masala" to "Grocery",
        "sauce" to "Grocery", "noodle" to "Grocery", "pasta" to "Grocery",
        "instant" to "Grocery", "food" to "Grocery", "grocery" to "Grocery",
        "plant-based" to "Grocery", "meal" to "Grocery", "spread" to "Grocery",
        "meat" to "Grocery", "vegetable" to "Grocery", "fruit" to "Grocery",
        "bakery" to "Grocery", "bread" to "Grocery", "condiment" to "Grocery"
    )

    /**
     * Lookup product by barcode. Tries multiple APIs for best coverage.
     * Returns null only if ALL APIs fail.
     */
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        // Try API 1: Open Food Facts (largest free food database)
        var result = lookupOpenFoodFacts(barcode)
        if (result != null) return@withContext result

        // Try API 2: Open Beauty Facts (cosmetics/personal care)
        result = lookupOpenBeautyFacts(barcode)
        if (result != null) return@withContext result

        // Try API 3: Open Products Facts (general products)
        result = lookupOpenProductsFacts(barcode)
        if (result != null) return@withContext result

        // Try API 4: UPC ItemDB (general barcode database)
        result = lookupUpcItemDb(barcode)
        if (result != null) return@withContext result


        null
    }

    /**
     * Open Food Facts — largest free food database
     */
    private fun lookupOpenFoodFacts(barcode: String): ProductInfo? {
        return lookupOpenXFacts(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,categories_tags,image_url",
            barcode,
            "OpenFoodFacts"
        )
    }

    /**
     * Open Beauty Facts — cosmetics, skincare, personal care
     */
    private fun lookupOpenBeautyFacts(barcode: String): ProductInfo? {
        return lookupOpenXFacts(
            "https://world.openbeautyfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,categories_tags,image_url",
            barcode,
            "OpenBeautyFacts"
        )
    }

    /**
     * Open Products Facts — household, electronics, general products
     */
    private fun lookupOpenProductsFacts(barcode: String): ProductInfo? {
        return lookupOpenXFacts(
            "https://world.openproductsfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,categories_tags,image_url",
            barcode,
            "OpenProductsFacts"
        )
    }

    /**
     * Shared logic for all Open*Facts APIs (same JSON format)
     */
    private fun lookupOpenXFacts(apiUrl: String, barcode: String, source: String): ProductInfo? {
        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "ExpiryGuard/1.0 (Android; contact@expiryguard.app)")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "$source: HTTP $responseCode for barcode: $barcode")
                connection.disconnect()
                return null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseBody)
            val status = json.optInt("status", 0)
            if (status != 1) {
                Log.d(TAG, "$source: Not found for barcode: $barcode")
                return null
            }

            val product = json.optJSONObject("product") ?: return null

            var name = product.optString("product_name", "").trim()
            if (name.isEmpty()) name = product.optString("product_name_en", "").trim()
            if (name.isEmpty()) name = product.optString("generic_name", "").trim()
            
            val brand = product.optString("brands", "").trim()

            if (name.isEmpty()) return null

            if (brand.isNotEmpty() && !name.contains(brand, ignoreCase = true)) {
                name = "$brand $name"
            }

            val categoriesTags = product.optJSONArray("categories_tags")
            val category = mapCategory(categoriesTags)
            val imageUrl = product.optString("image_url", "").ifEmpty { null }

            Log.d(TAG, "$source: Found '$name' category='$category' for barcode: $barcode")

            return ProductInfo(
                name = name,
                category = category,
                brand = brand.ifEmpty { null },
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            Log.d(TAG, "$source: Error for barcode $barcode: ${e.message}")
            return null
        }
    }

    /**
     * UPC ItemDB — general barcode database with broader coverage
     */
    private fun lookupUpcItemDb(barcode: String): ProductInfo? {
        try {
            val url = URL("https://api.upcitemdb.com/prod/trial/lookup?upc=$barcode")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "ExpiryGuard/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "UPCItemDB: HTTP $responseCode for barcode: $barcode")
                connection.disconnect()
                return null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseBody)
            val items = json.optJSONArray("items")
            if (items == null || items.length() == 0) {
                Log.d(TAG, "UPCItemDB: No items for barcode: $barcode")
                return null
            }

            val item = items.getJSONObject(0)
            var name = item.optString("title", "").trim()
            val brand = item.optString("brand", "").trim()
            val upcCategory = item.optString("category", "").trim()

            if (name.isEmpty()) return null

            // Map the UPC category
            val category = if (upcCategory.isNotEmpty()) {
                mapCategoryFromString(upcCategory)
            } else {
                "Grocery"
            }

            Log.d(TAG, "UPCItemDB: Found '$name' category='$category' for barcode: $barcode")

            return ProductInfo(
                name = name,
                category = category,
                brand = brand.ifEmpty { null }
            )
        } catch (e: Exception) {
            Log.d(TAG, "UPCItemDB: Error for barcode $barcode: ${e.message}")
            return null
        }
    }

    private fun mapCategory(categoriesTags: org.json.JSONArray?): String {
        if (categoriesTags == null || categoriesTags.length() == 0) {
            return "Grocery"
        }

        for (i in 0 until categoriesTags.length()) {
            val tag = categoriesTags.optString(i, "").lowercase()
                .removePrefix("en:")
                .replace("-", " ")

            for ((keyword, appCategory) in categoryMapping) {
                if (tag.contains(keyword)) {
                    return appCategory
                }
            }
        }

        return "Grocery"
    }

    private fun mapCategoryFromString(categoryStr: String): String {
        val lower = categoryStr.lowercase()
        for ((keyword, appCategory) in categoryMapping) {
            if (lower.contains(keyword)) {
                return appCategory
            }
        }
        return "Grocery"
    }
}
