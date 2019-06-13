package com.phaller.async
package test

import org.reactivestreams.{Subscriber, Subscription}

import java.util.concurrent.locks.ReentrantLock

object PublisherUtils {

  def fromList[T](l: List[T]): Publisher[T] = new Publisher[T] {
    def subscribe(sub: Subscriber[_ >: T]): Unit = {
      l.foreach(elem => sub.onNext(elem))
      sub.onComplete()
    }
  }

  def collectWhenDone[T](obs: Publisher[T])(fun: List[T] => Unit): Unit =
    obs.subscribe(new Subscriber[T] {
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
      def onSubscribe(s: Subscription): Unit = {
        // TODO
      }
    })

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
      def onSubscribe(s: Subscription): Unit = {
        // TODO
      }
    })

}
