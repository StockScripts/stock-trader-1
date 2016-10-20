package com.effinggames.core

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object FutureHelper {
  /**
    * Takes a list of elements and maps it to a future generating function.
    * Executes the futures sequentially, e.g. the sequential version of Future.traverse()
    *
    * @param list List of elements to be iterated over.
    * @param fn Iterator function that returns a Future.
    * @return Return a future
    */
  def traverseSequential[A, B](list: Seq[A])(fn: A => Future[B])
    (implicit ec: ExecutionContext): Future[Seq[B]] = {
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
    future.map(i => results)
  }

  /**
    * Converts a Future to a Try[Future].
    * @param future The future being converted
    * @return Returns the future wrapped in a try.
    */
  def convertToTry[A](future: Future[A])(implicit ec: ExecutionContext): Future[Try[A]] = {
    val promise = Promise[Try[A]]()
    future.onComplete(promise.success)
    promise.future
  }
}