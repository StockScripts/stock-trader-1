package com.effinggames.modules.backtest


import java.util.concurrent.Executors

import com.effinggames.modules.SharedModels

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Await, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import SharedModels.{UserList, EodData}
import com.effinggames.util.LoggerHelper.logger
import com.effinggames.util.DatabaseHelper._
import com.effinggames.util.DatabaseHelper.stockDB._

/**
  * Singleton responsible for loading all eodData from the database and caching the stocks.
  */
object StockLoader {
  //Limits fetching to 20 symbols at a time.
  private val fixedPool = Executors.newFixedThreadPool(20)
  private val fixedEC = ExecutionContext.fromExecutorService(fixedPool)
  private var symbolStockMap = Map[String, Stock]()
  private var symbolFutureMap = Map[String, Future[Unit]]()
  private var userListSymbolMap = Map[String, Seq[String]]()
  private var userListFutureMap = Map[String, Future[Seq[String]]]()

  //Fetches eodData for a symbol, and then saves stock object in the cache.
  private def fetchSymbol(symbol: String): Future[Unit] = async {
    logger.info(s"Fetching data for symbol: $symbol")

    val fetchQuery = quote {
      query[EodData].filter(_.symbol == lift(symbol))
    }

    val results = blocking {
      stockDB.run(fetchQuery)
    }.toVector

    if (results.isEmpty) {
      logger.warn(s"Could not find data for symbol: $symbol")
    }

    val stock = new Stock(results)
    symbolStockMap += (symbol -> stock)
  }

  //Fetches symbols for a user list, and then saves symbol seq in the cache.
  private def fetchUserList(userListName: String): Future[Seq[String]] = async {
    logger.info(s"Fetching data for user list: $userListName")

    val fetchQuery = quote {
      query[UserList].filter(_.name == lift(userListName))
    }

    val results = blocking {
      stockDB.run(fetchQuery)
    }

    if (results.isEmpty) {
      logger.warn(s"Could not find data for user list: $userListName")
    }

    val symbols = results.headOption.map(_.symbols).getOrElse(Seq.empty)
    userListSymbolMap += (userListName -> symbols)
    symbols
  }

  //Kicks off the symbol fetching, and handles caching and symbol formatting.
  def loadSymbol(symbol: String): Future[Unit] = {
    val formattedSymbol = symbol.toUpperCase

    symbolFutureMap.get(formattedSymbol) match {
      case Some(future) => future
      case None =>
        val future = Future {
          Await.result(fetchSymbol(formattedSymbol), 2.minutes)
        }(fixedEC)
        symbolFutureMap += (formattedSymbol -> future)
        future
    }

  }

  //Kicks off the user list fetching, and all symbols under it.
  def loadUserList(userListName: String): Future[Unit] = async {
    val formattedListName = userListName.toUpperCase

    val userListFuture = userListFutureMap.get(formattedListName) match {
      case Some(future) => future
      case None =>
        val future = fetchUserList(formattedListName)
        userListFutureMap += (formattedListName -> future)
        future
    }

    val symbolList = await(userListFuture)
    await(Future.traverse(symbolList)(loadSymbol))
  }

  private def getStockOpt(symbol: String): Option[Stock] = {
    val formattedSymbol = symbol.toUpperCase
    symbolStockMap.get(formattedSymbol)
  }

  //Returns seq of all stocks in the user list, from cache.
  //If a stock is missing then it is removed from the list.
  def getStocksForUserList(userListName: String): Seq[Stock] = {
    val formattedListName = userListName.toUpperCase
    val userList = userListSymbolMap(formattedListName).map(getStockOpt)
    userList.flatten
  }

  //Returns the stock from the cache, throws error if not found.
  def getStock(symbol: String): Stock = {
    getStockOpt(symbol).get
  }
}
