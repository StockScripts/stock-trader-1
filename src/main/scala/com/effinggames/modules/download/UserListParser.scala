package com.effinggames.modules.download

import com.effinggames.modules.SharedModels
import com.effinggames.util.{LoggerHelper, DatabaseHelper}
import LoggerHelper._
import SharedModels.UserList
import DatabaseHelper._
import DatabaseHelper.stockDB._

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object UserListParser {

  def parseUserListFromResourceFile(fileName: String): Future[RunActionResult] = async {
    logger.info(s"Parsing user list for $fileName")
    val listName = fileName.split("/").last.split("[.]")(0)

    val listStream = getClass.getResourceAsStream(fileName)
    val symbols = scala.io.Source.fromInputStream(listStream).getLines.toVector.map(_.trim)
    val userList = UserList(listName, symbols)

    val insertQuery = quote {
      query[UserList].insert(lift(userList))
    }

    val results = blocking {
      stockDB.run(insertQuery)
    }

    logger.info(s"Successfully inserted ${symbols.length} rows for $listName")
    results
  }
}
