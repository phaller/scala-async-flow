/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.{Timer, TimerTask}
import java.util.concurrent.Flow

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

object Test {

  private val timer = new Timer(true)

  /* Test rasync and yield. */
  def main(args: Array[String]): Unit = {
    import Async._

    val p = Promise[Boolean]()

    val pub = rasync {
      yieldNext(3)
      yieldNext(5)
      2
    }

    pub.subscribe(new Flow.Subscriber[Int] {
      def onNext(v: Int): Unit = {}
      def onError(t: Throwable): Unit = ???
      def onComplete(): Unit = {
        p.success(true)
      }
      def onSubscribe(s: Flow.Subscription): Unit = ???
    })

    println(Await.result(p.future, 2.seconds))
  }

  // create a Flow.Publisher[Int]
  def createPublisher[T](events: List[T]): Flow.Publisher[T] = {
    new Flow.Publisher[T] {
      def subscribe(sub: Flow.Subscriber[_ >: T]): Unit = {
        var remEvents = events
        timer.schedule(new TimerTask { self =>
          def run(): Unit = {
            remEvents match {
              case evt :: more =>
                remEvents = more
                sub.onNext(evt)
              case List() =>
                sub.onComplete()
                self.cancel() // cancel task
            }
          }
        }, 500, 500)
      }
    }
  }


  // create an rasync block which subscribes to the publisher:
  // rasync(pub) { ... }

  // within rasync block: await events using await(pub)
}
