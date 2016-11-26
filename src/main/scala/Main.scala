import com.effinggames.util.LoggerHelper.logger
import com.effinggames.util.FutureHelper._
import com.effinggames.modules.backtest.BacktestModule
import com.effinggames.modules.download.DownloadModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Handles the command line commands and loads the different modules.
  */
object Main {
  private val modules = Seq(BacktestModule, DownloadModule)
  private def printHelp(): Unit = modules.map(i => s"${i.name.capitalize} Usage: ${i.helpText}").foreach(println)

  def main(_args: Array[String]) {
    if (_args.length == 0) {
      printHelp()
    } else {
      val args = _args.toVector

      //Runs the modules that match the trigger word, and passes the remaining args.
      modules.find(_.triggerWord == args.head) match {
        case Some(module) =>
          logger.info(s"Running ${module.name.capitalize} Module with: ${args.drop(1).mkString(" ")}")
          val moduleFuture = module.run(args(1), args.drop(2)).withLogFailure
          Await.ready(moduleFuture, 1.hour)
        case _ => printHelp()
      }
      logger.info("All Finished")
    }
    System.exit(0)
  }
}
