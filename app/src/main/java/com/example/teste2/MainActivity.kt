package com.example.teste2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teste2.model.AccountDestination
import com.example.teste2.model.CaixinhaInput
import com.example.teste2.model.CardConfig
import com.example.teste2.model.DailyBalance
import com.example.teste2.model.FutureEvent
import com.example.teste2.model.SalaryConfig
import com.example.teste2.model.SimulationTransaction
import com.example.teste2.model.TransactionType
import com.example.teste2.model.TransferSimulation
import com.example.teste2.model.VoucherInput
import com.example.teste2.model.buildStandardEvents
import com.example.teste2.model.parseDateRanges
import com.example.teste2.model.simulateBalances
import com.example.teste2.ui.theme.Teste2Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Teste2Theme(darkTheme = true, dynamicColor = false) {
                FinancePlannerApp()
            }
        }
    }
}

data class Screen(val route: String, val label: String, val icon: @Composable () -> Unit)

private val moneyFormatter = java.text.NumberFormat.getCurrencyInstance().apply {
    maximumFractionDigits = 2
}

@Composable
fun FinancePlannerApp() {
    val screens = listOf(
        Screen("home", "Início") { Icon(Icons.Default.Home, contentDescription = null) },
        Screen("inputs", "Inputs") { Icon(Icons.Default.Settings, contentDescription = null) },
        Screen("simulation", "Simulação") { Icon(Icons.Default.DateRange, contentDescription = null) },
        Screen("dashboard", "Dashboard") { Icon(Icons.Default.Insights, contentDescription = null) }
    )

    var selected by rememberSaveable { mutableStateOf("home") }
    val today = LocalDate.now()

    var checkingBalance by rememberSaveable { mutableStateOf("7500.00") }
    val caixinhas = remember {
        mutableStateListOf(
            CaixinhaInput(name = "Viagem", balance = 1200.0),
            CaixinhaInput(name = "Aluguel", balance = 2300.0)
        )
    }
    val vouchers = remember {
        mutableStateListOf(
            VoucherInput(name = "Vale Refeição 1", balance = 1173.26, creditDay = null),
            VoucherInput(name = "Vale Alimentação", balance = 924.47, creditDay = null)
        )
    }
    var salary by rememberSaveable { mutableStateOf("8500.00") }
    var salaryDay by rememberSaveable { mutableStateOf("5") }
    var cardDebt by rememberSaveable { mutableStateOf("2500.00") }
    var cardClosing by rememberSaveable { mutableStateOf("10") }

    var simulationStart by rememberSaveable { mutableStateOf(today.toString()) }
    var simulationEnd by rememberSaveable { mutableStateOf(today.plusDays(29).toString()) }

    val simulatedTransactions = remember { mutableStateListOf<SimulationTransaction>() }
    val transferSimulations = remember { mutableStateListOf<TransferSimulation>() }

    val startDate = remember(simulationStart) { parseDateOr(today, simulationStart) }
    val endDate = remember(simulationEnd) { parseDateOr(today.plusDays(29), simulationEnd) }

    val salaryConfig = SalaryConfig(amount = salary.toDoubleOrZero(), payDay = salaryDay.toIntOrZero(5))
    val cardConfig = CardConfig(debt = cardDebt.toDoubleOrZero(), closingDay = cardClosing.toIntOrZero(10))

    val standardEvents = remember(startDate, endDate, salaryConfig, cardConfig, vouchers.toList()) {
        buildStandardEvents(startDate, endDate, salaryConfig, cardConfig, vouchers)
    }

    val balances by remember(startDate, endDate, checkingBalance, caixinhas.toList(), vouchers.toList(), salaryConfig, cardConfig, simulatedTransactions.toList(), transferSimulations.toList()) {
        mutableStateOf(
            simulateBalances(
                start = startDate,
                end = endDate,
                checkingBalance = checkingBalance.toDoubleOrZero(),
                caixinhas = caixinhas,
                vouchers = vouchers,
                salaryConfig = salaryConfig,
                cardConfig = cardConfig,
                simulatedTransactions = simulatedTransactions,
                transferSimulations = transferSimulations
            )
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = selected == screen.route,
                        onClick = { selected = screen.route },
                        icon = screen.icon,
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (selected) {
                "home" -> HomeScreen(balances, standardEvents, simulatedTransactions, caixinhas.size)
                "inputs" -> InputScreen(
                    checkingBalance = checkingBalance,
                    onCheckingChange = { checkingBalance = it },
                    salary = salary,
                    onSalaryChange = { salary = it },
                    salaryDay = salaryDay,
                    onSalaryDayChange = { salaryDay = it },
                    cardDebt = cardDebt,
                    onCardDebtChange = { cardDebt = it },
                    cardClosing = cardClosing,
                    onCardClosingChange = { cardClosing = it },
                    caixinhas = caixinhas,
                    vouchers = vouchers
                )
                "simulation" -> SimulationScreen(
                    startDateInput = simulationStart,
                    endDateInput = simulationEnd,
                    onStartDateChange = { simulationStart = it },
                    onEndDateChange = { simulationEnd = it },
                    standardEvents = standardEvents,
                    simulatedTransactions = simulatedTransactions,
                    transferSimulations = transferSimulations,
                    caixinhas = caixinhas,
                    vouchers = vouchers,
                    balances = balances,
                    onClearSimulated = {
                        simulatedTransactions.clear()
                        transferSimulations.clear()
                    }
                )
                else -> DashboardScreen(
                    balances = balances,
                    startDateInput = simulationStart,
                    endDateInput = simulationEnd,
                    onStartDateChange = { simulationStart = it },
                    onEndDateChange = { simulationEnd = it }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    balances: List<DailyBalance>,
    standardEvents: List<FutureEvent>,
    simulatedTransactions: List<SimulationTransaction>,
    caixinhasCount: Int
) {
    val latest = balances.lastOrNull()
    val first = balances.firstOrNull()
    val totalCaixinhas = latest?.totalCaixinhas ?: 0.0
    val checking = latest?.checking ?: 0.0
    val vales = latest?.totalVales ?: 0.0
    val card = latest?.cardDebt ?: 0.0
    val total = checking + totalCaixinhas

    Text("Visão Geral", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Conta Corrente", checking, modifier = Modifier.weight(1f))
        SummaryCard("Caixinhas ($caixinhasCount)", totalCaixinhas, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Vales", vales, modifier = Modifier.weight(1f))
        SummaryCard("Dívida Cartão", -card, emphasizeNegative = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    SummaryCard("Liquidez (CC + Caixinhas)", total, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(24.dp))
    Text("Próximas transações padrão", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    FutureEventsList(standardEvents)

    Spacer(Modifier.height(16.dp))
    Text("Simulações ativas", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    if (simulatedTransactions.isEmpty()) {
        Text("Nenhuma simulação cadastrada.")
    } else {
        simulatedTransactions.take(3).forEach { tx ->
            InfoCard(title = tx.name, subtitle = "${tx.type.name.lowercase().replaceFirstChar { it.titlecase() }} em ${tx.sourceAccount}", value = tx.amount)
            Spacer(Modifier.height(8.dp))
        }
    }

    Spacer(Modifier.height(16.dp))
    if (first != null && latest != null) {
        val delta = latest.liquidFunds - first.liquidFunds
        SummaryCard(
            title = "Variação no período",
            value = delta,
            modifier = Modifier.fillMaxWidth(),
            emphasizeNegative = true,
            subtitle = "Liquidez: ${formatMoney(first.liquidFunds)} → ${formatMoney(latest.liquidFunds)}"
        )
    }
}

@Composable
private fun InputScreen(
    checkingBalance: String,
    onCheckingChange: (String) -> Unit,
    salary: String,
    onSalaryChange: (String) -> Unit,
    salaryDay: String,
    onSalaryDayChange: (String) -> Unit,
    cardDebt: String,
    onCardDebtChange: (String) -> Unit,
    cardClosing: String,
    onCardClosingChange: (String) -> Unit,
    caixinhas: MutableList<CaixinhaInput>,
    vouchers: MutableList<VoucherInput>
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("Conta Corrente")
            OutlinedTextField(
                value = checkingBalance,
                onValueChange = onCheckingChange,
                label = { Text("Saldo atual") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = numberKeyboard()
            )
        }
        item {
            SectionTitle("Salário")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = salary,
                    onValueChange = onSalaryChange,
                    label = { Text("Valor") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
                OutlinedTextField(
                    value = salaryDay,
                    onValueChange = onSalaryDayChange,
                    label = { Text("Dia do crédito") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
            }
        }
        item {
            SectionTitle("Cartão de Crédito")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cardDebt,
                    onValueChange = onCardDebtChange,
                    label = { Text("Saldo da fatura") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
                OutlinedTextField(
                    value = cardClosing,
                    onValueChange = onCardClosingChange,
                    label = { Text("Dia de fechamento") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
            }
        }
        item {
            SectionTitle("Caixinhas CDB")
            caixinhas.forEach { caixinha ->
                EditableCard(
                    title = caixinha.name,
                    value = caixinha.balance,
                    onValueChange = { newValue ->
                        val index = caixinhas.indexOfFirst { it.id == caixinha.id }
                        if (index != -1) {
                            caixinhas[index] = caixinha.copy(balance = newValue.toDoubleOrZero())
                        }
                    },
                    onDelete = { caixinhas.remove(caixinha) }
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = {
                caixinhas.add(
                    CaixinhaInput(
                        name = "Nova Caixinha ${caixinhas.size + 1}",
                        balance = 0.0
                    )
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text("Adicionar caixinha")
            }
        }
        item {
            SectionTitle("Vales")
            vouchers.forEach { voucher ->
                EditableVoucherCard(
                    voucher = voucher,
                    onChange = { updated ->
                        val index = vouchers.indexOfFirst { it.id == voucher.id }
                        if (index != -1) vouchers[index] = updated
                    },
                    onDelete = { vouchers.remove(voucher) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SimulationScreen(
    startDateInput: String,
    endDateInput: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    standardEvents: List<FutureEvent>,
    simulatedTransactions: MutableList<SimulationTransaction>,
    transferSimulations: MutableList<TransferSimulation>,
    caixinhas: List<CaixinhaInput>,
    vouchers: List<VoucherInput>,
    balances: List<DailyBalance>,
    onClearSimulated: () -> Unit
) {
    var simulationName by rememberSaveable { mutableStateOf("") }
    var simulationAmount by rememberSaveable { mutableStateOf("") }
    var simulationDates by rememberSaveable { mutableStateOf("") }
    var simulationType by rememberSaveable { mutableStateOf(TransactionType.DEBIT) }
    var simulationSource by rememberSaveable { mutableStateOf(AccountDestination.CHECKING) }
    var simulationAccountLabel by rememberSaveable { mutableStateOf("") }

    var transferName by rememberSaveable { mutableStateOf("") }
    var transferAmount by rememberSaveable { mutableStateOf("") }
    var transferDates by rememberSaveable { mutableStateOf("") }
    var transferFrom by rememberSaveable { mutableStateOf(AccountDestination.CHECKING) }
    var transferTo by rememberSaveable { mutableStateOf(AccountDestination.CAIXINHA) }
    var transferFromLabel by rememberSaveable { mutableStateOf("") }
    var transferToLabel by rememberSaveable { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("Período da simulação")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = onStartDateChange,
                    label = { Text("Data inicial (AAAA-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDateInput,
                    onValueChange = onEndDateChange,
                    label = { Text("Data final (AAAA-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        item {
            SectionTitle("Transações simuladas")
            OutlinedTextField(
                value = simulationName,
                onValueChange = { simulationName = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = simulationAmount,
                    onValueChange = { simulationAmount = it },
                    label = { Text("Valor") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
                AssistChip(
                    onClick = {
                        simulationType = if (simulationType == TransactionType.DEBIT) TransactionType.CREDIT else TransactionType.DEBIT
                    },
                    label = { Text("Tipo: ${if (simulationType == TransactionType.DEBIT) "Débito" else "Crédito"}") }
                )
                AssistChip(
                    onClick = {
                        simulationSource = when (simulationSource) {
                            AccountDestination.CHECKING -> AccountDestination.CAIXINHA
                            AccountDestination.CAIXINHA -> AccountDestination.VALE
                            AccountDestination.VALE -> AccountDestination.CHECKING
                        }
                    },
                    label = { Text("Origem: ${simulationSource.name}") }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = simulationAccountLabel,
                onValueChange = { simulationAccountLabel = it },
                label = { Text("Caixinha/vale específico (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = simulationDates,
                onValueChange = { simulationDates = it },
                label = { Text("Datas (AAAA-MM-DD; AAAA-MM-DD:AAAA-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val (ranges, error) = parseDateRanges(simulationDates)
                if (simulationName.isNotBlank() && error == null && ranges.isNotEmpty()) {
                    simulatedTransactions.add(
                        SimulationTransaction(
                            name = simulationName,
                            amount = simulationAmount.toDoubleOrZero(),
                            dateRanges = ranges,
                            type = simulationType,
                            sourceAccount = simulationSource,
                            sourceName = simulationAccountLabel.takeIf { it.isNotBlank() }
                        )
                    )
                    simulationName = ""
                    simulationAmount = ""
                    simulationDates = ""
                    simulationAccountLabel = ""
                }
            }) {
                Text("Adicionar transação")
            }
        }
        item {
            SectionTitle("Transferências simuladas")
            OutlinedTextField(
                value = transferName,
                onValueChange = { transferName = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = transferAmount,
                    onValueChange = { transferAmount = it },
                    label = { Text("Valor") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
                AssistChip(onClick = {
                    transferFrom = nextAccount(transferFrom)
                }, label = { Text("De: ${transferFrom.name}") })
                AssistChip(onClick = {
                    transferTo = nextAccount(transferTo)
                }, label = { Text("Para: ${transferTo.name}") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = transferFromLabel,
                    onValueChange = { transferFromLabel = it },
                    label = { Text("Nome origem (opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = transferToLabel,
                    onValueChange = { transferToLabel = it },
                    label = { Text("Nome destino (opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = transferDates,
                onValueChange = { transferDates = it },
                label = { Text("Datas (AAAA-MM-DD; AAAA-MM-DD:AAAA-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val (ranges, error) = parseDateRanges(transferDates)
                if (transferName.isNotBlank() && error == null && ranges.isNotEmpty()) {
                    transferSimulations.add(
                        TransferSimulation(
                            name = transferName,
                            amount = transferAmount.toDoubleOrZero(),
                            dateRanges = ranges,
                            from = transferFrom,
                            to = transferTo,
                            fromName = transferFromLabel.takeIf { it.isNotBlank() },
                            toName = transferToLabel.takeIf { it.isNotBlank() }
                        )
                    )
                    transferName = ""
                    transferAmount = ""
                    transferDates = ""
                    transferFromLabel = ""
                    transferToLabel = ""
                }
            }) {
                Text("Adicionar transferência")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClearSimulated) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text("Limpar simulações")
                }
                AssistChip(onClick = { if (simulatedTransactions.isNotEmpty()) simulatedTransactions.removeLast() }, label = { Text("Remover última transação") })
            }
        }
        item {
            SectionTitle("Transações futuras padrão")
            FutureEventsList(standardEvents)
        }
        item {
            SectionTitle("Transações simuladas")
            if (simulatedTransactions.isEmpty() && transferSimulations.isEmpty()) {
                Text("Nenhuma simulação cadastrada.")
            } else {
                simulatedTransactions.forEach { tx ->
                    InfoCard(
                        title = tx.name,
                        subtitle = "${tx.type} - ${tx.sourceAccount}${tx.sourceName?.let { " · $it" } ?: ""}",
                        value = if (tx.type == TransactionType.DEBIT) -tx.amount else tx.amount,
                        onDelete = { simulatedTransactions.remove(tx) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                transferSimulations.forEach { transfer ->
                    InfoCard(
                        title = transfer.name,
                        subtitle = "${transfer.from} ${transfer.fromName ?: ""} → ${transfer.to} ${transfer.toName ?: ""}",
                        value = transfer.amount,
                        onDelete = { transferSimulations.remove(transfer) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item {
            SectionTitle("Tabela diária (30 dias por padrão)")
            BalanceTable(balances = balances.take(30))
        }
    }
}

@Composable
private fun DashboardScreen(
    balances: List<DailyBalance>,
    startDateInput: String,
    endDateInput: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    if (balances.isEmpty()) {
        Text("Cadastre entradas para visualizar o dashboard.")
        return
    }
    val start = balances.first()
    val end = balances.last()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Período e filtros")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startDateInput,
                onValueChange = onStartDateChange,
                label = { Text("Data inicial") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = endDateInput,
                onValueChange = onEndDateChange,
                label = { Text("Data final") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        SectionTitle("Visão 1 - Totais")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VariationCard("Total", end.liquidFunds, start.liquidFunds, modifier = Modifier.weight(1f))
            VariationCard("Conta Corrente", end.checking, start.checking, modifier = Modifier.weight(1f))
            VariationCard("Caixinhas", end.totalCaixinhas, start.totalCaixinhas, modifier = Modifier.weight(1f))
        }

        SectionTitle("Visão 2 - Caixinhas")
        CaixinhaGrid(start, end)

        SectionTitle("Visão 3 - Faixa diária")
        BandChart(
            totals = balances.map { it.date to it.liquidFunds },
            checking = balances.map { it.date to it.checking },
            caixinhas = balances.map { it.date to it.totalCaixinhas }
        )

        SectionTitle("Visão 4 - Variação diária")
        Sparkline(
            label = "Total vs dia anterior",
            values = balances.windowed(2, 1).map { (prev, current) ->
                current.date to (current.liquidFunds - prev.liquidFunds)
            }
        )
        Sparkline(
            label = "Total vs primeiro dia",
            values = balances.map { it.date to (it.liquidFunds - start.liquidFunds) }
        )
    }
}

@Composable
private fun VariationCard(title: String, endValue: Double, startValue: Double, modifier: Modifier = Modifier.fillMaxWidth()) {
    val delta = endValue - startValue
    val isPositive = delta >= 0
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(endValue), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${if (isPositive) "▲" else "▼"} ${formatMoney(delta)}",
                color = if (isPositive) positiveColor() else negativeColor(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CaixinhaGrid(start: DailyBalance, end: DailyBalance) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VariationCard("Total Caixinhas", end.totalCaixinhas, start.totalCaixinhas)
        end.caixinhas.keys.sorted().forEach { name ->
            val endValue = end.caixinhas[name] ?: 0.0
            val startValue = start.caixinhas[name] ?: 0.0
            VariationCard(name, endValue, startValue)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BandChart(
    totals: List<Pair<LocalDate, Double>>,
    checking: List<Pair<LocalDate, Double>>,
    caixinhas: List<Pair<LocalDate, Double>>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
) {
    val maxValue = (totals.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val minValue = (totals.minOfOrNull { it.second } ?: 0.0).coerceAtMost(0.0)
    val range = (maxValue - minValue).takeIf { it != 0.0 } ?: 1.0
    val points = totals.size.coerceAtLeast(1)

    Column(modifier.padding(12.dp)) {
        Text("Faixa de saldos", style = MaterialTheme.typography.labelLarge)
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 8.dp)
        ) {
            val widthStep = size.width / points
            totals.forEachIndexed { index, pair ->
                val heightFactor = (pair.second - minValue) / range
                val barHeight = size.height * heightFactor.toFloat()
                drawRoundRect(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    topLeft = androidx.compose.ui.geometry.Offset(x = widthStep * index, y = size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(widthStep * 0.6f, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            checking.forEachIndexed { index, pair ->
                val y = size.height - (((pair.second - minValue) / range) * size.height).toFloat()
                drawCircle(color = MaterialTheme.colorScheme.secondary, radius = 6f, center = androidx.compose.ui.geometry.Offset(widthStep * index, y))
            }
            caixinhas.forEachIndexed { index, pair ->
                val y = size.height - (((pair.second - minValue) / range) * size.height).toFloat()
                drawCircle(color = MaterialTheme.colorScheme.tertiary, radius = 5f, center = androidx.compose.ui.geometry.Offset(widthStep * index + 10, y))
            }
        }
    }
}

@Composable
private fun Sparkline(label: String, values: List<Pair<LocalDate, Double>>) {
    val maxValue = (values.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val minValue = (values.minOfOrNull { it.second } ?: 0.0)
    val range = (maxValue - minValue).takeIf { it != 0.0 } ?: 1.0
    val points = values.size.coerceAtLeast(1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = size.width / points
                var previous: androidx.compose.ui.geometry.Offset? = null
                values.forEachIndexed { index, pair ->
                    val y = size.height - (((pair.second - minValue) / range) * size.height).toFloat()
                    val point = androidx.compose.ui.geometry.Offset(step * index, y)
                    if (previous != null) {
                        drawLine(
                            color = MaterialTheme.colorScheme.primary,
                            start = previous!!,
                            end = point,
                            strokeWidth = 4f
                        )
                    }
                    drawCircle(color = MaterialTheme.colorScheme.secondary, radius = 5f, center = point)
                    previous = point
                }
            }
        }
    }
}

@Composable
private fun BalanceTable(balances: List<DailyBalance>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Data", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("CC", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Caixinhas", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Vales", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Cartão", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Divider(Modifier.padding(vertical = 8.dp))
            balances.forEach { balance ->
                val green = positiveColor()
                val red = negativeColor()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(balance.date.format(DateTimeFormatter.ISO_LOCAL_DATE), modifier = Modifier.weight(1f))
                    ColoredValue(balance.checking, Modifier.weight(1f), green, red)
                    ColoredValue(balance.totalCaixinhas, Modifier.weight(1f), green, red)
                    ColoredValue(balance.totalVales, Modifier.weight(1f), green, red)
                    ColoredValue(-balance.cardDebt, Modifier.weight(1f), green, red)
                }
                Divider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ColoredValue(value: Double, modifier: Modifier, positive: Color, negative: Color) {
    Text(
        text = formatMoney(value),
        modifier = modifier,
        textAlign = TextAlign.End,
        color = if (value >= 0) positive else negative
    )
}

@Composable
private fun FutureEventsList(events: List<FutureEvent>) {
    if (events.isEmpty()) {
        Text("Nada planejado no período")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.sortedBy { it.date }.forEach { event ->
            InfoCard(
                title = event.description,
                subtitle = "${event.date} · ${event.destination}",
                value = event.amount
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, value: Double, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                formatMoney(value),
                color = if (value >= 0) positiveColor() else negativeColor(),
                modifier = Modifier.padding(end = if (onDelete != null) 8.dp else 0.dp)
            )
            onDelete?.let {
                IconButton(onClick = it) { Icon(Icons.Default.Delete, contentDescription = "Remover") }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: Double,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    emphasizeNegative: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(value), style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            val color = if (value >= 0 || !emphasizeNegative) positiveColor() else negativeColor()
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = color
            )
        }
    }
}

@Composable
private fun EditableCard(title: String, value: Double, onValueChange: (String) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir") }
            }
            OutlinedTextField(
                value = value.toString(),
                onValueChange = onValueChange,
                label = { Text("Saldo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = numberKeyboard()
            )
        }
    }
}

@Composable
private fun EditableVoucherCard(
    voucher: VoucherInput,
    onChange: (VoucherInput) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(voucher.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = voucher.balance.toString(),
                    onValueChange = { onChange(voucher.copy(balance = it.toDoubleOrZero())) },
                    label = { Text("Saldo") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
                OutlinedTextField(
                    value = voucher.creditDay?.toString().orEmpty(),
                    onValueChange = { onChange(voucher.copy(creditDay = it.toIntOrNull())) },
                    label = { Text("Dia do crédito (opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = numberKeyboard()
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

private fun formatMoney(value: Double): String = moneyFormatter.format(value)

private fun String.toDoubleOrZero(): Double = toDoubleOrNull() ?: 0.0
private fun String.toIntOrZero(fallback: Int = 1): Int = toIntOrNull() ?: fallback

private fun parseDateOr(default: LocalDate, input: String): LocalDate =
    runCatching { LocalDate.parse(input) }.getOrElse { default }

@Composable
private fun numberKeyboard() = KeyboardOptions(keyboardType = KeyboardType.Number)

private fun nextAccount(current: AccountDestination): AccountDestination = when (current) {
    AccountDestination.CHECKING -> AccountDestination.CAIXINHA
    AccountDestination.CAIXINHA -> AccountDestination.VALE
    AccountDestination.VALE -> AccountDestination.CHECKING
}

private fun positiveColor(): Color = Color(0xFF8FE1A3)

private fun negativeColor(): Color = Color(0xFFFF6B6B)

@Preview(showBackground = true)
@Composable
fun PreviewFinanceApp() {
    Teste2Theme(darkTheme = true, dynamicColor = false) {
        FinancePlannerApp()
    }
}
