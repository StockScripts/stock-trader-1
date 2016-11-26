package com.effinggames.modules.download

import com.effinggames.modules.Module
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
  val helpText = "download <stocks --clean true --limit 100 --skip 10> <userLists>"

  private val stockListDirectory = "/stockLists/"
  private val userListDirectory = "/userLists/"

  def run(command: String, flags: Seq[String]): Future[Unit] = async {
    val limit = getFlag[Int](flags, "--limit")
    val skip = getFlag[Int](flags, "--skip")
    val clean = getFlag[Boolean](flags, "--clean")
    command match {
      case "stocks" =>
        await(fetchStocks(clean, limit, skip))
      case "userLists" =>
        await(parseUserLists())
    }
  }

  //Filters user / stock lists that aren't a csv or txt file.
  private def isCorrectFormat(str: String): Boolean = {
    val fileExt = str.takeRight(4)
    fileExt == ".txt" || fileExt == ".csv"
  }

  private def fetchStocks(clean: Option[Boolean] = None, limit: Option[Int] = None, skip: Option[Int] = None): Future[Unit] = async {
    if (clean.contains(true)) {
      logger.info("Truncating table eod_data")
      blocking { stockDB.executeAction("TRUNCATE eod_data") }
    }

    val filePaths = FileHelper.getFilePathsInDirectory(stockListDirectory)
      .filter(isCorrectFormat)

    await(StockFetcher.downloadStocksFromResourceFiles(filePaths, limit, skip))
  }

  private def parseUserLists(): Future[Unit] = async {
    logger.info("Truncating table user_list")
    blocking { stockDB.executeAction("TRUNCATE user_list") }

    val filePaths = FileHelper.getFilePathsInDirectory(userListDirectory) ++
      FileHelper.getFilePathsInDirectory(stockListDirectory)
      .filter(isCorrectFormat)

    await(FutureHelper.traverseSequential(filePaths)( filePath => {
      UserListParser.parseUserListFromResourceFile(filePath)
    }))
  }
}
