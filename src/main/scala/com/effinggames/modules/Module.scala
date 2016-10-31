package com.effinggames.modules

import com.effinggames.util.LoggerHelper._

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class Module {
  val name: String //e.g. "Backtest"
  val triggerWord: String //e.g. "backtest"
  val helpText: String //e.g. "backtest <scriptname> <stock:APPL> <list:SP500> <pair:KO:PEP> <2005-2012> <2/3/2006-3/14/2006>"

  def run(command: String, flags: Seq[String]): Future[Unit]

  /**
    * Gets an optional flag value.
    * @param flags Array of flags that is passed to module.
    * @param param The param name, e.g. "-limit"
    * @return Returns the flag value, if it exists.
    */
  protected def getFlag[A: ClassTag](flags: Seq[String], param: String): Option[A] = {
    val c = implicitly[ClassTag[A]].runtimeClass
    try {
      flags.indexWhere(_ == param) match {
        case -1 => None
        case i => flags(i + 1) match {
          case str if c == classOf[Float] => Some(str.toDouble.asInstanceOf[A])
          case str if c == classOf[Int] => Some(str.toInt.asInstanceOf[A])
          case str if c == classOf[String] => Some(str.asInstanceOf[A])
          case str if c == classOf[Boolean] => Some(str.toBoolean.asInstanceOf[A])
          case _ =>
            logger.error(s"""getFlag(flags, "$param") - Type [${c.toString.capitalize}] is not supported!""")
            None
        }
      }
    } catch {
      case err: Throwable =>
        logger.error(s"""getFlag(flags, "$param") - Unable to parse ${c.toString.capitalize}: $err""")
        None
    }

  }
}
