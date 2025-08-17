package com.example.tradingbot

data class RiskConfig(
    val positionSizeUsd: Double = 200.0,
    val stopLossPct: Double = 0.01,
    val takeProfitPct: Double = 0.02
)
