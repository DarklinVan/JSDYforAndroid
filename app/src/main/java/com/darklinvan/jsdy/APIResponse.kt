package com.darklinvan.jsdy

import com.google.gson.annotations.SerializedName

data class APIResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: String,

    @SerializedName("itemList")
    val itemList: List<Item>,

    @SerializedName("count")
    val count: Int
)

data class Item(
    @SerializedName("type")
    val type: Int,

    @SerializedName("imgs")
    val imgs: List<Image>?,

    @SerializedName("itemId")
    val itemId: Long,

    @SerializedName("error")
    val error: String?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("desc")
    val desc: String?
)

data class Image(
    @SerializedName("url")
    val url: String,

    @SerializedName("height")
    val height: Int,

    @SerializedName("width")
    val width: Int
)
