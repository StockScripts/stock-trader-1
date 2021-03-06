package com.effinggames.modules.download

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import com.effinggames.modules.sharedModels
import com.effinggames.util.{MathHelper, DatabaseHelper, LoggerHelper, FutureHelper}
import FutureHelper._
import DatabaseHelper.stockDB
import DatabaseHelper.stockDB._
import LoggerHelper.logger
import sharedModels.EodData

import com.github.tototoshi.csv.CSVReader
import play.api.libs.ws.ning.NingWSClient

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.io.Source
import scala.util.Try

object StockFetcher {
  private val wsClient = NingWSClient()
  //Limits fetching from Yahoo to 10 symbols at a time
  private val fixedPool = Executors.newFixedThreadPool(10)
  private val fixedEC = ExecutionContext.fromExecutorService(fixedPool)

  /**
    * Downloads all the stocks listed in the resources file.
    * @param filePaths List of file paths to get symbols from.
    * @param limit How many of the symbols to download.
    * @param skip How many of the symbols to skip.
    * @return
    */
  def downloadStocksFromResourceFiles(
    filePaths: Seq[String],
    limit: Option[Int] = None,
    skip: Option[Int] = None
  ): Future[Seq[Try[RunBatchActionResult]]] = async {
    val limitNum = limit.getOrElse(Int.MaxValue)
    val skipNum = skip.getOrElse(0)
    logger.info(s"Downloading stocks for ${filePaths.mkString(" ")}")
    logger.info(s"Limiting to $limitNum symbols, skipping $skipNum symbols")

    //Gets all the symbols from the files.
    val symbols = filePaths.flatMap({ filePath =>
      val fileStream = getClass.getResourceAsStream(filePath)
      scala.io.Source.fromInputStream(fileStream).getLines
    })

    //Fetches the stock csv and inserts into database.
    //Throttles to only downloading 10 csv's at a time.
    val throttledFutures = symbols.slice(skipNum, skipNum + limitNum).map( csvLine => {
      val symbol = csvLine.trim
      val future = Future {
        Await.result(fetchStockCSV(symbol.trim), 2.minutes)
      }(fixedEC)

      future.onFailure {
        case err => logger.warn(s"Failed to find data for $symbol - $err")
      }

      future.toTry
    })

    val results = await(Future.sequence(throttledFutures))
    logger.info(s"Finished downloading EOD data for ${results.length} stocks")
    results
  }

  //Fetches eod stock CSV from Yahoo and inserts into DB.
  private def fetchStockCSV(symbol: String): Future[RunBatchActionResult] = async {
    val formattedSymbol = symbol.toUpperCase
    logger.info(s"Fetching data for $formattedSymbol")
    val csvRsp = await(wsClient.url(s"http://ichart.finance.yahoo.com/table.csv?s=$formattedSymbol&g=d&a=1&b=1&c=2000&ignore=.csv").get())

    if (csvRsp.status == 200) {
      val reader = CSVReader.open(Source.fromString(csvRsp.body))
      val csvLines = reader.allWithHeaders.reverse
      val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val eodDataList = csvLines.map(i => {
        val adjustFactor = i("Adj Close").toDouble / i("Close").toDouble
        //Adjusts and rounds the value
        def getFormattedField(value: Double): Double = {
          MathHelper.roundDecimals(value * adjustFactor, 2)
        }
        EodData(
          symbol = formattedSymbol,
          date = LocalDate.parse(i("Date"), dateFormatter),
          open = getFormattedField(i("Open").toDouble),
          high = getFormattedField(i("High").toDouble),
          low = getFormattedField(i("Low").toDouble),
          close = getFormattedField(i("Close").toDouble),
          volume = i("Volume").toLong,
          adjClose = i("Adj Close").toDouble
        )
      })

      val insertQuery = quote {
        liftQuery(eodDataList).foreach(i => query[EodData].insert(i))
      }

      val results = blocking {
        stockDB.run(insertQuery)
      }

      logger.info(s"Successfully inserted ${eodDataList.length} rows for $symbol")
      results
    } else {
      throw new Exception("Stock CSV not found.")
    }
  }
}