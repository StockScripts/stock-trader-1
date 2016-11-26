package com.effinggames.util

object RandomHelper {
  /**
    * Generate random lowercase alphanumeric string of length X.
    * @param length Length of string.
    * @return Returns a random string.
    */
  def randomString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
  }

  private def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }
}
