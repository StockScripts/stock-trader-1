package com.effinggames.core

import java.util.Date

object Models {
  case class EodData(
    symbol: String,
    date: Date,
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
