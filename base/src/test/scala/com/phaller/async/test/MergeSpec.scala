package com.phaller.async
package test

import org.scalatest._

import org.reactivestreams.Subscriber

import java.util.Random

import scala.concurrent.{Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._
import Flow._

class MergeSpec extends FlatSpec with Matchers {

  def mkObservableOnThread[T](iter: Iterator[T], rnd: Random, name: String): Publisher[T] = new Publisher[T] {
    def subscribe(s: Subscriber[_ >: T]): Unit = {
      // start publishing events on separate thread
      val task = new Runnable {
        def run(): Unit = {
          while (iter.hasNext) {
            Thread.sleep(rnd.nextInt(100) + 10)
            s.onNext(iter.next())
          }
          s.onComplete()
        }
      }
      (new Thread(task, name)).start()
    }
  }

  def mkObservableTerminatingWithError[T](iter: Iterator[T], rnd: Random, name: String): Publisher[T] = new Publisher[T] {
    def subscribe(s: Subscriber[_ >: T]): Unit = {
      // start publishing events on separate thread
      val task = new Runnable {
        def run(): Unit = {
          while (iter.hasNext) {
            Thread.sleep(rnd.nextInt(100) + 10)
            s.onNext(iter.next())
          }
          s.onError(new Exception("simulated error"))
        }
      }
      (new Thread(task, name)).start()
    }
  }

  "merge" should "merge two streams" in {
    val p = Promise[Boolean]()

    val rnd = new Random(100L)
    val range1 = Iterator.range(10, 20)
    val range2 = Iterator.range(20, 30)
    val stream1 = mkObservableOnThread(range1, rnd, "range1")
    val stream2 = mkObservableOnThread(range2, rnd, "range2")

    // create an observable that merges the two streams
    val mergedStream = merge(stream1, stream2)

    val obs = flow[Int](async {
      val buff = subscribe(mergedStream)
      var i = 0
      while (i < 20) {
        val opt = await(buff)
        yieldNext(opt.get)
        i += 1
      }
      yieldDone()
      0
    })

    PublisherUtils.collectWhenDone(obs) { (vals: List[Int]) =>
      vals.size should be (20)
      for (i <- 10 until 30) {
        vals.contains(i) should be (true)
      }
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

  "merge" should "support errors" in {
    val p = Promise[Boolean]()

    val rnd = new Random(100L)
    val range1 = Iterator.range(10, 20)
    val range2 = Iterator.range(20, 29)
    val stream1 = mkObservableOnThread(range1, rnd, "range1")
    val stream2 = mkObservableTerminatingWithError(range2, rnd, "range2")

    // create an observable that merges the two streams
    val mergedStream = merge(stream1, stream2)

    val publisher = flow[Int](async {
      val buff = subscribe(mergedStream)
      var i = 0
      while (i < 20) {
        val opt = await(buff)
        yieldNext(opt.get)
        i += 1
      }
      yieldDone()
      0
    })

    PublisherUtils.collectWhenError(publisher) { (vals: List[Int]) =>
      vals.size should be (19)
      for (i <- 10 until 29) {
        vals.contains(i) should be (true)
      }
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

}
