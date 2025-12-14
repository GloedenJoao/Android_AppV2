package com.example.teste2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private enum class FinanceDestination(val label: String) {
    HOME("Início"), INPUTS("Inputs"), SIMULATION("Simulação"), DASHBOARD("Dashboard")
}

@Composable
fun FinancePlannerApp() {
    val caixinhas = remember {
        mutableStateListOf(
            Caixinha(name = "Viagem", balance = 0.0),
            Caixinha(name = "Aluguel", balance = 0.0)
        )
    }
    val vales = remember {
        mutableStateListOf(
            Vale(name = "Vale Refeição 1", balance = 1173.26, creditDay = 0, creditValue = 1173.26),
            Vale(name = "Vale Alimentação", balance = 924.47, creditDay = 0, creditValue = 924.47)
        )
    }
    val simulatedTransactions = remember { mutableStateListOf<SimulatedTransaction>() }
    val transfers = remember { mutableStateListOf<TransferEvent>() }

    var checkingBalance by rememberSaveable { mutableStateOf(0.0) }
    var salaryAmount by rememberSaveable { mutableStateOf(0.0) }
    var salaryDay by rememberSaveable { mutableStateOf(5) }
    var creditCardDebt by rememberSaveable { mutableStateOf(0.0) }
    var creditCardClosingDay by rememberSaveable { mutableStateOf(10) }

    var startDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateText by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).toString()) }

    val startDate = parseLocalDateOr(LocalDate.now(), startDateText)
    val endDate = parseLocalDateOr(LocalDate.now().plusDays(30), endDateText)

    val salaryConfig = SalaryConfig(salaryAmount, salaryDay)
    val creditCardConfig = CreditCardConfig(creditCardDebt, creditCardClosingDay)

    val dailyBalances by remember {
        androidx.compose.runtime.derivedStateOf {
            generateDailyBalances(
                checkingBalance,
                caixinhas,
                vales,
                salaryConfig,
                creditCardConfig,
                simulatedTransactions,
                transfers,
                startDate,
                endDate
            )
        }
    }

    var destination by rememberSaveable { mutableStateOf(FinanceDestination.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "Planner Financeiro") })
        },
        bottomBar = {
            NavigationBar {
                FinanceDestination.values().forEach { dest ->
                    NavigationBarItem(
                        selected = destination == dest,
                        onClick = { destination = dest },
                        icon = {
                            when (dest) {
                                FinanceDestination.HOME -> Icon(Icons.Default.Home, contentDescription = null)
                                FinanceDestination.INPUTS -> Icon(Icons.Default.Input, contentDescription = null)
                                FinanceDestination.SIMULATION -> Icon(Icons.Default.Tune, contentDescription = null)
                                FinanceDestination.DASHBOARD -> Icon(Icons.Default.ShowChart, contentDescription = null)
                            }
                        },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (destination) {
            FinanceDestination.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                checkingBalance = checkingBalance,
                caixinhas = caixinhas,
                vales = vales,
                creditCardDebt = creditCardDebt
            )
            FinanceDestination.INPUTS -> InputsScreen(
                modifier = Modifier.padding(padding),
                checkingBalance = checkingBalance,
                onCheckingChange = { checkingBalance = it },
                caixinhas = caixinhas,
                onCaixinhaChange = { index, updated -> caixinhas[index] = updated },
                onRemoveCaixinha = { caixinhas.removeAt(it) },
                onAddCaixinha = { caixinhas.add(it) },
                vales = vales,
                onValeChange = { index, updated -> vales[index] = updated },
                onRemoveVale = { vales.removeAt(it) },
                onAddVale = { vales.add(it) },
                salaryAmount = salaryAmount,
                salaryDay = salaryDay,
                onSalaryChange = { amount, day ->
                    salaryAmount = amount
                    salaryDay = day
                },
                creditCardDebt = creditCardDebt,
                creditCardClosingDay = creditCardClosingDay,
                onCreditCardChange = { debt, day ->
                    creditCardDebt = debt
                    creditCardClosingDay = day
                },
                startDateText = startDateText,
                endDateText = endDateText,
                onPeriodChange = { start, end ->
                    startDateText = start
                    endDateText = end
                }
            )
            FinanceDestination.SIMULATION -> SimulationScreen(
                modifier = Modifier.padding(padding),
                caixinhas = caixinhas,
                vales = vales,
                simulatedTransactions = simulatedTransactions,
                onAddSimulation = { simulatedTransactions.add(it) },
                onRemoveSimulation = { tx -> simulatedTransactions.remove(tx) },
                transfers = transfers,
                onAddTransfer = { transfers.add(it) },
                onRemoveTransfer = { transfer -> transfers.remove(transfer) },
                salary = salaryConfig,
                creditCard = creditCardConfig,
                startDate = startDate,
                endDate = endDate,
                dailyBalances = dailyBalances
            )
            FinanceDestination.DASHBOARD -> DashboardScreen(
                modifier = Modifier.padding(padding),
                caixinhas = caixinhas,
                vales = vales,
                dailyBalances = dailyBalances,
                startDate = startDate,
                endDate = endDate
            )
        }
    }
}

private fun parseLocalDateOr(fallback: LocalDate, raw: String): LocalDate =
    runCatching { LocalDate.parse(raw.trim()) }.getOrDefault(fallback)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    checkingBalance: Double,
    caixinhas: List<Caixinha>,
    vales: List<Vale>,
    creditCardDebt: Double
) {
    val caixinhasTotal = caixinhas.sumOf { it.balance }
    val valesTotal = vales.sumOf { it.balance }
    val totalLiquido = checkingBalance + caixinhasTotal
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Visão Geral",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        OverviewCard(title = "Conta Corrente", value = checkingBalance)
        OverviewCard(title = "Total Caixinhas CDB", value = caixinhasTotal)
        OverviewCard(title = "Total Vales", value = valesTotal)
        OverviewCard(title = "Conta Corrente + Caixinhas", value = totalLiquido)
        OverviewCard(title = "Dívida Cartão", value = -creditCardDebt, emphasize = true)
    }
}

