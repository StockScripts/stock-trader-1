package com.effinggames.modules.backtest

import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.{ChronoUnit, ChronoField}
import java.time.{LocalDateTime, LocalDate, ZoneId}

import com.effinggames.modules.Module
import com.effinggames.modules.sharedModels.{Backtest, AlgoResult}
import com.effinggames.util.{RandomHelper, MathHelper, FileHelper, FutureHelper}
import com.effinggames.util.LoggerHelper.logger
import com.effinggames.util.DatabaseHelper._
import com.effinggames.util.DatabaseHelper.stockDB._

import com.twitter.util.Eval
import com.typesafe.config.ConfigFactory

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

object BacktestModule extends Module {
  val name = "Backtest"
  val triggerWord = "backtest"
  val helpText = "backtest <run --algo Test1 --algo2 Test2 --algo3 Test3 --pipe true --from 2000 --to 3/14/2006>"

  private val evalInstance = new Eval
  private val dateFormatter = new DateTimeFormatterBuilder()
    .appendPattern("[yyyy]")
    .appendPattern("[M/d/yyyy]")
    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
    .toFormatter()

  def run(command: String, flags: Seq[String]): Future[Unit] = async {
    command match {
      case "run" =>
        //Gets the start / end date flags if applicable.
        val startDate = getFlag[String](flags, "--from") match {
          case Some(dateStr) => LocalDate.parse(dateStr, dateFormatter)
          case _ => LocalDate.parse("2000-01-01")
        }

        val endDate = getFlag[String](flags, "--to") match {
          case Some(dateStr) => LocalDate.parse(dateStr, dateFormatter)
          case _ => LocalDate.now()
        }

        val algoFileNames = Vector(
          getFlag[String](flags, "--algo"),
          getFlag[String](flags, "--algo2"),
          getFlag[String](flags, "--algo3")
        )

        logger.info(s"Back test starting from $startDate to $endDate")

        logger.info("Warming up script interpreter..")
        Future {
          //Booting up the scala interpreter takes 2-4 seconds.
          //This helps reduce the script parsing time.
          evalInstance.apply[Int]("1 + 1")
        }

        val pipedAlgoInput = getFlag[Boolean](flags, "--pipe") match {
          case Some(true) =>
            logger.info("Input Algorithm:")
            val input = FileHelper.getStdInChunk.mkString("\n")
            logger.info("Input received")
            Some(input)
          case _ => None
        }

        //Calculates the total length of dataset being tested
        await(StockLoader.loadSymbol("IBM"))
        val ibmStock = StockLoader.getStock("IBM")
        val ibmDateSeq = ibmStock._eodData.map(_.date)

        val startIndex = findClosestDateIndex(ibmDateSeq, startDate)
        val endIndex = findClosestDateIndex(ibmDateSeq, endDate)

        //Sets the starting index in the eodData array.
        Stock.setCurrentDateIndex(startIndex)
        //Uses IBM as the baseline stock in terms of data length.
        //If a stock has less data length, then assumes the stock got listed later.
        Stock.setTargetDataLength(ibmDateSeq.size)
        logger.info(s"Date range found for ${Stock.targetDataLength - Stock.currentDateIndex} days")

        logger.info(s"Loading algos..")

        val parsedAlgos = algoFileNames.flatten.map { algoFileName =>
          loadAlgoFromFile(s"/algorithms/$algoFileName.scala")
        }

        val pipedAlgo = pipedAlgoInput.map(loadAlgoFromString)

        val algos = parsedAlgos ++ pipedAlgo :+ Benchmark

        logger.info(s"${algos.length} algos loaded")

        val initializeFuture = FutureHelper.traverseSequential(algos)(_.initialize())
        await(initializeFuture)

        logger.info("Successfully preloaded stocks")
        val startingCash = 100000f
        var portfolioMap: Map[Algorithm, Portfolio] = algos.map(_ -> new Portfolio(startingCash)).toMap
        //Maps algorithms to a Seq[(date, totalPortfolioValue)], for tracking algo returns.
        var historicalValueMap: Map[Algorithm, Vector[(LocalDate, Double)]] = algos.map(_ -> Vector()).toMap

        while (Stock.currentDateIndex < endIndex) {
          //Get algos which have enough data.
          val activeAlgos = algos.filter(Stock.currentDateIndex >= _.minimumDataLength)

          //Iterate through all active algos.
          activeAlgos.foreach { algo =>
            val ctx = new TickHandlerContext(portfolioMap(algo))
            algo.tickHandler(ctx)
            portfolioMap += (algo -> ctx.currentPortfolio)
          }

          if (Stock.isFirstTickOfDay) {
            //Track portfolio results end of every day.
            activeAlgos.foreach { algo =>
              val currentDate = ibmStock.getDate
              val portfolioValue = portfolioMap(algo).totalValue
              val newHistoricalData = historicalValueMap(algo) :+ (currentDate, portfolioValue)
              historicalValueMap += (algo -> newHistoricalData)
            }

            //Log it to console every month.
            if (ibmStock.getDate.getDayOfMonth == 1) {
              logger.info(s"Portfolio Value ${ibmStock.getDate}:")

              activeAlgos.foreach { algo =>
                logger.info(f"${algo.name}: $$${historicalValueMap(algo).last._2}%1.2f")
              }
            }
          }

          //Finish and start next tick.
          Stock.incrementTicker()
        }

        //Log results for each algo.
        val algoResults = algos.map { algo =>
          println()
          logger.info(s"${algo.name} Final Stats ($startDate to ${ibmStock.getDate}):")
          val historicalDates = historicalValueMap(algo).map(_._1)
          val historicalValues = historicalValueMap(algo).map(_._2)
          //Calc annualized return
          val durationYears = ChronoUnit.DAYS.between(startDate, endDate) / 365
          val absoluteReturns = historicalValues.last / startingCash
          val annReturns = math.pow(absoluteReturns, 1d / durationYears.toDouble) - 1
          //Calc volatility (std deviation)
          val dailyPercentageReturns = historicalValues.sliding(2).map {
            case Vector(prev, current) =>
              (current - prev) / prev
          }.toVector
          val annVolatility = MathHelper.stdDeviation(dailyPercentageReturns) * Math.sqrt(252)
          //Calc max drawdown
          def calcDrawdown(values: Seq[Double]): Double = {
            var peakValue = 0.01d
            var maxDrawdown = 0d
            values.foreach { totalValue =>
              val returnsFromPeak = (totalValue - peakValue) / peakValue
              if (returnsFromPeak < maxDrawdown) {
                maxDrawdown = returnsFromPeak
              }
              if (totalValue > peakValue) {
                peakValue = totalValue
              }
            }
            -maxDrawdown
          }
          val maxDrawdown = calcDrawdown(historicalValues)
          //Calc sharpe ratio
          val riskFreeReturn = 0.01
          val sharpeRatio = (annReturns - riskFreeReturn) / annVolatility
          //Calc sortino ratio
          val targetReturnRate = riskFreeReturn
          val negativeDailyReturns = dailyPercentageReturns.filter(_ <= 0)
          val negativeAnnVolatilty = MathHelper.stdDeviation(negativeDailyReturns) * Math.sqrt(252)
          val sortinoRatio = (annReturns - riskFreeReturn) / negativeAnnVolatilty
          //Calc calmar ratio (3 years)
          val calmarRatio = if (historicalValues.length >= 252 * 3) {
            val threeYearReturns = historicalValues.takeRight(252 * 3)
            val absoluteReturns = threeYearReturns.last / threeYearReturns.head
            val annReturns = math.pow(absoluteReturns, 1d / 3d) - 1
            val maxDrawdown = calcDrawdown(threeYearReturns)
            val calmarRatio = annReturns / maxDrawdown
            Some(calmarRatio)
          } else {
            None
          }

          logger.info(f"Total Value: $$${MathHelper.roundDecimals(historicalValues.last, 2)}%1.2f")
          logger.info(s"Ann Returns: ${MathHelper.roundDecimals(annReturns * 100d, 2)}%")
          logger.info(s"Ann Volatility: ${MathHelper.roundDecimals(annVolatility * 100d, 2)}%")
          logger.info(s"Max drawdown: ${MathHelper.roundDecimals(maxDrawdown * 100d, 2)}%")
          logger.info(s"Sharpe Ratio: ${MathHelper.roundDecimals(sharpeRatio, 3)}")
          logger.info(s"Sortino Ratio: ${MathHelper.roundDecimals(sortinoRatio, 3)}")
          logger.info(s"Calmar Ratio (3 yr): ${if (calmarRatio.isDefined) MathHelper.roundDecimals(calmarRatio.get, 3) else "N/A"}")
          AlgoResult(
            algoName = algo.name,
            annReturns = annReturns,
            annVolatility = annVolatility,
            maxDrawdown = maxDrawdown,
            sharpe = sharpeRatio,
            sortino = sortinoRatio,
            calmar = calmarRatio,
            historicalValues = historicalValues,
            historicalDates = historicalDates
          )
        }
        logger.info(s"Saving results")
        val currentDate = LocalDateTime.now()
        val displayId = RandomHelper.randomString(20)

        stockDB.transaction {
          //Inserts the backtest and gets the id.
          val backtest = Backtest(currentDate, displayId)
          val backtestInsert = quote {
            query[Backtest].insert(lift(backtest)).returning(_.id)
          }
          val backtestId = stockDB.run(backtestInsert)

          //Inserts the algo results with the backtest id.
          val resultsWithId = algoResults.map(_.copy(backtestId = backtestId))
          val algoResultInsert = quote {
            liftQuery(resultsWithId.toList).foreach(i => query[AlgoResult].insert(i))
          }
          blocking {
            stockDB.run(algoResultInsert)
          }
        }

        val conf = ConfigFactory.load()
        val chartViewerUrl = conf.getString("app.chartViewerUrl")
        logger.info(s"Results saved: $chartViewerUrl$displayId")
        logger.info("Back test successful")
    }
  }
  /**
    * Evals an algorithm from the resources folder.
    * @param filePath Resources file path of algorithm.
    * @return Returns the parsed algorithm.
    */
  private def loadAlgoFromFile(filePath: String): Algorithm = {
    val fileStream = getClass.getResourceAsStream(filePath)
    val fileStr = scala.io.Source.fromInputStream(fileStream).mkString

    loadAlgoFromString(fileStr)
  }

  private def loadAlgoFromString(str: String): Algorithm = {
    try {
      evalInstance.apply[Algorithm](str)
    } catch {
      case err: Throwable =>
        logger.error("Cannot parse algo:", err)
        throw err
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