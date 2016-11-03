package com.effinggames.algorithms

import com.effinggames.modules.backtest.{Algorithm, InitContext, TickHandlerContext}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Benchmark algo, buys the SP500 and holds it forever.
  */
object Benchmark extends Algorithm {
  def initialize(ctx: InitContext): Future[Unit] = async {
      await(ctx.loadSymbol("VOO"))
  }

  def tickHandler(ctx: TickHandlerContext): Unit = {
    val portfolio = ctx.portfolio
    val stock = ctx.getStock("VOO")
    val numShares = Math.floor(portfolio.floatingCash / stock.getPrice).toInt
    if (numShares > 0) {
      ctx.order(stock, numShares)
    }
  }
}
