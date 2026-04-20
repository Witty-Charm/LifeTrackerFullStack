package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.model.InventoryItemUi
import com.lifetracker.mobile.ui.model.ShopItemUi
import com.lifetracker.mobile.ui.model.ShopScreenState
import com.lifetracker.mobile.ui.model.isBuyLoading

@Composable
fun ShopScreen(
    state: ShopScreenState,
    hero: HeroUi?,
    onBuy: (itemId: Int) -> Unit,
    onShowInventory: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Your Gold: ", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = hero?.goldText ?: "0 Gold",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.showInventory,
                onClick = { onShowInventory(false) },
                label = { Text("Shop") },
                leadingIcon = { Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(16.dp)) },
            )
            FilterChip(
                selected = state.showInventory,
                onClick = { onShowInventory(true) },
                label = { Text("Inventory") },
                leadingIcon = { Icon(Icons.Filled.Backpack, null, Modifier.size(16.dp)) },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (!state.showInventory) {
            ShopItemsList(state = state, onBuy = onBuy)
        } else {
            InventoryList(state = state)
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun ShopItemsList(
    state: ShopScreenState,
    onBuy: (Int) -> Unit,
) {
    if (state.isLoadingItems) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { item ->
            ShopItemCard(item = item, isLoading = state.isBuyLoading(item.id), onBuy = { onBuy(item.id) })
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItemUi,
    isLoading: Boolean,
    onBuy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconForItemType(item.itemType),
                contentDescription = item.name,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.cost} Gold",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onBuy,
                enabled = item.canAfford && !isLoading,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            if (item.canAfford) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                    ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (item.canAfford) "Buy" else "Can't afford")
                }
            }
        }
    }
}

@Composable
private fun InventoryList(state: ShopScreenState) {
    if (state.isLoadingInventory) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.inventory.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Inventory2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("No items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(state.inventory, key = { it.purchaseId }) { inv ->
            InventoryItemCard(inv)
        }
    }
}

@Composable
private fun InventoryItemCard(inv: InventoryItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconForItemType(inv.item.itemType),
                contentDescription = inv.item.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(inv.item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(inv.item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(inv.purchasedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun iconForItemType(itemType: Int): ImageVector =
    when (itemType) {
        1 -> Icons.Filled.LocalHospital
        2 -> Icons.Filled.Science
        3 -> Icons.Filled.Star
        4 -> Icons.Filled.Shield
        5 -> Icons.Filled.Inventory2
        else -> Icons.Filled.Star
    }
