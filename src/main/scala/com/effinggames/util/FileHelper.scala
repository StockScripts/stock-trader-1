package com.effinggames.util

import java.io.File
import java.util.concurrent.{TimeUnit, Callable, Executors}
import java.util.jar.JarFile

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.{Failure, Success, Try}

object FileHelper {
  /**
    * Gets all the file paths in a resource directory. Works from IDE or inside a JAR.
    * @param directoryPath Relative path for the directory (relative to the resource folder).
    * @return Returns a vector of the full file paths inside the directory.
    */
  def getFilePathsInDirectory(directoryPath: String): Vector[String] = {
    var formattedPath = directoryPath
    //Adds trailing '/' to end if necessary
    if (directoryPath.last != '/') {
      formattedPath = formattedPath + '/'
    }

    val jarFile = new File(getClass.getProtectionDomain.getCodeSource.getLocation.getPath)
    val results = if (jarFile.isFile) {
      //When running from JAR
      //Removes trailing '/' from front if necessary
      if (directoryPath.head == '/') {
        formattedPath = formattedPath.drop(1)
      }

      val directoryNumOfSlashes = formattedPath.count(_ == '/')
      val filePaths = ArrayBuffer[String]()
      val jar = new JarFile(jarFile)
      val entries = jar.entries

      //Iterates over all the jar files
      while (entries.hasMoreElements) {
        val name = entries.nextElement().getName
        val numOfSlashes = name.count(_ == '/')
        //Matches file paths and filters out sub-directories
        if (name.startsWith(formattedPath) && numOfSlashes == directoryNumOfSlashes && name != formattedPath) {
          filePaths.append('/' + name)
        }
      }

      jar.close()
      filePaths.toVector
    } else {
      //When running from IDE
      //Adds trailing '/' to front if necessary
      if (directoryPath.head != '/') {
        formattedPath = '/' + formattedPath
      }

      Source.fromInputStream(getClass.getResourceAsStream(formattedPath)).getLines().map(formattedPath+_).toVector
    }

    results.sorted
  }

  /**
    * Waits for StdIn input (blocking), accepts either 1 line or a copy pasted block.
    * Source: http://stackoverflow.com/a/40433488/1836087
    * @return Returns lines of user input.
    */
  def getStdInChunk: List[String] = {  // blocks until StdIn has data
    val executor = Executors.newSingleThreadExecutor()
      val callable = new Callable[String]() {
        def call(): String = scala.io.StdIn.readLine()
      }

      def nextLine(acc: List[String]): List[String] =
        Try {executor.submit(callable).get(10L, TimeUnit.MILLISECONDS)} match {
          case Success(str) if str != null => nextLine(str :: acc)
          case _ =>
            executor.shutdownNow() // should test for Failure type
            acc.reverse
        }

      nextLine(List(scala.io.StdIn.readLine()))  // this is the blocking part
    }
  }
