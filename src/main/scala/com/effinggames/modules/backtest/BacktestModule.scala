package com.effinggames.modules.backtest

import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import java.time.temporal.ChronoField
import java.time.{LocalDate, ZoneId}

import com.effinggames.algorithms._
import com.effinggames.modules.Module
import com.effinggames.util.FutureHelper
import com.effinggames.util.LoggerHelper.logger

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BacktestModule extends Module {
  val name = "Backtest"
  val triggerWord = "backtest"
  val helpText = "backtest <run -algo BuyOnGap -from 2000 -to 3/14/2006>"

  private val dateFormatter = new DateTimeFormatterBuilder()
    .appendPattern("[yyyy]")
    .appendPattern("[M/d/yyyy]")
    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
    .toFormatter()

  def run(command: String, flags: Seq[String]): Future[Unit] = async {
    command match {
      case "run" =>
        val startDate = getFlag[String](flags, "-from") match {
          case Some(dateStr) => LocalDate.parse(dateStr, dateFormatter)
          case _ => LocalDate.parse("2000-01-01")
        }

        val endDate = getFlag[String](flags, "-to") match {
          case Some(dateStr) => LocalDate.parse(dateStr, dateFormatter)
          case _ => LocalDate.now()
        }

        logger.info(s"Back test starting from $startDate to $endDate")
        await(StockLoader.loadSymbol("IBM"))
        val ibmStock = StockLoader.getStock("IBM")
        val ibmDataSeq: Seq[LocalDate] = ibmStock._eodData.map(_.date)

        val startIndex = findClosestDateIndex(ibmDataSeq, startDate)
        val endIndex = findClosestDateIndex(ibmDataSeq, endDate)

        //Sets the starting index in the eodData array.
        Stock.setCurrentDateIndex(startIndex)
        //Uses IBM as the baseline stock in terms of data length.
        //If a stock has less data length, then assumes the stock got listed later.
        Stock.setTargetDataLength(ibmDataSeq.size)
        logger.info(s"Loading data for ${Stock.targetDataLength - Stock.currentDateIndex} days")

        val algos = List(BuyOnGap, Benchmark)
        val initializeFuture = FutureHelper.traverseSequential(algos)(_.initialize())
        await(initializeFuture)

        logger.info("Successfully preloaded stocks")
        val startingCash = 100000
        var portfolioMap: Map[Algorithm, Portfolio] = algos.map(_ -> new Portfolio(startingCash)).toMap

        while (Stock.currentDateIndex < endIndex) {
          algos.filter(Stock.currentDateIndex >= _.minimumDataLength).foreach { algo =>
            val ctx = new TickHandlerContext(portfolioMap(algo))
            algo.tickHandler(ctx)
            portfolioMap = portfolioMap + (algo -> ctx.currentPortfolio)
          }

          Stock.incrementTicker()
        }

        portfolioMap.foreach({ case (algo: Algorithm, portfolio: Portfolio) =>
          logger.info(s"${algo.getClass.getName} final value: ${portfolio.totalValue}")
        })

        logger.info("Back test successful")
    }
  }

  /**
    * Finds the index of the closest date, that is before or equal to targetDate, for the given date seq.
    * Returns 0 if no dates are before the targetDate.
    * @param dates Seq of dates to search.
    * @param targetDate The date to be compared against.
    * @return Returns the index of the closest date.
    */
  private def findClosestDateIndex(dates: Seq[LocalDate], targetDate: LocalDate): Int = {
    val zoneId = ZoneId.systemDefault()
    //Gets index of closest date that is before or equal to targetDate
    dates.filter(date => date.isBefore(targetDate) || date.isEqual(targetDate)) match {
      case filteredDates if filteredDates.isEmpty => 0
      case filteredDates =>
        val closestDate = filteredDates.minBy( date => {
          val diff = targetDate.atStartOfDay(zoneId).toEpochSecond - date.atStartOfDay(zoneId).toEpochSecond
          diff
        })
        dates.indexOf(closestDate)
    }
  }
}