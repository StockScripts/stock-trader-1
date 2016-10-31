package com.effinggames.modules

import java.time.LocalDate

object SharedModels {
  case class EodData(
    symbol: String,
    date: LocalDate,
    open: Float,
    high: Float,
    low: Float,
    close: Float,
    volume: Long,
    adjClose: Float
  )
  case class UserList(
    name: String,
    symbols: Seq[String]
  )
}