@Composable
fun OverviewCard(title: String, value: Double, emphasize: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value.formatCurrency(),
                style = MaterialTheme.typography.headlineSmall,
                color = if (value >= 0 || emphasize.not()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun InputsScreen(
    modifier: Modifier = Modifier,
    checkingBalance: Double,
    onCheckingChange: (Double) -> Unit,
    caixinhas: List<Caixinha>,
    onCaixinhaChange: (Int, Caixinha) -> Unit,
    onRemoveCaixinha: (Int) -> Unit,
    onAddCaixinha: (Caixinha) -> Unit,
    vales: List<Vale>,
    onValeChange: (Int, Vale) -> Unit,
    onRemoveVale: (Int) -> Unit,
    onAddVale: (Vale) -> Unit,
    salaryAmount: Double,
    salaryDay: Int,
    onSalaryChange: (Double, Int) -> Unit,
    creditCardDebt: Double,
    creditCardClosingDay: Int,
    onCreditCardChange: (Double, Int) -> Unit,
    startDateText: String,
    endDateText: String,
    onPeriodChange: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Inputs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conta Corrente", fontWeight = FontWeight.SemiBold)
                CurrencyField("Saldo atual", checkingBalance) { onCheckingChange(it) }
            }
        }
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Caixinhas CDB", fontWeight = FontWeight.SemiBold)
                caixinhas.forEachIndexed { index, caixinha ->
                    CaixinhaInput(caixinha) { updated -> onCaixinhaChange(index, updated) }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onRemoveCaixinha(index) }) { Text("Excluir") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(onClick = { onAddCaixinha(Caixinha(name = "Nova caixinha", balance = 0.0)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Adicionar caixinha")
                }
            }
        }
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vales", fontWeight = FontWeight.SemiBold)
                vales.forEachIndexed { index, vale ->
                    ValeInput(vale) { updated -> onValeChange(index, updated) }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onRemoveVale(index) }) { Text("Excluir") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TextButton(onClick = { onAddVale(Vale(name = "Novo vale", balance = 0.0, creditDay = 0, creditValue = 0.0)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Adicionar vale")
                }
            }
        }
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Salário", fontWeight = FontWeight.SemiBold)
                CurrencyField("Valor", salaryAmount) { amount -> onSalaryChange(amount, salaryDay) }
                IntegerField("Dia do crédito", salaryDay) { day -> onSalaryChange(salaryAmount, day) }
                Text(
                    "Se cair no fim de semana, o crédito é antecipado para o dia útil anterior mais próximo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cartão de Crédito", fontWeight = FontWeight.SemiBold)
                CurrencyField("Dívida atual", creditCardDebt) { debt ->
                    onCreditCardChange(debt, creditCardClosingDay)
                }
                IntegerField("Dia de fechamento da fatura", creditCardClosingDay) { day ->
                    onCreditCardChange(creditCardDebt, day)
                }
            }
        }
        OutlinedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Período de simulação", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { onPeriodChange(it, endDateText) },
                    label = { Text("Data inicial (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { onPeriodChange(startDateText, it) },
                    label = { Text("Data final (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CaixinhaInput(caixinha: Caixinha, onChange: (Caixinha) -> Unit) {
    OutlinedTextField(
        value = caixinha.name,
        onValueChange = { onChange(caixinha.copy(name = it)) },
        label = { Text("Nome da caixinha") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    CurrencyField("Saldo", caixinha.balance) { value -> onChange(caixinha.copy(balance = value)) }
}

@Composable
private fun ValeInput(vale: Vale, onChange: (Vale) -> Unit) {
    OutlinedTextField(
        value = vale.name,
        onValueChange = { onChange(vale.copy(name = it)) },
        label = { Text("Nome do vale") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    CurrencyField("Saldo", vale.balance) { value -> onChange(vale.copy(balance = value)) }
    CurrencyField("Saldo padrão de recarga", vale.creditValue) { value -> onChange(vale.copy(creditValue = value)) }
    IntegerField("Dia padrão de crédito (0 = penúltimo útil)", vale.creditDay) { day ->
        onChange(vale.copy(creditDay = day))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    modifier: Modifier = Modifier,
    caixinhas: List<Caixinha>,
    vales: List<Vale>,
    simulatedTransactions: List<SimulatedTransaction>,
    onAddSimulation: (SimulatedTransaction) -> Unit,
    onRemoveSimulation: (SimulatedTransaction) -> Unit,
    transfers: List<TransferEvent>,
    onAddTransfer: (TransferEvent) -> Unit,
    onRemoveTransfer: (TransferEvent) -> Unit,
    salary: SalaryConfig,
    creditCard: CreditCardConfig,
    startDate: LocalDate,
    endDate: LocalDate,
    dailyBalances: List<DailyBalance>
) {
    var name by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    var dates by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.DEBIT) }
    var source by rememberSaveable { mutableStateOf(BalanceSource.CHECKING) }
    var selectedCaixinha by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVale by rememberSaveable { mutableStateOf<String?>(null) }

    var transferName by rememberSaveable { mutableStateOf("") }
    var transferValue by rememberSaveable { mutableStateOf("") }
    var transferDates by rememberSaveable { mutableStateOf("") }
    var sourceTransfer by rememberSaveable { mutableStateOf(BalanceSource.CHECKING) }
    var targetTransfer by rememberSaveable { mutableStateOf(BalanceSource.CAIXINHA) }
    var sourceCaixinha by rememberSaveable { mutableStateOf<String?>(null) }
    var targetCaixinha by rememberSaveable { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Simulação", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        ElevatedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transação simulada", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                CurrencyField("Valor", value.toDoubleOrZero()) { value = it.toString() }
                OutlinedTextField(value = dates, onValueChange = { dates = it }, label = { Text("Datas (YYYY-MM-DD ou intervalo 2024-12-15..2024-12-20)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { type = TransactionType.DEBIT }, label = { Text("Débito") }, leadingIcon = { if (type == TransactionType.DEBIT) Icon(Icons.Default.Delete, contentDescription = null) })
                    AssistChip(onClick = { type = TransactionType.CREDIT }, label = { Text("Crédito") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { source = BalanceSource.CHECKING }, label = { Text("Conta Corrente") })
                    AssistChip(onClick = { source = BalanceSource.CAIXINHA }, label = { Text("Caixinha") })
                    AssistChip(onClick = { source = BalanceSource.VALE }, label = { Text("Vale") })
                }
                if (source == BalanceSource.CAIXINHA) {
                    DropdownSelector(
                        label = "Escolha a caixinha",
                        options = caixinhas.associate { it.id to it.name },
                        selected = selectedCaixinha,
                        onSelect = { selectedCaixinha = it }
                    )
                }
                if (source == BalanceSource.VALE) {
                    DropdownSelector(
                        label = "Escolha o vale",
                        options = vales.associate { it.id to it.name },
                        selected = selectedVale,
                        onSelect = { selectedVale = it }
                    )
                }
                TextButton(onClick = {
                    val parsedDates = parseDateRanges(dates)
                    if (parsedDates.isNotEmpty() && value.toDoubleOrZero() != 0.0) {
                        onAddSimulation(
                            SimulatedTransaction(
                                name = name.ifBlank { "Simulação" },
                                amount = value.toDoubleOrZero(),
                                dates = parsedDates,
                                type = type,
                                source = source,
                                caixinhaId = selectedCaixinha,
                                valeId = selectedVale
                            )
                        )
                        name = ""
                        value = ""
                        dates = ""
                        selectedCaixinha = null
                        selectedVale = null
                    }
                }) {
                    Text("Adicionar transação")
                }
            }
        }

        ElevatedCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transferências entre contas/caixinhas", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = transferName, onValueChange = { transferName = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                CurrencyField("Valor", transferValue.toDoubleOrZero()) { transferValue = it.toString() }
                OutlinedTextField(value = transferDates, onValueChange = { transferDates = it }, label = { Text("Datas (YYYY-MM-DD ou intervalo)") }, modifier = Modifier.fillMaxWidth())
                Text("De onde sai")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { sourceTransfer = BalanceSource.CHECKING }, label = { Text("Conta Corrente") })
                    AssistChip(onClick = { sourceTransfer = BalanceSource.CAIXINHA }, label = { Text("Caixinha") })
                }
                if (sourceTransfer == BalanceSource.CAIXINHA) {
                    DropdownSelector(label = "Caixinha de origem", options = caixinhas.associate { it.id to it.name }, selected = sourceCaixinha, onSelect = { sourceCaixinha = it })
                }
                Text("Para onde vai")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { targetTransfer = BalanceSource.CHECKING }, label = { Text("Conta Corrente") })
                    AssistChip(onClick = { targetTransfer = BalanceSource.CAIXINHA }, label = { Text("Caixinha") })
                }
                if (targetTransfer == BalanceSource.CAIXINHA) {
                    DropdownSelector(label = "Caixinha de destino", options = caixinhas.associate { it.id to it.name }, selected = targetCaixinha, onSelect = { targetCaixinha = it })
                }
                TextButton(onClick = {
                    val parsedDates = parseDateRanges(transferDates)
                    if (parsedDates.isNotEmpty() && transferValue.toDoubleOrZero() > 0) {
                        onAddTransfer(
                            TransferEvent(
                                name = transferName.ifBlank { "Transferência" },
                                amount = transferValue.toDoubleOrZero(),
                                dates = parsedDates,
                                source = sourceTransfer,
                                target = targetTransfer,
                                sourceId = sourceCaixinha,
                                targetId = targetCaixinha
                            )
                        )
                        transferName = ""
                        transferValue = ""
                        transferDates = ""
                        sourceCaixinha = null
                        targetCaixinha = null
                    }
                }) { Text("Adicionar transferência") }
            }
        }

        FutureEventsCard(
            salary = salary,
            creditCard = creditCard,
            vales = vales,
            startDate = startDate,
            endDate = endDate
        )

        SimulatedEventsCard(simulatedTransactions, onRemoveSimulation)

        TransferListCard(transfers, onRemoveTransfer)

        BalanceTable(dailyBalances)
    }
}

@Composable
fun FutureEventsCard(
    salary: SalaryConfig,
    creditCard: CreditCardConfig,
    vales: List<Vale>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val events = mutableListOf<String>()
    generateSequence(startDate) { current ->
        val next = current.plusDays(1)
        if (next > endDate) null else next
    }.forEach { date ->
        val salaryDate = adjustedPayday(date.year, date.monthValue, salary.payday)
        if (date == salaryDate) events.add("Salário ${salary.amount.formatCurrency()} em ${date}")
        if (date.dayOfMonth == creditCard.closingDay) events.add("Fatura do cartão em ${date}")
        vales.forEach { vale ->
            val defaultDay = penultimateBusinessDay(date.year, date.monthValue).dayOfMonth
            val creditDay = vale.creditDay.takeIf { it in 1..28 } ?: defaultDay
            if (date.dayOfMonth == creditDay) {
                events.add("${vale.name} +${vale.creditValue.formatCurrency()} em ${date}")
            }
        }
    }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Transações futuras padrão", fontWeight = FontWeight.Bold)
            if (events.isEmpty()) {
                Text("Sem lançamentos no período selecionado", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            } else {
                events.sorted().forEach { Text(it) }
            }
        }
    }
}

