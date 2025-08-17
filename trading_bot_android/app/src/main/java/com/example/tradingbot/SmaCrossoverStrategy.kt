package com.example.tradingbot

import java.util.ArrayDeque

enum class Signal { BUY, SELL, HOLD }

class SmaCrossoverStrategy(private val short: Int, private val long: Int) {
    private val prices = ArrayDeque<Double>()

    fun addPrice(p: Double) {
        prices.addLast(p)
        while (prices.size > long) prices.removeFirst()
    }

    fun reset() { prices.clear() }

    private fun sma(window: Int): Double? {
        if (prices.size < window) return null
        return prices.takeLast(window).average()
    }

    fun signal(): Signal {
        val s = sma(short) ?: return Signal.HOLD
        val l = sma(long) ?: return Signal.HOLD
        return when {
            s > l -> Signal.BUY
            s < l -> Signal.SELL
            else -> Signal.HOLD
        }
    }
}
