package com.example.tradingbot

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PriceRepository {
    private val endpoint = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd"

    fun fetchBtcUsd(): Double {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        conn.inputStream.use { stream ->
            val text = stream.bufferedReader().readText()
            val json = JSONObject(text)
            return json.getJSONObject("bitcoin").getDouble("usd")
        }
    }
}
