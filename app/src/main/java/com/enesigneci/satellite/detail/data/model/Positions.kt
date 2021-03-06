package com.enesigneci.satellite.detail.data.model


import com.google.gson.annotations.SerializedName

data class Positions(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("positions")
    val positions: List<Position>? = null
)