package com.phaller.async
package test

import org.scalatest._

import org.reactivestreams.Subscriber

import scala.concurrent.{Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._

class AwaitOnlySpec extends FlatSpec with Matchers {

  /* Sleeps for `delay` ms before emitting each element returned by `iter`.
   */
  def mkObservableOnThread[T](iter: Iterator[T], delay: Long, name: String): Publisher[T] = new Publisher[T] {
    def subscribe(o: Subscriber[_ >: T]): Unit = {
      // start producing values on separate thread
      val task = new Runnable {
        def run(): Unit = {
          while (iter.hasNext) {
            Thread.sleep(delay)
            o.onNext(iter.next())
          }
          o.onComplete()
        }
      }
      (new Thread(task, name)).start()
    }
  }

  "awaitOnly" should "work" in {
    val p = Promise[Boolean]()

    val r1 = Iterator.range(10, 13)
    val r2 = Iterator.range(20, 21)
    val s1 = mkObservableOnThread(r1, 250, "s1")
    val s2 = mkObservableOnThread(r2, 600, "s2")

    val obs = new Flow[Int]
    obs.init((r: Context[Int]) => async {
      val b1 = r.subscribe(s1)
      val b2 = r.subscribe(s2)

      val opt1 = await(b1)
      r.yieldNext(opt1.get)
      val opt2 = awaitOnly(b2)
      r.yieldNext(opt2.get)
      val opt3 = await(b1)
      opt3.get
    })

    PublisherUtils.collectWhenDone(obs) { (vals: List[Int]) =>
      vals should be (List(10, 20, 12))
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

}
