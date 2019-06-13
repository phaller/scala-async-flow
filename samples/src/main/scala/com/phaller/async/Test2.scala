package com.phaller.async

import scala.concurrent.ExecutionContext.Implicits.global

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._

object Test2 {
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

  var buff: BufferedSubscription[Int] = null
/*
  def test(): Unit = {
    val stream1 = makeObs(from(1, 10))

    //implicit val ic: Reaction[Int] = null

    buff = subscribe(stream1)

    val forwarder = rasync[Int] {
      println("[fwd] wait..")
      await(delay(1000))
      println("[fwd] continuing after first `await`...")
      var next = await(buff)
      while (!next.isNoth) {
        if (next.value % 2 == 0) yields(next.value)
        println("[fwd] wait..")
        await(delay(1000))
        next = await(buff)
      }
    }

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
 */
}
