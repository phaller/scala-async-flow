package com.phaller.async

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._
import Flow._

object Test3 {

  def subscribe[T](pub: Publisher[T]): DynamicSubscription[T] = {
    val r = Context.r.asInstanceOf[Context[T]]
    r.subscribe(pub)
  }

  def yieldNext[T](t: T): Unit = {
    val r = Context.r.asInstanceOf[Context[T]]
    r.yieldNext(t)
  }

  def from(s: Int, f: Int): Iterator[Int] = new Iterator[Int] {
    val rnd = new java.util.Random
    var s0 = s
    def hasNext: Boolean = s0 < f
    def next(): Int = {
      Thread.sleep(rnd.nextInt(100) + 1)
      s0 += 1
      s0-1
    }
  }

  def test(): Unit = {
    val stream1 = fromIterator(from(1, 10))

    val forwarder = flow[Int](async {
      val buff = subscribe(stream1)
      println("[fwd] wait..")
      await(delay(1000))
      println("[fwd] continuing after first `await`...")
      var next = await(buff)
      while (!next.isEmpty) {
        if (next.get % 2 == 0) yieldNext(next.get)
        println("[fwd] wait..")
        await(delay(1000))
        next = await(buff)
      }
      yieldDone()
      0
    })

    val iter = makeIter(forwarder)
    println("[main] hasNext?")
    while (iter.hasNext) {
      val x = iter.next()
      println("" + x)
      println("[main] hasNext?")
    }
    println("[main] done")
  }

  def main(args: Array[String]): Unit = {
    test()
  }

}
