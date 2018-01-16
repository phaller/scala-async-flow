package scala.async.flow

import org.scalatest._

import scala.concurrent.{Future, Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._
import Flow._

class SugarSpec extends FlatSpec with Matchers {

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

  "simple async block with sugar" should "work" in {
    val p = Promise[Boolean]()

    val stream1 = fromIterator(from(1, 10))

    val forwarder = flow[Int](async {
      val buff = subscribe(stream1)
      await(delay(100))
      var next = await(buff)
      while (!next.isEmpty) {
        if (next.get % 2 == 0) yieldNext(next.get)
        await(delay(100))
        next = await(buff)
      }
      yieldDone()
      0
    })

    PublisherUtils.collectWhenDone(forwarder) { (vals: List[Int]) =>
      vals.size should be (4)
      for (i <- 1 until 10 if i % 2 == 0) {
        vals.contains(i) should be (true)
      }
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }

}
