import com.effinggames.Module
import com.effinggames.backtest.BacktestModule
import com.effinggames.database.DatabaseModule

/**
  * Handles the command line commands and loads the different modules.
  */
object Main {
  private val moduleList: List[Module] = List(BacktestModule, DatabaseModule)
  private def printHelp(): Unit = moduleList.map(i => s"${i.name.capitalize} Usage: ${i.helpText}").foreach(println)

  def main(args: Array[String]) {
    if (args.length == 0) {
      printHelp()
    } else {
      val argList = args.toList.map(_.toLowerCase)

      //Runs the modules that match the trigger word, and passes the remaining args.
      moduleList.filter(_.triggerWord == argList.head) match {
        case i if i.nonEmpty => i.foreach(_.run(argList.drop(1)))
        case _ => printHelp()
      }
    }
  }
}
