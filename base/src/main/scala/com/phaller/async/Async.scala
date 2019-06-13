/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.concurrent.Flow

import scala.util.Try
import scala.util.control.NonFatal
import scala.concurrent.{Future, Promise, ExecutionContext}

trait Async[T] {
  def onComplete[S](handler: Try[T] => S) given (executor: ExecutionContext): Unit
  def getCompleted: Try[T]
  def getCompletedOnly: Try[T]
}

object Async {

  delegate publisher2Async[T, S] for Conversion[Flow.Publisher[T], Async[Option[T]]] given (ctx: Context[S]) {
    def apply(pub: Flow.Publisher[T]): Async[Option[T]] = {
      val impl = ctx.asInstanceOf[PublisherImpl[S]]
      impl.getOrSubscribeTo(pub)
    }
  }

  delegate PublisherOps {
    def (pub: Flow.Publisher[T]) awaitPost[T,S] given (ctx: Context[S], ec: ExecutionContext): Option[T] = {
      val impl = the[Context[S]].asInstanceOf[PublisherImpl[S]]
      val a: Async[Option[T]] = impl.getOrSubscribeTo(pub)
      Async.await(a)
    }
  }

  def rasync[T](body: given Context[T] => T): Flow.Publisher[T] = {
    val flow = new PublisherImpl[T]
    delegate ctx for Context[T] = flow

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

  def yieldNext[T](event: T) given (ctx: Context[T]) = {
    ctx.yieldNext(event)
  }

  def yieldDone[T]() given (ctx: Context[T]): T = {
    ctx.yieldDone()

    val impl = ctx.asInstanceOf[PublisherImpl[T]]
    impl.suspend()

    0.asInstanceOf[T]
  }

  def yieldError[T](error: Throwable) given (ctx: Context[T]) = {
    ctx.yieldError(error)

    val impl = ctx.asInstanceOf[PublisherImpl[T]]
    impl.suspend()

    0.asInstanceOf[T]
  }

  def async[T](body: given Context[T] => T): Future[T] = {
    val pubImpl = new PublisherImpl[T]
    // as the concrete instance we should use a private implementation type
    delegate ctx for Context[T] = pubImpl

    val p = Promise[T]()

    // SCOPE is required for yielding the current continuation
    val SCOPE = new ContinuationScope("async")

    val cont = new Continuation(SCOPE, new Runnable {
      def run(): Unit = {
        p.success(body)
      }
    })

    pubImpl.init(cont, SCOPE)

    cont.run()
    p.future
  }

  def await[T, S](a: Async[T]) given (ctx: Context[S], executor: ExecutionContext): T = {
    val res = a.getCompleted
    if (res eq null) {
      val impl = ctx.asInstanceOf[PublisherImpl[S]]
      a.onComplete(x => impl.resume())
      impl.suspend()
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
