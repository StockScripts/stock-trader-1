package com.effinggames.util

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
}
