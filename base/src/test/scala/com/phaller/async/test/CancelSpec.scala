package com.phaller.async
package test

import org.scalatest._

import scala.concurrent.{Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._

class CancelSpec extends FlatSpec with Matchers {

  "cancelling a cancellation tag" should "cancel publisher" in {
    val p = Promise[Boolean]()

    val source = new CancellationTagSource
    val ct = source.mkTag

    // pass cancellation token to enable cancellation
    val s1 = new Flow[Int](Some(ct))
    s1.init((ctx: Context[Int]) => async {
      await(delay(100))
      ctx.yieldNext(5)
      await(delay(500))
      ctx.yieldNext(6)
      await(delay(500))
      7
    })

    val s2 = new Flow[Int]
    s2.init((ctx: Context[Int]) => async {
      val buf = ctx.subscribe(s1)
      val res1 = await(buf)
      ctx.yieldNext(res1.get)
      val res2 = await(buf)
      res2.get
    })

    PublisherUtils.collectWhenError(s2) { (vals: List[Int]) =>
      vals.size should be (1)
      vals.contains(5) should be (true)
      p.success(true)
    }

    Thread.sleep(200)

    ct.cancel()
    // s1 "realizes" that it is cancelled at the 2nd "yieldNext".

    Await.ready(p.future, 5.seconds)
  }

}
