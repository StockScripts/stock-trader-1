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
  case class AlgoResult(
    algoName: String,
    date: LocalDateTime,
    annReturns: Double,
    annVolatility: Double,
    maxDrawdown: Double,
    sharpe: Double,
    sortino: Double,
    calmar: Option[Double],
    historicalValues: Seq[Double],
    historicalDates: Seq[LocalDate],
    backtestId: String = "foobar"
  )
  case class UserList(
    name: String,
    symbols: Seq[String]
  )
}