@Composable
fun SimulatedEventsCard(simulatedTransactions: List<SimulatedTransaction>, onRemove: (SimulatedTransaction) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Transações simuladas", fontWeight = FontWeight.Bold)
                TextButton(onClick = { simulatedTransactions.toList().forEach { onRemove(it) } }) { Text("Excluir todas") }
            }
            if (simulatedTransactions.isEmpty()) {
                Text("Nenhuma simulação cadastrada", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            } else {
                simulatedTransactions.sortedBy { it.dates.minOrNull() }.forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tx.name, fontWeight = FontWeight.SemiBold)
                            Text("${tx.type.name.lowercase().replaceFirstChar { it.uppercase() }} ${tx.amount.formatCurrency()} | ${tx.dates.size} data(s)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onRemove(tx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransferListCard(transfers: List<TransferEvent>, onRemove: (TransferEvent) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transferências", fontWeight = FontWeight.Bold)
            if (transfers.isEmpty()) {
                Text("Nenhuma transferência cadastrada", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            } else {
                transfers.sortedBy { it.dates.minOrNull() }.forEach { transfer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(transfer.name, fontWeight = FontWeight.SemiBold)
                            Text("${transfer.amount.formatCurrency()} | ${transfer.dates.size} data(s)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onRemove(transfer) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceTable(dailyBalances: List<DailyBalance>) {
    if (dailyBalances.isEmpty()) return
    val columns = dailyBalances.first().balances.keys.toList()
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Data", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold)
            columns.forEach { column ->
                Text(column, modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold)
            }
        }
        dailyBalances.forEach { day ->
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(day.date.toString(), modifier = Modifier.width(90.dp))
                columns.forEach { column ->
                    val value = day.balances[column] ?: 0.0
                    val color = if (value >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
                    Text(value.formatCurrency(), color = color, modifier = Modifier.width(140.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    caixinhas: List<Caixinha>,
    vales: List<Vale>,
    dailyBalances: List<DailyBalance>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val availableAccounts = remember { mutableStateListOf<String>() }
    LaunchedEffect(dailyBalances) {
        availableAccounts.clear()
        if (dailyBalances.isNotEmpty()) {
            availableAccounts.addAll(dailyBalances.first().balances.keys)
        }
    }
    val selectedAccounts = remember { mutableStateListOf<String>() }
    LaunchedEffect(availableAccounts) {
        selectedAccounts.clear()
        selectedAccounts.addAll(availableAccounts)
    }

    val totalSeries = dailyBalances.map { it.date to ((it.balances["Conta Corrente"] ?: 0.0) + caixinhasSum(it)) }
    val checkingSeries = dailyBalances.map { it.date to (it.balances["Conta Corrente"] ?: 0.0) }
    val caixinhasSeries = dailyBalances.map { it.date to caixinhasSum(it) }

    val finalTotal = totalSeries.lastOrNull()?.second ?: 0.0
    val startTotal = totalSeries.firstOrNull()?.second ?: 0.0
    val filterSet = selectedAccounts.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Filtros", fontWeight = FontWeight.SemiBold)
                Text("Selecionar contas")
                FlowChips(options = availableAccounts, selected = selectedAccounts) { account ->
                    if (selectedAccounts.contains(account)) selectedAccounts.remove(account) else selectedAccounts.add(account)
                }
                Text("Período: ${startDate} a ${endDate}")
            }
        }

        VariationCard(
            title = "Visão 1 - Total",
            entries = listOfNotNull(
                "Total" to (finalTotal to (finalTotal - startTotal)),
                "Conta Corrente" to variationFor("Conta Corrente", dailyBalances).takeIf { filterSet.isEmpty() || filterSet.contains("Conta Corrente") },
                run {
                    val last = caixinhasSeries.lastOrNull()?.second ?: 0.0
                    val first = caixinhasSeries.firstOrNull()?.second ?: 0.0
                    "Caixinhas Total" to (last to (last - first))
                }.takeIf { filterSet.isEmpty() || filterSet.any { it.startsWith("Caixinha") } }
            )
        )

        VariationCard(
            title = "Visão 2 - Caixinhas",
            entries = buildList {
                val last = caixinhasSeries.lastOrNull()?.second ?: 0.0
                val first = caixinhasSeries.firstOrNull()?.second ?: 0.0
                if (filterSet.isEmpty() || filterSet.any { it.startsWith("Caixinha") }) {
                    add("Caixinhas Total" to (last to (last - first)))
                }
                caixinhas.forEach { caixinha ->
                    val label = caixinhaLabel(caixinha)
                    if (filterSet.isEmpty() || filterSet.contains(label)) {
                        add(label to variationFor(label, dailyBalances))
                    }
                }
            }
        )

        FilledChart(
            title = "Visão 3 - Gráfico de faixas (Total / Conta Corrente / Caixinhas)",
            series = listOf(
                "Total" to totalSeries,
                "Conta Corrente" to checkingSeries,
                "Caixinhas" to caixinhasSeries
            ).filter { filterSet.isEmpty() || it.first == "Total" || filterSet.contains(it.first) || (it.first == "Caixinhas" && filterSet.any { acc -> acc.startsWith("Caixinha") }) }
        )

        val variationVsFirstTotal = totalSeries.map { it.first to (it.second - startTotal) }
        val variationVsFirstChecking = checkingSeries.map { it.first to (it.second - checkingSeries.firstOrNull()?.second ?: 0.0) }
        val variationVsFirstCaixinhas = caixinhasSeries.map { it.first to (it.second - caixinhasSeries.firstOrNull()?.second ?: 0.0) }

        val variationVsPreviousTotal = totalSeries.zipWithNext { prev, next -> next.first to (next.second - prev.second) }
        val variationVsPreviousChecking = checkingSeries.zipWithNext { prev, next -> next.first to (next.second - prev.second) }
        val variationVsPreviousCaixinhas = caixinhasSeries.zipWithNext { prev, next -> next.first to (next.second - prev.second) }

        LineChart(
            title = "Visão 4 - Variação vs primeiro dia",
            series = listOf(
                "Total" to variationVsFirstTotal,
                "Conta Corrente" to variationVsFirstChecking,
                "Caixinhas" to variationVsFirstCaixinhas
            ).filter { filterSet.isEmpty() || it.first == "Total" || filterSet.contains(it.first) || (it.first == "Caixinhas" && filterSet.any { acc -> acc.startsWith("Caixinha") }) }
        )

        LineChart(
            title = "Visão 4 - Variação vs dia anterior",
            series = listOf(
                "Total" to variationVsPreviousTotal,
                "Conta Corrente" to variationVsPreviousChecking,
                "Caixinhas" to variationVsPreviousCaixinhas
            ).filter { filterSet.isEmpty() || it.first == "Total" || filterSet.contains(it.first) || (it.first == "Caixinhas" && filterSet.any { acc -> acc.startsWith("Caixinha") }) }
        )
    }
}

@Composable
private fun FlowChips(options: List<String>, selected: List<String>, onClick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            FilterChip(
                selected = selected.contains(option),
                onClick = { onClick(option) },
                label = { Text(option) }
            )
        }
    }
}

@Composable
fun VariationCard(title: String, entries: List<Pair<String, Pair<Double, Double>>>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            entries.forEach { (label, values) ->
                val (finalValue, diff) = values
                val color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text("Final: ${finalValue.formatCurrency()}")
                    }
                    Text(
                        (if (diff >= 0) "▲" else "▼") + diff.formatCurrency(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun FilledChart(title: String, series: List<Pair<String, List<Pair<LocalDate, Double>>>>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)) {
                    val maxX = (series.firstOrNull()?.second?.size?.coerceAtLeast(1)?.minus(1))?.toFloat() ?: 1f
                    val values = series.flatMap { it.second.map { point -> point.second } }
                    val minY = values.minOrNull() ?: 0.0
                    val maxY = values.maxOrNull() ?: 1.0
                    val heightRange = (maxY - minY).takeIf { it != 0.0 } ?: 1.0

                    series.forEachIndexed { index, (_, points) ->
                        if (points.isEmpty()) return@forEachIndexed
                        val path = Path()
                        points.forEachIndexed { i, point ->
                            val x = size.width * (i / maxX.coerceAtLeast(1f))
                            val y = size.height - ((point.second - minY) / heightRange * size.height).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.lineTo(size.width, size.height)
                        path.lineTo(0f, size.height)
                        path.close()
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f / (index + 1)),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEach { (label, _) ->
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
fun LineChart(title: String, series: List<Pair<String, List<Pair<LocalDate, Double>>>>) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)) {
                    val maxX = (series.firstOrNull()?.second?.size?.minus(1))?.toFloat() ?: 1f
                    val allValues = series.flatMap { it.second.map { pair -> pair.second } }
                    val minY = allValues.minOrNull() ?: 0.0
                    val maxY = allValues.maxOrNull() ?: 1.0
                    val range = (maxY - minY).takeIf { it != 0.0 } ?: 1.0
                    val colors = listOf(Color(0xFF80CBC4), Color(0xFFFFAB91), Color(0xFFA5D6A7))
                    series.forEachIndexed { index, (_, values) ->
                        if (values.isEmpty()) return@forEachIndexed
                        val color = colors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                        val path = Path()
                        values.forEachIndexed { i, point ->
                            val x = size.width * (i / maxX.coerceAtLeast(1f))
                            val y = size.height - ((point.second - minY) / range * size.height).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = color, style = Stroke(width = 4f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEach { (label, _) ->
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
fun CurrencyField(label: String, value: Double, onChange: (Double) -> Unit) {
    OutlinedTextField(
        value = value.takeIf { !it.isNaN() }?.toString() ?: "",
        onValueChange = { onChange(it.toDoubleOrZero()) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun IntegerField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toIntOrZero()) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DropdownSelector(label: String, options: Map<String, String>, selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.let { options[it] } ?: label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
            DropdownMenuItem(text = { Text("Limpar") }, onClick = {
                onSelect(null)
                expanded = false
            })
        }
    }
}

private fun String.toDoubleOrZero(): Double = toDoubleOrNull() ?: 0.0
private fun String.toIntOrZero(): Int = toIntOrNull() ?: 0

private fun variationFor(label: String, dailyBalances: List<DailyBalance>): Pair<Double, Double> {
    val first = dailyBalances.firstOrNull()?.balances?.get(label) ?: 0.0
    val last = dailyBalances.lastOrNull()?.balances?.get(label) ?: 0.0
    return last to (last - first)
}

private fun caixinhasSum(day: DailyBalance): Double =
    day.balances.filter { it.key.startsWith("Caixinha") }.values.sum()
