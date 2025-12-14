package com.example.teste2.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

enum class AccountDestination { CHECKING, CAIXINHA, VALE }
enum class TransactionType { DEBIT, CREDIT }

data class CaixinhaInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double
)

data class VoucherInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double,
    val creditDay: Int? = null
)

data class SalaryConfig(
    val amount: Double,
    val payDay: Int
)

data class CardConfig(
    val debt: Double,
    val closingDay: Int
)

data class SimulationTransaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val dateRanges: List<ClosedRange<LocalDate>>,
    val type: TransactionType,
    val sourceAccount: AccountDestination,
    val sourceName: String? = null
)

data class TransferSimulation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val dateRanges: List<ClosedRange<LocalDate>>,
    val from: AccountDestination,
    val to: AccountDestination,
    val fromName: String? = null,
    val toName: String? = null
)

data class FutureEvent(
    val date: LocalDate,
    val description: String,
    val amount: Double,
    val destination: String
)

data class DailyBalance(
    val date: LocalDate,
    val checking: Double,
    val caixinhas: Map<String, Double>,
    val vales: Map<String, Double>,
    val cardDebt: Double
) {
    val totalCaixinhas: Double = caixinhas.values.sum()
    val totalVales: Double = vales.values.sum()
    val netWorth: Double = checking + totalCaixinhas + totalVales - cardDebt
    val liquidFunds: Double = checking + totalCaixinhas
}

private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseDateRanges(raw: String): Pair<List<ClosedRange<LocalDate>>, String?> {
    if (raw.isBlank()) return emptyList<ClosedRange<LocalDate>>() to null
    val ranges = mutableListOf<ClosedRange<LocalDate>>()
    val parts = raw.split(";", ",").map { it.trim() }.filter { it.isNotEmpty() }
    for (part in parts) {
        try {
            if (part.contains(":")) {
                val (startRaw, endRaw) = part.split(":", limit = 2)
                val start = LocalDate.parse(startRaw.trim(), formatter)
                val end = LocalDate.parse(endRaw.trim(), formatter)
                ranges += start..end
            } else {
                val single = LocalDate.parse(part, formatter)
                ranges += single..single
            }
        } catch (ex: DateTimeParseException) {
            return emptyList<ClosedRange<LocalDate>>() to "Data inválida: ${part}. Use o formato AAAA-MM-DD."
        }
    }
    return ranges to null
}

fun expandRanges(ranges: List<ClosedRange<LocalDate>>): List<LocalDate> =
    ranges.flatMap { range ->
        generateSequence(range.start) { previous ->
            val next = previous.plusDays(1)
            if (next <= range.endInclusive) next else null
        }.toList()
    }

fun penultimateBusinessDay(yearMonth: YearMonth): LocalDate {
    var date = yearMonth.atEndOfMonth()
    var businessDaysFound = 0
    while (true) {
        if (date.dayOfWeek !in weekend()) {
            businessDaysFound++
            if (businessDaysFound == 2) return date
        }
        date = date.minusDays(1)
    }
}

fun adjustForWeekend(date: LocalDate): LocalDate = when (date.dayOfWeek) {
    DayOfWeek.SATURDAY -> date.minusDays(1)
    DayOfWeek.SUNDAY -> date.minusDays(2)
    else -> date
}

private fun weekend(): Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

fun nextOccurrence(day: Int, from: LocalDate): LocalDate {
    val currentMonth = YearMonth.from(from)
    val candidate = currentMonth.atDay(day.coerceIn(1, currentMonth.lengthOfMonth()))
    return if (candidate >= from) candidate else {
        val nextMonth = currentMonth.plusMonths(1)
        nextMonth.atDay(day.coerceIn(1, nextMonth.lengthOfMonth()))
    }
}

fun voucherCreditDate(voucher: VoucherInput, from: LocalDate): LocalDate {
    val ym = YearMonth.from(from)
    val baseDay = voucher.creditDay ?: penultimateBusinessDay(ym).dayOfMonth
    val target = nextOccurrence(baseDay, from)
    return adjustForWeekend(target)
}

fun buildStandardEvents(
    start: LocalDate,
    end: LocalDate,
    salaryConfig: SalaryConfig,
    cardConfig: CardConfig,
    vouchers: List<VoucherInput>
): List<FutureEvent> {
    val events = mutableListOf<FutureEvent>()
    val salaryDate = adjustForWeekend(nextOccurrence(salaryConfig.payDay, start))
    if (salaryDate in start..end) {
        events += FutureEvent(salaryDate, "Salário", salaryConfig.amount, "Conta Corrente")
    }

    val cardDate = adjustForWeekend(nextOccurrence(cardConfig.closingDay, start))
    if (cardDate in start..end) {
        events += FutureEvent(cardDate, "Fechamento cartão", -cardConfig.debt, "Conta Corrente")
    }

    vouchers.forEach { voucher ->
        val creditDate = voucherCreditDate(voucher, start)
        if (creditDate in start..end) {
            events += FutureEvent(creditDate, voucher.name, voucher.balance, "Vale")
        }
    }
    return events.sortedBy { it.date }
}

