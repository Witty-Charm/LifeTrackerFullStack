package com.lifetracker.mobile.domain.model

data class ShopItemDomain(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val itemType: Int,
    val effectValue: Int,
)
