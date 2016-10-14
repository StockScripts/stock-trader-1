package com.effinggames.backtest

import com.effinggames.Module

object BacktestModule extends Module {
  val name = "Backtest"
  val triggerWord = "backtest"
  val helpText = "backtest <scriptname> <stock:APPL> <list:SP500> <pair:KO:PEP> <2005-2012> <2/3/2006-3/14/2006>"

  def run(params: List[String]): Unit = {
    params.head.toLowerCase match {
      case "test" =>
    }
  }
}