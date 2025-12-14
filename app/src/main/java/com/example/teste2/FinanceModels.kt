package com.example.teste2

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class BalanceSource { CHECKING, CAIXINHA, VALE }
enum class TransactionType { DEBIT, CREDIT }

data class Caixinha(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double
)

data class Vale(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double,
    val creditDay: Int,
    val creditValue: Double
)

data class SalaryConfig(
    val amount: Double,
    val payday: Int
)

data class CreditCardConfig(
    val debt: Double,
    val closingDay: Int
)

data class SimulatedTransaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val dates: List<LocalDate>,
    val type: TransactionType,
    val source: BalanceSource,
    val caixinhaId: String? = null,
    val valeId: String? = null
)

data class TransferEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val dates: List<LocalDate>,
    val source: BalanceSource,
    val target: BalanceSource,
    val sourceId: String? = null,
    val targetId: String? = null
)

data class DailyBalance(
    val date: LocalDate,
    val balances: Map<String, Double>
)

private const val CHECKING_KEY = "Conta Corrente"
private const val CREDIT_CARD_KEY = "Cartão Crédito"

fun parseDateRanges(input: String): List<LocalDate> {
    if (input.isBlank()) return emptyList()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    return input.split(",", ";", " ")
        .mapNotNull { it.trim().takeIf { token -> token.isNotEmpty() } }
        .flatMap { token ->
            if (".." in token) {
                val (start, end) = token.split("..", limit = 2)
                runCatching {
                    val startDate = LocalDate.parse(start.trim(), formatter)
                    val endDate = LocalDate.parse(end.trim(), formatter)
                    generateSequence(startDate) { current ->
                        val next = current.plusDays(1)
                        if (next > endDate) null else next
                    }.toList()
                }.getOrElse { emptyList() }
            } else {
                runCatching { LocalDate.parse(token, formatter) }.getOrNull()?.let { listOf(it) }
                    ?: emptyList()
            }
        }
}

fun penultimateBusinessDay(year: Int, month: Int): LocalDate {
    val lastDay = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth())
    var candidate = lastDay
    while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
        candidate = candidate.minusDays(1)
    }
    // Penultimate business day
    var penultimate = candidate.minusDays(1)
    while (penultimate.dayOfWeek == DayOfWeek.SATURDAY || penultimate.dayOfWeek == DayOfWeek.SUNDAY) {
        penultimate = penultimate.minusDays(1)
    }
    return penultimate
}

fun adjustedPayday(year: Int, month: Int, desiredDay: Int): LocalDate {
    val baseDate = LocalDate.of(year, month, desiredDay.coerceIn(1, 28))
    return when (baseDate.dayOfWeek) {
        DayOfWeek.SATURDAY -> baseDate.minusDays(1)
        DayOfWeek.SUNDAY -> baseDate.minusDays(2)
        else -> baseDate
    }
}

fun generateDailyBalances(
    checkingBalance: Double,
    caixinhas: List<Caixinha>,
    vales: List<Vale>,
    salary: SalaryConfig,
    creditCard: CreditCardConfig,
    simulatedTransactions: List<SimulatedTransaction>,
    transfers: List<TransferEvent>,
    start: LocalDate,
    end: LocalDate
): List<DailyBalance> {
    val caixinhaMap = caixinhas.associateBy { it.id }
    val valeMap = vales.associateBy { it.id }

    val balances = mutableMapOf<String, Double>().apply {
        put(CHECKING_KEY, checkingBalance)
        put(CREDIT_CARD_KEY, -creditCard.debt)
        caixinhas.forEach { put(caixinhaLabel(it), it.balance) }
        vales.forEach { put(valeLabel(it), it.balance) }
    }

    var cardDebt = creditCard.debt

    return generateSequence(start) { current ->
        val next = current.plusDays(1)
        if (next > end) null else next
    }.fold(mutableListOf<DailyBalance>()) { acc, date ->
        val dayBalances = balances.toMutableMap()

        // Salary
        val salaryDate = adjustedPayday(date.year, date.monthValue, salary.payday)
        if (date == salaryDate) {
            dayBalances[CHECKING_KEY] = (dayBalances[CHECKING_KEY] ?: 0.0) + salary.amount
        }

        // Vales credited on configurable day, default penultimate business day
        vales.forEach { vale ->
            val defaultCreditDay = penultimateBusinessDay(date.year, date.monthValue).dayOfMonth
            val creditDay = vale.creditDay.takeIf { it in 1..28 } ?: defaultCreditDay
            if (date.dayOfMonth == creditDay) {
                val key = valeLabel(vale)
                dayBalances[key] = (dayBalances[key] ?: 0.0) + vale.creditValue
            }
        }

        // Credit card payment
        if (date.dayOfMonth == creditCard.closingDay && cardDebt != 0.0) {
            dayBalances[CHECKING_KEY] = (dayBalances[CHECKING_KEY] ?: 0.0) - cardDebt
            cardDebt = 0.0
            dayBalances[CREDIT_CARD_KEY] = 0.0
        }

        simulatedTransactions.forEach { tx ->
            if (date in tx.dates) {
                val amount = if (tx.type == TransactionType.DEBIT) -tx.amount else tx.amount
                applyMovement(dayBalances, amount, tx.source, tx.caixinhaId, tx.valeId, caixinhaMap, valeMap)
            }
        }

        transfers.forEach { transfer ->
            if (date in transfer.dates) {
                applyMovement(dayBalances, -transfer.amount, transfer.source, transfer.sourceId, null, caixinhaMap, valeMap)
                applyMovement(dayBalances, transfer.amount, transfer.target, transfer.targetId, null, caixinhaMap, valeMap)
            }
        }

        acc.add(DailyBalance(date, dayBalances.toMap()))
        balances.clear()
        balances.putAll(dayBalances)
        acc
    }
}

private fun applyMovement(
    balances: MutableMap<String, Double>,
    amount: Double,
    source: BalanceSource,
    caixinhaId: String?,
    valeId: String?,
    caixinhaMap: Map<String, Caixinha>,
    valeMap: Map<String, Vale>
) {
    val key = when (source) {
        BalanceSource.CHECKING -> CHECKING_KEY
        BalanceSource.CAIXINHA -> caixinhaId?.let { caixinhaMap[it]?.let { caixinhaLabel(it) } }
            ?: "Caixinha"
        BalanceSource.VALE -> valeId?.let { valeMap[it]?.let { valeLabel(it) } } ?: "Vale"
    }
    balances[key] = (balances[key] ?: 0.0) + amount
}

fun caixinhaLabel(caixinha: Caixinha) = "Caixinha - ${caixinha.name}"
fun valeLabel(vale: Vale) = "Vale - ${vale.name}"

fun Double.formatCurrency(): String = "R$ " + String.format("%,.2f", this).replace(",", "_").replace('.', ',').replace('_', '.')
