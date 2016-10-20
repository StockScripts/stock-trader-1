package com.effinggames.modules.download

import java.text.SimpleDateFormat
import java.util.concurrent.Executors

import com.effinggames.core.{LoggerHelper, FutureHelper, DatabaseHelper}
import DatabaseHelper.stockDB
import DatabaseHelper.stockDB._
import LoggerHelper.logger
import com.effinggames.core.Models.EodData

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
  private val fixedPool = Executors.newFixedThreadPool(10)
  private val fixedEC = ExecutionContext.fromExecutorService(fixedPool)

  //Downloads all the stocks listed in the resources file.
  def downloadStocksFromResourceFile(fileName: String): Future[Iterator[Try[RunBatchActionResult]]] = async {
    logger.info(s"Downloading stocks for $fileName")
    val stockList = getClass.getResourceAsStream(fileName)

    //Fetches the stock csv and inserts into database.
    //Throttles to only downloading 10 csv's at a time.
    val throttledFutures = scala.io.Source.fromInputStream(stockList).getLines.map( csvLine => {
      val symbol = csvLine.trim
      val future = Future {
        Await.result(fetchStockCSV(symbol.trim), 2.minutes)
      }(fixedEC)

      future.onFailure {
        case err => logger.warn(s"Failed to find data for $symbol - $err")
      }

      FutureHelper.convertToTry(future)
    })

    val results = await(Future.sequence(throttledFutures))
    logger.info(s"Finished downloading $fileName for ${results.length} stocks")
    results
  }

  //Fetches eod stock CSV from Yahoo and inserts into DB.
  private def fetchStockCSV(symbol: String): Future[RunBatchActionResult] = async {
    logger.info(s"Fetching data for $symbol")
    val csvRsp = await(wsClient.url(s"http://ichart.finance.yahoo.com/table.csv?s=$symbol&g=d&a=1&b=1&c=2000&ignore=.csv").get())

    if (csvRsp.status == 200) {
      val reader = CSVReader.open(Source.fromString(csvRsp.body))
      val csvLines = reader.allWithHeaders.reverse
      val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
      val eodDataList = csvLines.map(i => EodData(
        symbol = symbol,
        date = dateFormatter.parse(i("Date")),
        open = i("Open").toFloat,
        high = i("High").toFloat,
        low = i("Low").toFloat,
        close = i("Close").toFloat,
        volume = i("Volume").toLong,
        adjClose = i("Adj Close").toFloat
      ))

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