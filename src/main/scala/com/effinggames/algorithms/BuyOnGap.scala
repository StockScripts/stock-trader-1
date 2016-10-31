package com.effinggames.algorithms

import com.effinggames.modules.backtest.{TickHandlerContext, InitContext, Algorithm}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BuyOnGap extends Algorithm {
  def initialize(ctx: InitContext): Future[Unit] = async {
    ctx.loadUserList("SP500_2010")
    ctx.loadSymbol("AAPL")
  }

  def tickHandler(ctx: TickHandlerContext): Unit = {
//    StockDatabase.getStockArray("sp500_2010")
    ctx.getStocksForUserList("SP500_2010")
    ctx.getStock("AAPL")
//    ctx.order(stock, 20)
    ctx.portfolio
    ctx.isFirstTickOfDay
    ctx.isLastTickOfDay
  }
}
