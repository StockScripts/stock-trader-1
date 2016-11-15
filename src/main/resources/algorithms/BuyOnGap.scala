import com.effinggames.modules.backtest.{Algorithm, InitContext, TickHandlerContext}
import com.effinggames.util.LoggerHelper.logger

import scala.concurrent.Future

new Algorithm {
  override val name = "BuyOnGap"
  //Needs at least 91 days of data to operate
  override val minimumDataLength = 91

  def initialize(ctx: InitContext): Future[Unit] = {
    ctx.loadUserList("SP500_2007")
  }

  def tickHandler(ctx: TickHandlerContext): Unit = {
    if (ctx.isFirstTickOfDay) {
      //Selects sp500 stocks that have gapped down more than the 90 day daily SD.
      val selectedStocks = ctx.getStocksForUserList("SP500_2007").filter( stock => {
        val overnightChange = stock.getOpen(0) - stock.getLow(1)
        overnightChange < -stock.getDailySD(90, 1)
      }).toVector

      //Gets the top 100 losers of that list.
      val biggestLosers = selectedStocks.sortBy( stock => {
        val overnightChange = stock.getOpen(0) - stock.getLow(1)
        val gapPercentage = overnightChange / stock.getClose(1)
        gapPercentage
      }).reverse.take(100)

//      logger.debug("BuyOnGap buys: " + biggestLosers.size)

      //Buys an equal dollar amount of each stock.
      val fundsPerOrder = ctx.portfolio.floatingCash / biggestLosers.size
      biggestLosers.foreach { stock =>
        val quantity = Math.floor(fundsPerOrder / stock.getPrice).toInt
        if (quantity > 0) {
          ctx.order(stock, quantity)
        }
      }
    }

    if (ctx.isLastTickOfDay) {
      //Liquidates portfolio at end of day.
      ctx.portfolio.positions.foreach { case (stock, position) =>
        ctx.order(stock, -position.quantity)
      }
    }
  }
}
