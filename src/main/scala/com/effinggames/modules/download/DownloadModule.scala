package com.effinggames.modules.download

import com.effinggames.core.Module
import com.effinggames.util.{FileHelper, DatabaseHelper, LoggerHelper, FutureHelper}
import FutureHelper._
import DatabaseHelper.stockDB
import LoggerHelper.logger

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

object DownloadModule extends Module {
  val name = "Download"
  val triggerWord = "download"
  val helpText = "download <all> <stocks> <userLists>"

  private val stockListDirectory = "/stockLists/"
  private val userListDirectory = "/userLists/"

  def run(params: Seq[String]): Future[Unit] = async {
    params.head match {
      case "all" =>
        await(fetchStocks().withLogFailure)
        await(parseUserLists().withLogFailure)
      case "stocks" =>
        await(fetchStocks().withLogFailure)
      case "userLists" =>
        await(parseUserLists().withLogFailure)
    }
  }

  //Filters user / stock lists that aren't a csv or txt file.
  private def isCorrectFormat(str: String): Boolean = {
    val fileExt = str.takeRight(4)
    fileExt == ".txt" || fileExt == ".csv"
  }

  private def fetchStocks(): Future[Unit] = async {
    logger.info("Truncating table eod_data")
    blocking { stockDB.executeAction("TRUNCATE eod_data") }

    val filePaths = FileHelper.getFilePathsInDirectory(stockListDirectory)
      .filter(isCorrectFormat)
      .toVector

    await(FutureHelper.traverseSequential(filePaths)( filePath => {
      StockFetcher.downloadStocksFromResourceFile(filePath).withLogFailure
    }))
  }

  private def parseUserLists(): Future[Unit] = async {
    logger.info("Truncating table user_list")
    blocking { stockDB.executeAction("TRUNCATE user_list") }

    val filePaths = FileHelper.getFilePathsInDirectory(userListDirectory)
      .filter(isCorrectFormat)
      .toVector

    await(FutureHelper.traverseSequential(filePaths)( filePath => {
      UserListParser.parseUserListFromResourceFile(filePath).withLogFailure
    }))
  }
}
