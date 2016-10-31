package com.effinggames.util

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

import LoggerHelper._

object FutureHelper {
  /**
    * Takes a list of elements and maps it to a future generating function.
    * Executes the futures sequentially, e.g. the sequential version of Future.traverse()
    * @param list List of elements to be iterated over.
    * @param fn Iterator function that returns a Future.
    * @example FutureHelper.traverseSequential(listOfStr)(getUrl)
    * @return Returns a future s.
    */
  def traverseSequential[A, B](list: Seq[A])(fn: A => Future[B])
    (implicit ec: ExecutionContext): Future[Vector[B]] = {
    val results = ArrayBuffer[B]()
    var future = Future()

    //Iterates through list and chains futures sequentially.
    //Appends individual results to the results array.
    list.foreach( item =>
      future = future.flatMap { i =>
        fn(item).map { result =>
          results.append(result)
        }
      }
    )

    //When all the futures complete, returns the results array.
    future.map(i => results.toVector)
  }

  implicit class FutureWithTrying[A](self: Future[A])(implicit ec: ExecutionContext) {
    /**
      * Converts the Future to a Try[Future].
      * @return Returns the future wrapped in a try.
      */
    def toTry(implicit ec: ExecutionContext): Future[Try[A]] = {
      val promise = Promise[Try[A]]()
      self.onComplete(promise.success)
      promise.future
    }
  }


  implicit class FutureWithFailureLogging[A](self: Future[A])(implicit ec: ExecutionContext) {
    /**
      * Adds a logger warn on future failure.
      * @return Returns the future.
      */
    def withLogFailure: Future[A] = {
      self.onFailure {
        case err => logger.warn(s"Future failed - $err")
      }
      self
    }
  }
}
