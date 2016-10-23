package com.effinggames.util

object MathHelper {
  /**
    * Rounds a number to X decimals.
    * @param number Number to round.
    * @param decimalLimit Limit to X decimal digits.
    * @return Returns the rounded number.
    */
  def roundDecimals[A <: Numeric](number: A, decimalLimit: Int): A = {
    val roundingFactor = math.pow(10, decimalLimit)
    Math.round(number * roundingFactor) / roundingFactor
  }
}
