package com.phaller.async

import org.reactivestreams.{Publisher => RSPublisher, Subscriber, Subscription}

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

trait Publisher[+T] {

  def subscribe(sub: Subscriber[_ >: T]): Unit

}

object Publisher {

  def fromRSPublisher[T](rspub: RSPublisher[T]): Publisher[T] = new Publisher[T] {
    def subscribe(sub: Subscriber[_ >: T]): Unit = {
      rspub.subscribe(sub)
    }
  }

  /** Creates a publisher from an array.
    */
  def fromArray[T](a: Array[T]): Publisher[T] = new Publisher[T] {
    def subscribe(sub: Subscriber[_ >: T]): Unit = {
      a.foreach(elem => sub.onNext(elem))
      sub.onComplete()
    }
  }

  /** Creates a publisher from an iterator.
    */
  def fromIterator[T](iter: Iterator[T]): Publisher[T] = new Publisher[T] {
    def subscribe(sub: Subscriber[_ >: T]): Unit = {
      while (iter.hasNext) {
        sub.onNext(iter.next())
      }
      sub.onComplete()
    }
  }

  def makeIter[T](pub: Publisher[T]): Iterator[T] = {
    val q = new LinkedBlockingQueue[Option[T]](1024)
    val sub = new Subscriber[T] {
      def onNext(t: T): Unit = q.put(Some(t))
      def onError(e: Throwable): Unit = q.put(None)
      def onComplete(): Unit = q.put(None)
      def onSubscribe(s: Subscription): Unit = {
        ??? // TODO
      }
    }
    pub.subscribe(sub)
    new Iterator[T] {
      var last: Option[T] = q.take()
      def hasNext: Boolean = last.nonEmpty
      def next(): T = {
        val reply = last
        if (reply.isEmpty) throw new NoSuchElementException
        else {
          last = q.take()
          reply.get
        }
      }
    }
  }

  /**
    * Essential combinator to enable awaiting the first event emitted
    * by one of two streams.
    */
  def merge[T](p1: Publisher[T], p2: Publisher[T]): Publisher[T] = {
    val subj = new ReplaySubject[T]
    val numDone = new AtomicInteger(0)
    val sub = new Subscriber[T] {
      def onNext(v: T): Unit = {
        subj.emit(v)
      }
      // invoked when either `p1` or `p2` publish an error:
      def onError(t: Throwable): Unit = {
        subj.emitError(t)
      }
      def onComplete(): Unit = {
        if (numDone.incrementAndGet() == 2) {
          subj.emitDone()
        }
      }
      def onSubscribe(s: Subscription): Unit = {
        // TODO
      }
    }
    p1.subscribe(sub)
    p2.subscribe(sub)
    subj
  }

}
