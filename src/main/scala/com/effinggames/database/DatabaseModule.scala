package com.effinggames.database

import com.effinggames.Module

object DatabaseModule extends Module {
  val name = "Database"
  val triggerWord = "database"
  val helpText = "database <download>"

  def run(params: List[String]): Unit = {
    params.head.toLowerCase match {
      case "download" =>
        fetchStocks()
        buildStockLists()
    }
  }

  private def fetchStocks(): Unit = {
//    StockFetcher.downloadStocks("databases/StockLists/NYSE.txt", "databases/nyse_eod.db")
//    StockFetcher.downloadStocks("databases/StockLists/NASDAQ.txt", "databases/nasdaq_eod.db")
//    StockFetcher.downloadStocks("databases/StockLists/AMEX.txt", "databases/amex_eod.db")
  }

  private def buildStockLists(): Unit = {
//    StockFetcher.parseListsIntoDatabase("UserLists/", "databases/user_lists.db")
  }
}
