package scala.async.flow

import org.reactivestreams.{Publisher => RSPublisher, Subscriber, Subscription => RSSubscription}

import java.util.concurrent.locks.ReentrantLock

import scala.util.{Try, Success => TSuccess, Failure => TFailure}

import scala.concurrent.{Future, Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._

object TestCancel {

  def collectWhenError[T](obs: Publisher[T])(fun: List[T] => Unit): Unit =
    obs.subscribe(new Subscriber[T] {
      var vals: List[T] = List()
      val lock = new ReentrantLock
      def onNext(v: T): Unit = {
        lock.lock()
        vals = vals ::: List(v)
        lock.unlock()
      }
      def onError(t: Throwable): Unit = {
        fun(vals)
      }
      def onComplete(): Unit = {
        sys.error("error expected")
      }
      def onSubscribe(s: RSSubscription): Unit = {
        // TODO
      }
    })

  def test(): Unit = {
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
      println(s"[s2] res1 = $res1")
      ctx.yieldNext(res1.get)
      val res2 = await(buf)
      println(s"[s2] res2 = $res2")
      res2.get
    })

    collectWhenError(s2) { (vals: List[Int]) =>
      assert(vals.size == 1)
      assert(vals.contains(5))
      p.success(true)
    }

    Thread.sleep(200)

    ct.cancel()
    // s1 "realizes" that it is cancelled at the 2nd "yieldNext".

    Await.ready(p.future, 5.seconds)
  }

  def main(args: Array[String]): Unit = {
    test()
  }

}
