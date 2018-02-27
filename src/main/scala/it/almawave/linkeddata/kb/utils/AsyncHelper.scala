package it.almawave.linkeddata.kb.utils

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.Await

object AsyncHelper {

  /**
   * this is a facility for avoiding writing  code like:
   * `val res = Await.result(future, Duration.Inf)`
   * and instead enable writing in the form of:
   * `val res = future.await`
   */
  implicit class AwaitFuture[R](future: Future[R]) {

    def await(implicit duration: Duration = Duration.Inf) = {
      Await.result(future, duration)
    }

  }

}