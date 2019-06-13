/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.concurrent.Flow
import java.util.concurrent.atomic.AtomicReference

class PublisherImpl[T] extends Flow.Publisher[T] with Context[T] {

  private var cont: Continuation = _
  private var scope: ContinuationScope = _

  private var subscribers =
    new AtomicReference[List[Flow.Subscriber[_ >: T]]](List())

  private var bufferOf: Map[Flow.Publisher[_], BufferedSubscription[_]] = Map()

  private[async] def init(c: Continuation, s: ContinuationScope): Unit = synchronized {
    cont = c
    scope = s
  }

  private[async] def subscribe(sub: Flow.Subscriber[_ >: T]): Unit = {
    var subs = subscribers.get()
    // have we seen the first subscriber?
    var firstSub = subs.isEmpty
    var successful = subscribers.compareAndSet(subs, sub :: subs)
    while (!successful) {
      subs = subscribers.get()
      firstSub = subs.isEmpty
      successful = subscribers.compareAndSet(subs, sub :: subs)
    }
    if (firstSub) {
      cont.run()
    }
  }

  def yieldNext(event: T): Unit = {
    emitNext(event)
  }

  def yieldDone(): Unit = {
    emitComplete()
  }

  def yieldError(error: Throwable): Unit = {
    emitError(error)
  }

  private[async] def subscribeTo[S](pub: Flow.Publisher[S]): DynamicSubscription[S] = {
    // pass `this` to `BufferedSubscription`, needed for `getCompletedOnly`
    val buf = new BufferedSubscription[S](this)
    bufferOf = bufferOf + (pub -> buf)
    pub.subscribe(buf)
    buf
  }

  private[async] def getOrSubscribeTo[S](pub: Flow.Publisher[S]): DynamicSubscription[S] =
    bufferOf.get(pub) match {
      case Some(sub) =>
        sub.asInstanceOf[DynamicSubscription[S]]
      case None =>
        subscribeTo(pub)
    }

  private[async] def resume(): Unit = {
    cont.run()
  }

  private[async] def suspend(): Unit = {
    Continuation.`yield`(scope)
  }

  private[async] def emitNext(item: T): Unit = {
    val subs = subscribers.get()
    subs.foreach(sub => sub.onNext(item))
  }

  private[async] def emitComplete(): Unit = {
    val subs = subscribers.get()
    subs.foreach(sub => sub.onComplete())
  }

  private[async] def emitError(error: Throwable): Unit = {
    val subs = subscribers.get()
    subs.foreach(sub => sub.onError(error))
  }

  private[async] def dropExcept(buf: BufferedSubscription[_]): Unit = {
    for (b <- bufferOf.values if b != buf)
      b.disable()
  }

  private[async] def undropAll(): Unit = {
    for (b <- bufferOf.values)
      b.enable()
  }

}
