import com.effinggames.util.LoggerHelper.logger
import com.effinggames.modules.backtest.BacktestModule
import com.effinggames.modules.download.DownloadModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Handles the command line commands and loads the different modules.
  */
object Main {
  private val moduleList = List(BacktestModule, DownloadModule)
  private def printHelp(): Unit = moduleList.map(i => s"${i.name.capitalize} Usage: ${i.helpText}").foreach(println)

  def main(args: Array[String]) {
    if (args.length == 0) {
      printHelp()
    } else {
      val argList = args.toVector

      //Runs the modules that match the trigger word, and passes the remaining args.
      moduleList.find(_.triggerWord == argList.head) match {
        case Some(module) =>
          logger.info(s"Running ${module.name.capitalize} Module with: ${argList.drop(1).mkString(" ")}")
          val moduleFuture = module.run(argList(1), argList.drop(2))
          moduleFuture.onFailure {
            case err => logger.warn(s"${module.name.capitalize} failed - $err")
          }
          Await.ready(moduleFuture, 1.hour)
        case _ => printHelp()
      }
      logger.info("All Finished")
    }
    System.exit(0)
  }
}