fun simulateBalances(
    start: LocalDate,
    end: LocalDate,
    checkingBalance: Double,
    caixinhas: List<CaixinhaInput>,
    vouchers: List<VoucherInput>,
    salaryConfig: SalaryConfig,
    cardConfig: CardConfig,
    simulatedTransactions: List<SimulationTransaction>,
    transferSimulations: List<TransferSimulation>
): List<DailyBalance> {
    val days = generateSequence(start) { if (it < end) it.plusDays(1) else null }.toList()
    val caixinhasState = caixinhas.associate { it.name to it.balance }.toMutableMap()
    val voucherState = vouchers.associate { it.name to it.balance }.toMutableMap()
    var checking = checkingBalance
    var cardDebt = cardConfig.debt

    val standardEvents = buildStandardEvents(start, end, salaryConfig, cardConfig, vouchers)
    val simulatedEventsByDate = simulatedTransactions.flatMap { tx ->
        expandRanges(tx.dateRanges).map { date -> date to tx }
    }.groupBy({ it.first }, { it.second })

    val transfersByDate = transferSimulations.flatMap { transfer ->
        expandRanges(transfer.dateRanges).map { date -> date to transfer }
    }.groupBy({ it.first }, { it.second })

    val eventsByDate = standardEvents.groupBy { it.date }

    val snapshots = mutableListOf<DailyBalance>()

    for (date in days) {
        eventsByDate[date]?.forEach { event ->
            when (event.destination) {
                "Conta Corrente" -> checking += event.amount
                "Vale" -> {
                    val key = voucherState.keys.firstOrNull() ?: "Vale"
                    voucherState[key] = (voucherState[key] ?: 0.0) + event.amount
                }
                else -> checking += event.amount
            }
        }

        simulatedEventsByDate[date]?.forEach { tx ->
            when (tx.sourceAccount) {
                AccountDestination.CHECKING -> checking += if (tx.type == TransactionType.CREDIT) tx.amount else -tx.amount
                AccountDestination.CAIXINHA -> {
                    val key = tx.sourceName ?: caixinhasState.keys.firstOrNull() ?: "Caixinha"
                    val current = caixinhasState.getOrDefault(key, 0.0)
                    caixinhasState[key] = current + if (tx.type == TransactionType.CREDIT) tx.amount else -tx.amount
                }
                AccountDestination.VALE -> {
                    val key = tx.sourceName ?: voucherState.keys.firstOrNull() ?: "Vale"
                    val current = voucherState.getOrDefault(key, 0.0)
                    voucherState[key] = current + if (tx.type == TransactionType.CREDIT) tx.amount else -tx.amount
                }
            }
        }

        transfersByDate[date]?.forEach { transfer ->
            fun withdraw(account: AccountDestination, label: String?, amount: Double) {
                when (account) {
                    AccountDestination.CHECKING -> checking -= amount
                    AccountDestination.CAIXINHA -> {
                        val key = label ?: caixinhasState.keys.firstOrNull() ?: "Caixinha"
                        caixinhasState[key] = caixinhasState.getOrDefault(key, 0.0) - amount
                    }
                    AccountDestination.VALE -> {
                        val key = label ?: voucherState.keys.firstOrNull() ?: "Vale"
                        voucherState[key] = voucherState.getOrDefault(key, 0.0) - amount
                    }
                }
            }

            fun deposit(account: AccountDestination, label: String?, amount: Double) {
                when (account) {
                    AccountDestination.CHECKING -> checking += amount
                    AccountDestination.CAIXINHA -> {
                        val key = label ?: caixinhasState.keys.firstOrNull() ?: "Caixinha"
                        caixinhasState[key] = caixinhasState.getOrDefault(key, 0.0) + amount
                    }
                    AccountDestination.VALE -> {
                        val key = label ?: voucherState.keys.firstOrNull() ?: "Vale"
                        voucherState[key] = voucherState.getOrDefault(key, 0.0) + amount
                    }
                }
            }

            withdraw(transfer.from, transfer.fromName, transfer.amount)
            deposit(transfer.to, transfer.toName, transfer.amount)
        }

        if (cardDebt != 0.0 && eventsByDate[date]?.any { it.description == "Fechamento cartão" } == true) {
            cardDebt = 0.0
        }

        snapshots += DailyBalance(
            date = date,
            checking = checking,
            caixinhas = caixinhasState.toMap(),
            vales = voucherState.toMap(),
            cardDebt = cardDebt
        )
    }

    return snapshots
}
