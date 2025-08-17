package com.example.tradingbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val vm: TradingViewModel = viewModel(factory = TradingViewModel.Factory)
                TradingScreen(vm)
            }
        }
    }
}

@Composable
fun TradingScreen(vm: TradingViewModel) {
    val ui by vm.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trading Bot — Paper") }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Symbol: BTC/USD", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last: $${ui.lastPrice}")
                Text("Pos: ${ui.position}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PnL: $${ui.realizedPnl}")
                Text("Equity: $${ui.equity}")
            }
            Text("Signal: ${ui.lastSignal}")

            Spacer(Modifier.height(16.dp))
            RiskControls(
                positionSizeUsd = ui.positionSizeUsd,
                slPct = ui.stopLossPct,
                tpPct = ui.takeProfitPct,
                onChange = { a, b, c -> vm.onConfigChange(a, b, c) }
            )

            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = { vm.toggle() }) { Text(if (ui.running) "Stop" else "Start") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { vm.reset() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Reset") }
            }

            Spacer(Modifier.height(16.dp))
            Text("Trades", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(ui.trades) { t ->
                    ListItem(
                        headlineContent = { Text("${t.action} ${t.side} ${t.qty} @ $${t.price}") },
                        supportingContent = { Text(t.timestamp) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun RiskControls(
    positionSizeUsd: String,
    slPct: String,
    tpPct: String,
    onChange: (String, String, String) -> Unit
) {
    Column {
        Text("Risk Controls", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = positionSizeUsd,
            onValueChange = { onChange(it, slPct, tpPct) },
            label = { Text("Position Size (USD)") },
            singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = slPct,
            onValueChange = { onChange(positionSizeUsd, it, tpPct) },
            label = { Text("Stop Loss (%)") },
            singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = tpPct,
            onValueChange = { onChange(positionSizeUsd, slPct, it) },
            label = { Text("Take Profit (%)") },
            singleLine = true
        )
    }
}
