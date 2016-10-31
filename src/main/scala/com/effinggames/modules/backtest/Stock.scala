package com.effinggames.modules.backtest

import com.effinggames.modules.SharedModels
import SharedModels.EodData

//Stock companion object for tracking the global eodData index, and intraday tick counts.
object Stock {
  //EodData seq index to use for getOpen(), getClose(), etc on.
  private var _currentDateIndex = 0
  def currentDateIndex = _currentDateIndex

  //Intraday tick counter, e.g. the 3rd 1 minute tick etc.
  private var _currentIntraDayTick = 0
  def currentIntraDayTick = _currentIntraDayTick

  //EodData seq length if a stock was listed during the full time period.
  private var _targetDataLength = 0
  def targetDataLength = _targetDataLength

  //Number of intraday ticks per day.
  val ticksPerDay = 2

  /**
    * Increments the intraday ticker count.
    * If the intraday ticker is maxed, then increment the day.
    */
  def incrementTicker(): Unit = {
    if (_currentIntraDayTick < ticksPerDay - 1) {
      _currentIntraDayTick += 1
    } else {
      _currentDateIndex += 1
      _currentIntraDayTick = 0
    }
  }

  /**
    * Sets the date index for the EodData array.
    */
  def setCurrentDateIndex(value: Int): Unit = {
    _currentDateIndex = value
    _currentIntraDayTick = 0
  }

  /**
    * Sets the data length for a fully listed stock.
    */
  def setTargetDataLength(value: Int): Unit = {
    _targetDataLength = value
  }

  def isFirstTickOfDay: Boolean = {
    currentIntraDayTick == 0
  }
  def isLastTickOfDay: Boolean = {
    currentIntraDayTick == ticksPerDay - 1
  }
}

//Wrapper class for a big seq of EodData.
class Stock(eodData: IndexedSeq[EodData]) {
  //Give internal access of underlying eodData to backtest module
  private[backtest] val _eodData = eodData
  //If the stock was listed later, then will need to offset data start point.
  val lengthOffset = Stock.targetDataLength - eodData.size
  /**
    * Gets the index for the current day, offset by daysAgo.
    * @param daysAgo How many days ago to offset.
    * @return Returns the array index for the current day.
    */
  def getDataIndex(daysAgo: Int): Int = Stock.currentDateIndex - lengthOffset - daysAgo

  /**
    * Checks if the stock was listed at the current time.
    * @return Returns true/false if the stock was listed at the current time.
    */
  def isListed: Boolean = getOpen() != -1

  //Gets the current price, the close price if end of day, or the open price otherwise.
  def getPrice: Float = {
    if (Stock.isLastTickOfDay) {
      getClose()
    } else {
      getOpen()
    }
  }

  def getOpen: Float = getOpen()
  def getOpen(daysAgo: Int = 0): Float = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.open
      case None => -1
    }
  }
  def getClose: Float = getOpen()
  def getClose(daysAgo: Int = 0): Float = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. Cannot query since day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.close
      case None => -1
    }
  }
  def getLow: Float = getOpen()
  def getLow(daysAgo: Int = 0): Float = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. Cannot query since day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.low
      case None => -1
    }
  }
  def getHigh: Float = getOpen()
  def getHigh(daysAgo: Int = 0): Float = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. Cannot query since day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.high
      case None => -1
    }
  }
  def getVolume: Float = getOpen()
  def getVolume(daysAgo: Int = 0): Float = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. Cannot query since day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.volume
      case None => -1
    }
  }
}