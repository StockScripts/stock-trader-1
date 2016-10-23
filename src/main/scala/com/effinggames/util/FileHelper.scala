package com.effinggames.util

import java.io.File
import java.util.jar.JarFile

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

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
}
