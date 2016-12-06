package com.effinggames.modules

import java.time.{LocalDateTime, LocalDate}

import scala.util.Random

object SharedModels {
  case class EodData(
    symbol: String,
    date: LocalDate,
    open: Double,
    high: Double,
    low: Double,
    close: Double,
    volume: Long,
    adjClose: Double
  )
  case class Backtest(
    date: LocalDateTime,
    displayId: String,
    id: Int = 0
  )
  case class AlgoResult(
    algoName: String,
    annReturns: Double,
    annVolatility: Double,
    maxDrawdown: Double,
    sharpe: Double,
    sortino: Double,
    calmar: Option[Double],
    historicalValues: Seq[Double],
    historicalDates: Seq[LocalDate],
    backtestId: Int = 0
  )
  case class UserList(
    name: String,
    symbols: Seq[String]
  )
}
