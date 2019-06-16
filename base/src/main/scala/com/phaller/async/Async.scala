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

  delegate [T, S] for Conversion[JFlow.Publisher[T], Async[Option[T]]] given (flow: Flow[S]) {
    def apply(pub: JFlow.Publisher[T]): Async[Option[T]] = {
      flow.getOrSubscribeTo(pub)
    }
  }

  delegate PublisherOps {
    def (pub: JFlow.Publisher[T]) awaitPost[T,S] given (flow: Flow[S], ec: ExecutionContext): Option[T] = {
      val a: Async[Option[T]] = flow.getOrSubscribeTo(pub)
      Async.await(a)
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

  def await[T, S](a: Async[T]) given (flow: Flow[S], executor: ExecutionContext): T = {
    val res = a.getCompleted
    if (res eq null) {
      a.onComplete(x => flow.resume())
      flow.suspend()
      a.getCompleted.get
    } else {
      res.get
    }
  }

  def main(args: Array[String]): Unit = {
    println("================")
    println("TEST 1")
    println("================")
    val SCOPE = new ContinuationScope("generators")
    var continue = false
    val cont = new Continuation(SCOPE, new Runnable {
      def run(): Unit = {
        println("hello from continuation")
        while (!continue) {
          println("suspending")
          Continuation.`yield`(SCOPE)
          println("resuming")
        }
        println("all the way to the end")
      }
    })
    cont.run()
    println("isDone: " + cont.isDone())
    cont.run()
    println("isDone: " + cont.isDone())

    continue = true
    cont.run()
    println("isDone: " + cont.isDone())
  }
}
