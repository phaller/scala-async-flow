package com.phaller.async

import scala.util.{Try, Success => TSuccess, Failure => TFailure}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._

object Test1 {

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

  def test1(): Unit = {
    val stream1 = fromIterator(from(1, 10))

    val forwarder = new Flow[Int]
    forwarder.init((r: Context[Int]) => async {
      val buff = r.subscribe(stream1)
      println("[fwd] wait..")
      await(delay(1000))
      println("[fwd] continuing after first `await`...")
      var next = await(buff)
      while (!next.isEmpty) {
        if (next.get % 2 == 0) r.yieldNext(next.get)
        println("[fwd] wait..")
        await(delay(1000))
        next = await(buff)
      }
      r.yieldDone()
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
    test1()
  }

}
