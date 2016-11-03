package com.effinggames.modules.backtest

import scala.concurrent.Future

abstract class Algorithm {
  //Overridable backtester hints.
  //How many days of data the algo needs.
  val minimumDataLength = 0

  //Used for loading all data that the algo will need.
  def initialize(ctx: InitContext = new InitContext()): Future[Unit]

  //Iterates over all eod ticks and holds the main algo logic.
  def tickHandler(ctx: TickHandlerContext): Unit
}

class InitContext {
  def loadSymbol(symbol: String): Future[Unit] = StockLoader.loadSymbol(symbol)
  def loadUserList(userListName: String): Future[Unit] = StockLoader.loadUserList(userListName)
}

class TickHandlerContext(_portfolio: Portfolio) {
  //The portfolio after all the orders go in.
  private[backtest] var currentPortfolio = _portfolio

  //Gets the stocks based on the user list name / symbol. Throws an error if not found.
  def getStocksForUserList(userListName: String): Seq[Stock] = StockLoader.getStocksForUserList(userListName)
  def getStock(symbol: String): Stock = StockLoader.getStock(symbol)

  //Gets the starting portfolio for this tick.
  def portfolio: Portfolio = _portfolio

  //Orders X shares of stock at the current price.
  def order(stock: Stock, quantity: Int): Unit = {
    if (quantity != 0) {
      val position = Position(stock, quantity, stock.getPrice)
      currentPortfolio = currentPortfolio.addPosition(position)
    }
  }

  def isFirstTickOfDay: Boolean = Stock.isFirstTickOfDay
  def isLastTickOfDay: Boolean = Stock.isLastTickOfDay
}