/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async
package test

import scala.language.implicitConversions

import org.junit.Test

import scala.concurrent.{Future, Promise, ExecutionContext, Await}
import scala.concurrent.duration._

import Async._
import delegate Async._
import Utils.{delay, delayError}

import delegate AsyncFuture._

class AsyncSpec {

  delegate for ExecutionContext = ExecutionContext.global

  @Test def testAsync(): Unit = {
    val myFut = delay(500, true)

    val done = async[Boolean] {
      val res = await(myFut)
      res
    }

    assert(Await.result(done, 2.seconds))
  }

  @Test def testYieldNext(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      2
    }

    PublisherUtils.collectWhenDone(pub) { (vals: List[Int]) =>
      assert(vals == List(3, 5, 2))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testYieldDone(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      yieldNext(2)
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub) { (vals: List[Int]) =>
      assert(vals == List(3, 5, 2))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testAwait(): Unit = {
    val p = Promise[Boolean]()
    val fut = delay(500, 3)

    val pub = rasync {
      yieldNext(5)
      val num = await(fut)
      yieldNext(num)
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub) { (vals: List[Int]) =>
      assert(vals == List(5, 3))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testAwaitFutureException(): Unit = {
    val p = Promise[Boolean]()
    val fut = delayError[Int](500, new Exception("boom"))

    val pub = rasync {
      yieldNext(5)
      try {
        val num = await(fut)
      } catch {
        case e: Exception =>
          yieldNext(4)
      }
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub) { (vals: List[Int]) =>
      assert(vals == List(5, 4))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testPubToSub(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      yieldNext(2)
      yieldDone()
    }

    val pub2 = rasync {
      yieldNext(5)
      val res = await(pub)
      yieldNext(res.get)
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub2) { (vals: List[Int]) =>
      assert(vals == List(5, 3))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testAwaitPublisherException(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync[Int] {
      yieldError(new Exception("boom"))
    }

    val pub2 = rasync {
      yieldNext(5)
      try {
        val res = await(pub)
        yieldNext(res.get)
      } catch {
        case e: Exception =>
          yieldNext(4)
      }
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub2) { (vals: List[Int]) =>
      assert(vals == List(5, 4))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testAwaitTermination(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      yieldNext(2)
      yieldDone()
    }

    val pub2 = rasync[Int] {
      var res = await(pub)
      while (res.nonEmpty) {
        yieldNext(res.get)
        res = await(pub)
      }
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub2) { (vals: List[Int]) =>
      assert(vals == List(3, 5, 2))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  @Test def testAwaitTerminationExt(): Unit = {
    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      yieldNext(2)
      yieldDone()
    }

    val pub2 = rasync[Int] {
      var res = pub.awaitPost
      while (res.nonEmpty) {
        yieldNext(res.get)
        res = await(pub)
      }
      yieldDone()
    }

    PublisherUtils.collectWhenDone(pub2) { (vals: List[Int]) =>
      assert(vals == List(3, 5, 2))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  def yielding(scope: ContinuationScope): Unit = {
    Continuation.`yield`(scope)
  }

  @Test def testCallYield(): Unit = {
    val SCOPE = new ContinuationScope("generators")
    var continue = false
    val cont = new Continuation(SCOPE, new Runnable {
      def run(): Unit = {
        while (!continue) {
          yielding(SCOPE)
        }
      }
    })
    cont.run()
    continue = true
    cont.run()
    assert(cont.isDone())
  }

}
