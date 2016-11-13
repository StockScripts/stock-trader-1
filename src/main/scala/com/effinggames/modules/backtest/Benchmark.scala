package com.effinggames.modules.backtest

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Built-in benchmark algo, buys the SP500 and holds it.
  */
object Benchmark extends Algorithm {
  override val name = "Benchmark"

  def initialize(ctx: InitContext): Future[Unit] = async {
    await(ctx.loadSymbol("SPY"))
  }

  def tickHandler(ctx: TickHandlerContext): Unit = {
    val portfolio = ctx.portfolio
    val stock = ctx.getStock("SPY")
    val numShares = Math.floor(portfolio.floatingCash / stock.getPrice).toInt
    if (numShares > 0) {
      ctx.order(stock, numShares)
    }
  }
}
