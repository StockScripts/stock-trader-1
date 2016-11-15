package com.effinggames.modules

import java.time.LocalDate

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
  case class UserList(
    name: String,
    symbols: Seq[String]
  )
}
