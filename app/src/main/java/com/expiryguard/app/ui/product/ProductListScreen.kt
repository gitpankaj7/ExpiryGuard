package com.expiryguard.app.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expiryguard.app.data.local.UserProfile
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.components.ProductCard
import com.expiryguard.app.ui.theme.ExpiredRedLight
import com.expiryguard.app.ui.theme.SafeGreenLight
import com.expiryguard.app.ui.theme.WarningOrangeLight

@Composable
fun ProductListScreen(
    userProfile: UserProfile?,
    requestedFilter: ProductFilter = ProductFilter.ALL,
    filterRequestKey: Int = 0,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    onScanBarcode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory)
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(filterRequestKey) {
        viewModel.onFilterChange(requestedFilter)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Product")
                Spacer(Modifier.width(8.dp))
                Text("Add")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header + User Status ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventory",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (userProfile?.isSubscribed == true) {
                        Text(
                            text = "Premium Member",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (userProfile != null) {
                        val days = userProfile.getTrialDaysRemaining()
                        if (days > 0) {
                            Text(
                                text = "$days days left in free trial",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Free trial expired",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── Search Bar + Scan ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search products…") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onScanBarcode
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ── Filter Chips ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                ProductFilter.entries.forEach { filter ->
                    val isSelected = state.activeFilter == filter
                    val chipColor = when (filter) {
                        ProductFilter.EXPIRED -> ExpiredRedLight
                        ProductFilter.EXPIRING_SOON -> WarningOrangeLight
                        ProductFilter.SAFE -> SafeGreenLight
                        ProductFilter.ALL -> MaterialTheme.colorScheme.primary
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    ProductFilter.ALL -> "All"
                                    ProductFilter.EXPIRED -> "Expired"
                                    ProductFilter.EXPIRING_SOON -> "Expiring Soon"
                                    ProductFilter.SAFE -> "Safe"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.15f),
                            selectedLabelColor = chipColor
                        )
                    )
                }
            }

            // ── Product Count ──
            Text(
                text = "${state.products.size} products",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // ── Product List ──
            if (state.products.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyState(
                        icon = Icons.Rounded.Inventory2,
                        title = "No Products Found",
                        subtitle = "Add your first product to start tracking expiry dates.",
                        actionLabel = "Add Product",
                        onAction = onAddProduct
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = state.products,
                        key = { _, product -> product.id }
                    ) { index, product ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { 40 * (index + 1) }
                        ) {
                            ProductCard(
                                product = product,
                                onClick = { onEditProduct(product.id) },
                                onDelete = { viewModel.deleteProduct(product) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
