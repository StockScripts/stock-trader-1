package com.effinggames.util

import io.getquill.{JdbcContext, PostgresDialect, SnakeCase}

object DatabaseHelper {
  val stockDB = new JdbcContext[PostgresDialect, SnakeCase]("stockDB")

  import stockDB._

  implicit val StringSeqDecoder: Decoder[Seq[String]] = decoder[Seq[String]] {
      resultSet => {
        index => {
          resultSet.getArray(index).getArray().asInstanceOf[Seq[String]]
        }
      }
    }
  implicit val StringSeqEncoder: Encoder[Seq[String]] = encoder[Seq[String]]( resultSet =>
      (index, seq) => {
        val conn = resultSet.getConnection
        resultSet.setArray(index, conn.createArrayOf("text", seq.toArray))
      },
      java.sql.Types.ARRAY
  )
}
