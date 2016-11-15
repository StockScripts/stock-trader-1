package com.effinggames.modules.backtest

import java.time.LocalDate

import com.effinggames.modules.SharedModels
import SharedModels.EodData
import com.effinggames.util.MathHelper

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

//Wrapper for a big seq of EodData that adds querying + statistical methods.
class Stock(eodData: IndexedSeq[EodData]) {
  //Give internal access of underlying eodData to backtest module
  private[backtest] val _eodData = eodData
  //If the stock was listed later, then will need to offset data start point.
  def lengthOffset = Stock.targetDataLength - eodData.size
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
  def getPrice: Double = {
    if (Stock.isLastTickOfDay) {
      getClose()
    } else {
      getOpen()
    }
  }

  def getDate: LocalDate = getDate()
  def getDate(daysAgo: Int = 0): LocalDate = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.date
      case None => LocalDate.ofEpochDay(0)
    }
  }
  def getOpen: Double = getOpen()
  def getOpen(daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.open
      case None => -1
    }
  }
  def getClose: Double = getClose()
  def getClose(daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.close
      case None => -1
    }
  }
  def getLow: Double = getLow()
  def getLow(daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.low
      case None => -1
    }
  }
  def getHigh: Double = getHigh()
  def getHigh(daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.high
      case None => -1
    }
  }
  def getVolume: Long = getVolume()
  def getVolume(daysAgo: Int = 0): Long = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    eodData.lift(getDataIndex(daysAgo)) match {
      case Some(data) => data.volume
      case None => -1
    }
  }

  //Gets the simple moving average over X days.
  def getSMA(overHowManyDays: Int, daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    val closeValues = (0 until overHowManyDays).map(i => getClose(i + daysAgo))
    MathHelper.mean(closeValues)
  }

  //Standard deviation of close to close change over X days.
  def getDailySD(overHowManyDays: Int, daysAgo: Int = 0): Double = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    val dailyChangeValues = (0 until overHowManyDays).map(i => {
      getClose(i + daysAgo) - getClose(i + daysAgo + 1)
    })
    MathHelper.stdDeviation(dailyChangeValues)
  }

  //Gets minimum volume over last X days.
  def getMinimumVolume(overHowManyDays: Int, daysAgo: Int): Long = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    val volumes = (0 until overHowManyDays).map(i => getVolume(i + daysAgo))
    volumes.min
  }

  //Gets average volume over last X days.
  def getAverageVolume(overHowManyDays: Int, daysAgo: Int): Long = {
    require(daysAgo >= 0, "daysAgo must be a non-negative.")
    require(!(daysAgo == 0 && !Stock.isLastTickOfDay), "No looking in the future. DaysAgo must be > 0 if day has not ended.")
    val volumes = (0 until overHowManyDays).map(i => getVolume(i + daysAgo))
    volumes.sum / volumes.size
  }
}