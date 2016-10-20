package com.effinggames.modules.download

import com.effinggames.core.{Module, FutureHelper, LoggerHelper, DatabaseHelper}
import DatabaseHelper.stockDB
import LoggerHelper.logger
import org.apache.commons.io.{Charsets, IOUtils}

import scala.async.Async.{async, await}
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

object DownloadModule extends Module {
  val name = "Download"
  val triggerWord = "download"
  val helpText = "download <all> <stocks> <userLists>"

  def run(params: Seq[String]): Future[Unit] = async {
    params.head match {
      case "all" =>
        await(fetchStocks())
        await(parseUserLists())
      case "stocks" =>
        await(fetchStocks())
      case "userLists" =>
        await(parseUserLists())
    }
  }

  //Filters user / stock lists that are named incorrectly.
  private def isCorrectFormat(str: String): Boolean = str.matches("[a-zA-Z0-9_]*[.]txt")

  private def fetchStocks(): Future[Unit] = async {
    logger.info("Truncating table eod_data")
    blocking { stockDB.executeAction("TRUNCATE eod_data") }

    val stockListDirectory = "/stockLists/"
    val fileNames = IOUtils.readLines(getClass.getResourceAsStream(stockListDirectory), Charsets.UTF_8)
      .filter(isCorrectFormat)
      .toVector
    await(FutureHelper.traverseSequential(fileNames)( fileName => {
      StockFetcher.downloadStocksFromResourceFile(s"$stockListDirectory$fileName")
    }))
  }

  private def parseUserLists(): Future[Unit] = async {
    logger.info("Truncating table user_list")
    blocking { stockDB.executeAction("TRUNCATE user_list") }

    val userListDirectory = "/userLists/"
    val fileNames = IOUtils.readLines(getClass.getResourceAsStream(userListDirectory), Charsets.UTF_8)
      .filter(isCorrectFormat)
      .toVector
    await(FutureHelper.traverseSequential(fileNames)( fileName => {
      UserListParser.parseUserListFromResourceFile(s"$userListDirectory$fileName")
    }))
  }
}
