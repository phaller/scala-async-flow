/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.concurrent.{Flow => JFlow}

import scala.util.Try
import scala.util.control.NonFatal
import scala.concurrent.{Future, Promise, ExecutionContext}

trait Async[T] {
  def onComplete[S](handler: Try[T] => S) given (executor: ExecutionContext): Unit
  def getCompleted: Try[T]
  def getCompletedOnly: Try[T]
}

object Async {

  delegate PublisherOps {
    def (pub: JFlow.Publisher[T]) await[T, S] given (flow: Flow[S], executor: ExecutionContext): Option[T] = {
      val a: Async[Option[T]] = flow.getOrSubscribeTo(pub)
      Async.await(a)
    }

    def (fut: Future[T]) await[T, S] given (flow: Flow[S], executor: ExecutionContext): T = {
      Async.await(new AsyncFuture(fut))
    }
  }

  def rasync[T](body: given Flow[T] => T): JFlow.Publisher[T] = {
    delegate flow for Flow[T] = new Flow[T]

    val SCOPE = new ContinuationScope("async")

    val cont = new Continuation(SCOPE, new Runnable {
      def run(): Unit = {
        try {
          val v = body
          flow.emitNext(v)
          flow.emitComplete()
        } catch {
          case NonFatal(error) =>
            flow.emitError(error)
        }
      }
    })

    flow.init(cont, SCOPE)

    flow
  }

  def yieldNext[T](event: T) given (flow: Flow[T]) = {
    flow.yieldNext(event)
  }

  def yieldDone[T]() given (flow: Flow[T]): T = {
    flow.yieldDone()

    flow.suspend()

    0.asInstanceOf[T]
  }

  def yieldError[T](error: Throwable) given (flow: Flow[T]) = {
    flow.yieldError(error)

    flow.suspend()

    0.asInstanceOf[T]
  }

  def async[T](body: given Flow[T] => T): Future[T] = {
    delegate flow for Flow[T] = new Flow[T]

    val p = Promise[T]()

    // SCOPE is required for yielding the current continuation
    val SCOPE = new ContinuationScope("async")

    val cont = new Continuation(SCOPE, new Runnable {
      def run(): Unit = {
        p.success(body)
      }
    })

    flow.init(cont, SCOPE)

    cont.run()
    p.future
  }

  private[this] def await[T, S](a: Async[T]) given (flow: Flow[S], executor: ExecutionContext): T = {
    val res = a.getCompleted
    if (res eq null) {
      a.onComplete(x => flow.resume())
      flow.suspend()
      a.getCompleted.get
    } else {
      res.get
    }
  }

}
