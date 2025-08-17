package com.example.tradingbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TradingViewModel(
    private val priceRepo: PriceRepository = PriceRepository(),
    private val strategy: SmaCrossoverStrategy = SmaCrossoverStrategy(short = 20, long = 50),
    private val executor: PaperExecutor = PaperExecutor()
) : ViewModel() {

    data class UIState(
        val running: Boolean = false,
        val lastPrice: Double = 0.0,
        val lastSignal: String = "-",
        val position: String = "FLAT",
        val realizedPnl: Double = 0.0,
        val equity: Double = 10_000.0,
        val trades: List<Trade> = emptyList(),
        val positionSizeUsd: String = "200",
        val stopLossPct: String = "1.0",
        val takeProfitPct: String = "2.0"
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private var loop: Job? = null

    fun toggle() { if (_uiState.value.running) stop() else start() }

    private fun start() {
        applyConfig()
        _uiState.value = _uiState.value.copy(running = true)
        loop = viewModelScope.launch {
            while (_uiState.value.running) {
                try {
                    val price = priceRepo.fetchBtcUsd()
                    val auto = executor.onTick(price)
                    val autoTrades = if (auto != null) _uiState.value.trades + auto else _uiState.value.trades

                    strategy.addPrice(price)
                    val signal = strategy.signal()
                    val manual = executor.onSignal(signal, price)

                    val newTrades = if (manual != null) autoTrades + manual else autoTrades
                    _uiState.value = _uiState.value.copy(
                        lastPrice = price,
                        lastSignal = signal.name,
                        position = executor.position.name,
                        realizedPnl = executor.realizedPnl,
                        equity = executor.equity,
                        trades = newTrades
                    )
                } catch (_: Exception) { }
                delay(60_000)
            }
        }
    }

    private fun stop() { _uiState.value = _uiState.value.copy(running = false); loop?.cancel() }

    fun reset() { stop(); strategy.reset(); executor.reset(); _uiState.value = UIState() }

    fun onConfigChange(positionUsd: String? = null, slPct: String? = null, tpPct: String? = null) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            positionSizeUsd = positionUsd ?: cur.positionSizeUsd,
            stopLossPct = slPct ?: cur.stopLossPct,
            takeProfitPct = tpPct ?: cur.takeProfitPct
        )
        applyConfig()
    }

    private fun applyConfig() {
        val u = _uiState.value
        val size = u.positionSizeUsd.toDoubleOrNull() ?: 200.0
        val sl = (u.stopLossPct.toDoubleOrNull() ?: 1.0) / 100.0
        val tp = (u.takeProfitPct.toDoubleOrNull() ?: 2.0) / 100.0
        executor.updateConfig(RiskConfig(size, sl, tp))
    }

    object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TradingViewModel() as T
    }
}
