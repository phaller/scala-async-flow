/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async
package test

import java.util.concurrent.Flow
import java.util.concurrent.locks.ReentrantLock

object PublisherUtils {

  def collectWhenDone[T](obs: Flow.Publisher[T])(fun: List[T] => Unit): Unit =
    obs.subscribe(new Flow.Subscriber[T] {
      var vals: List[T] = List()
      val lock = new ReentrantLock
      def onNext(v: T): Unit = {
        lock.lock()
        vals = vals ::: List(v)
        lock.unlock()
      }
      def onError(t: Throwable): Unit = {
        // TODO
        sys.error("oh, no!")
      }
      def onComplete(): Unit = {
        fun(vals)
      }
      def onSubscribe(s: Flow.Subscription): Unit = {
        // TODO
      }
    })

}
