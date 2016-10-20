package com.effinggames.core

import scala.concurrent.Future

abstract class Module {
  val name: String //e.g. "Backtest Module"
  val triggerWord: String //e.g. "backtest"
  val helpText: String //e.g. "backtest <scriptname> <stock:APPL> <list:SP500> <pair:KO:PEP> <2005-2012> <2/3/2006-3/14/2006>"

  def run(params: Seq[String]): Future[Unit]
}
