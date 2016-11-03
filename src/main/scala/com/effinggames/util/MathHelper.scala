package com.effinggames.util

import scala.annotation.tailrec
import scala.language.postfixOps

object MathHelper {
  /**
    * Rounds a number to X decimals.
    * @param number Number to round.
    * @param decimalLimit Limit to X decimal digits.
    * @return Returns the rounded number.
    */
  def roundDecimals(number: Float, decimalLimit: Int): Float = {
    val roundingFactor = math.pow(10, decimalLimit).toFloat
    math.round(number * roundingFactor) / roundingFactor
  }

  /**
    * Gets the mean for a list of numbers.
    * @return Returns the mean as a float.
    */
  def mean[T](items: Traversable[T])(implicit n: Numeric[T]): Float = {
    n.toFloat(items.sum) / items.size.toFloat
  }

  /**
    * Gets the variance for a list of numbers.
    * @return Returns the variance as a float.
    */
  def variance[T](items: Traversable[T])(implicit n: Numeric[T]): Float = {
    val itemMean = mean(items)
    val sumOfSquares = items.foldLeft(0.0f)((total, item)=>{
      val itemValue = n.toFloat(item)
      val square = math.pow(itemValue - itemMean, 2).toFloat
      total + square
    })
    sumOfSquares / items.size.toFloat
  }

  /**
    * Gets the standard deviation for a list of numbers.
    * @return Returns the standard deviation as a float.
    */
  def stdDeviation[T](items: Traversable[T])(implicit n: Numeric[T]): Float = {
    math.sqrt(variance(items)).toFloat
  }

  /**
    * Linear time median algorithm.
    * Source: http://stackoverflow.com/questions/4662292/scala-median-implementation
    */
  @tailrec private def findKMedian(arr: Seq[Float], k: Int): Float = {
    val a = arr(scala.util.Random.nextInt(arr.size))
    val (s, b) = arr partition (a >)
    if (s.size == k) a
    // The following test is used to avoid infinite repetition
    else if (s.isEmpty) {
      val (s, b) = arr partition (a ==)
      if (s.size > k) a
      else findKMedian(b, k - s.size)
    } else if (s.size < k) findKMedian(b, k - s.size)
    else findKMedian(s, k)
  }

  /**
    * Gets the median for a list of numbers.
    * @return Returns the median as a float.
    */
  def findMedian[T](items: Traversable[T])(implicit n: Numeric[T]): Float = {
    val indexedArr = items.map(n.toFloat).toVector
    if (indexedArr.size % 2 == 0) {
      (findKMedian(indexedArr, indexedArr.size / 2 - 1) + findKMedian(indexedArr, indexedArr.size / 2)) / 2
    } else {
      findKMedian(indexedArr, (indexedArr.size - 1) / 2)
    }
  }
}
