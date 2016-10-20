package com.effinggames.modules.backtest


import com.effinggames.core.Module

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BacktestModule extends Module {
  val name = "Backtest"
  val triggerWord = "backtest"
  val helpText = "backtest <scriptname> <stock:APPL> <list:SP500> <pair:KO:PEP> <2005-2012> <2/3/2006-3/14/2006>"

  def run(params: Seq[String]): Future[Unit] = async {
    params.head.toLowerCase match {
      case "test" =>
    }
  }
}