package com.example.tradingbot

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class Position { LONG, SHORT, FLAT }

data class Trade(val action: String, val side: Position, val price: Double, val qty: Double, val timestamp: String)

class PaperExecutor(
    private var config: RiskConfig = RiskConfig(),
    private val startingEquity: Double = 10_000.0
) {
    var position: Position = Position.FLAT
        private set

    var realizedPnl: Double = 0.0
        private set

    var equity: Double = startingEquity
        private set

    private var entryPrice: Double? = null
    private var qty: Double = 0.0
    private var stop: Double? = null
    private var tp: Double? = null

    fun reset() {
        position = Position.FLAT
        realizedPnl = 0.0
        equity = startingEquity
        entryPrice = null
        qty = 0.0
        stop = null
        tp = null
    }

    fun updateConfig(newConfig: RiskConfig) { this.config = newConfig }

    /** Call on every price tick to auto-exit at SL/TP */
    fun onTick(price: Double): Trade? {
        if (position == Position.FLAT) return null
        val e = entryPrice ?: return null
        val s = stop
        val t = tp
        return when (position) {
            Position.LONG -> {
                when {
                    s != null && price <= s -> exit(price, "SL hit")
                    t != null && price >= t -> exit(price, "TP hit")
                    else -> null
                }
            }
            Position.SHORT -> {
                when {
                    s != null && price >= s -> exit(price, "SL hit")
                    t != null && price <= t -> exit(price, "TP hit")
                    else -> null
                }
            }
            else -> null
        }
    }

    fun onSignal(sig: Signal, price: Double): Trade? {
        return when (sig) {
            Signal.BUY -> handleBuy(price)
            Signal.SELL -> handleSell(price)
            Signal.HOLD -> null
        }
    }

    private fun handleBuy(price: Double): Trade? {
        return when (position) {
            Position.FLAT -> enter(Position.LONG, price)
            Position.SHORT -> { exit(price, "Reverse to LONG"); enter(Position.LONG, price) }
            Position.LONG -> null
        }
    }

    private fun handleSell(price: Double): Trade? {
        return when (position) {
            Position.FLAT -> enter(Position.SHORT, price)
            Position.LONG -> { exit(price, "Reverse to SHORT"); enter(Position.SHORT, price) }
            Position.SHORT -> null
        }
    }

    private fun enter(newSide: Position, price: Double): Trade {
        position = newSide
        entryPrice = price
        qty = (config.positionSizeUsd / price).coerceAtLeast(0.000001)
        when (newSide) {
            Position.LONG -> {
                stop = price * (1 - config.stopLossPct)
                tp = price * (1 + config.takeProfitPct)
            }
            Position.SHORT -> {
                stop = price * (1 + config.stopLossPct)
                tp = price * (1 - config.takeProfitPct)
            }
            else -> {}
        }
        return tradeLog("ENTER", newSide, price)
    }

    private fun exit(price: Double, reason: String): Trade {
        val e = entryPrice ?: 0.0
        val pnl = when (position) {
            Position.LONG -> (price - e) * qty
            Position.SHORT -> (e - price) * qty
            else -> 0.0
        }
        realizedPnl += pnl
        equity += pnl
        val t = tradeLog("EXIT: $reason", position, price)
        position = Position.FLAT
        entryPrice = null
        qty = 0.0
        stop = null
        tp = null
        return t
    }

    private fun tradeLog(action: String, side: Position, price: Double): Trade {
        val ts = DateTimeFormatter.ISO_INSTANT
            .format(Instant.now().atOffset(ZoneOffset.UTC))
        return Trade(action, side, price, qty, ts)
    }
}
