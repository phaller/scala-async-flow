package com.phaller.async

import scala.language.implicitConversions

import org.reactivestreams.Subscriber

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.concurrent.TrieMap
import scala.util.{Try, Success, Failure}

object Flow {

  private val subOf: TrieMap[(Context[_], Publisher[_]), DynamicSubscription[_]] =
    TrieMap.empty[(Context[_], Publisher[_]), DynamicSubscription[_]]

  implicit def pubToSub[T, S](pub: Publisher[T])(implicit ctx: Context[S]): DynamicSubscription[T] =
    subOf.get((ctx, pub)) match {
      case Some(sub) =>
        // `ctx` already subscribed to `pub`, `sub` is subscription
        sub.asInstanceOf[DynamicSubscription[T]]
      case None =>
        subscribeInternal(pub)
    }

  private def subscribeInternal[T, S](pub: Publisher[T])(implicit ctx: Context[S]): DynamicSubscription[T] = {
    val s = ctx.subscribe(pub)
    subOf.put((ctx, pub), s)
    s
  }

  def apply[T, S](pubs: Publisher[S]*)(body: Context[T] => Future[T]): Flow[T] = {
    val flow = new Flow[T]
    // `flow` is `Context[T]`
    for (pub <- pubs)
      subscribeInternal(pub)(flow)
    flow.init(body)
    flow
  }

  def flow[T](body: => Future[T]): Flow[T] = {
    val flow = new Flow[T]
    flow.init((r: Context[T]) => {
      Context.r = r
      body
    })
    flow
  }

  def subscribe[T](pub: Publisher[T]): DynamicSubscription[T] = {
    val r = Context.r.asInstanceOf[Context[T]]
    r.subscribe(pub)
  }

  def yieldNext[T](t: T): Unit = {
    val r = Context.r.asInstanceOf[Context[T]]
    r.yieldNext(t)
  }

  def yieldDone(): Unit = {
    val r = Context.r.asInstanceOf[Context[Nothing]]
    r.yieldDone()
  }

}

class Flow[T](tag: Option[CancellationTag] = None) extends Publisher[T] with Context[T] {

  private var body: Context[T] => Future[T] = _
  private var observers = List[Subscriber[_ >: T]]()
  private var complete = false
  private var bufferOf: Map[Publisher[_], BufferedSubscription[_]] = Map()

  def subscribe(sub: Subscriber[_ >: T]): Unit = synchronized {
    observers = sub :: observers

    // only start future task when first subscriber registers
    if (observers.size == 1)
      body(this).onComplete {
        case Success(v) =>
          val done = this.synchronized { complete }
          if (!done) {
            emitNext(v)
            emitComplete()
          }
        case Failure(e) =>
          emitError(e)
      }
  }

  def yieldNext(evt: T): Unit = {
    if (isCancelled)
      emitError(new CancellationException(tag.get))
    else
      emitNext(evt)
  }

  def yieldDone(): Unit = {
    if (isCancelled)
      emitError(new CancellationException(tag.get))
    else
      emitComplete()
  }

  def subscribe[S](pub: Publisher[S]): DynamicSubscription[S] = {
    // pass `this` to `Buffer`, needed for `getCompletedOnly`
    val buf = new BufferedSubscription[S](this)
    bufferOf = bufferOf + (pub -> buf)
    pub.subscribe(buf)
    buf
  }

  def isCancelled: Boolean =
    tag.nonEmpty && tag.get.isCancelled

  private[async] def init(f: Context[T] => Future[T]): Unit = synchronized {
    // do not even start if already cancelled
    // TODO: add test
    if (isCancelled)
      emitError(new CancellationException(tag.get))

    body = f
  }

  private[async] def dropExcept(buf: BufferedSubscription[_]): Unit = {
    for (b <- bufferOf.values if b != buf)
      b.disable()
  }

  private[async] def undropAll(): Unit = {
    for (b <- bufferOf.values)
      b.enable()
  }

  private def emitNext(t: T): Unit = synchronized {
    observers.foreach(o => o.onNext(t))
  }

  private def emitComplete(): Unit = synchronized {
    complete = true
    observers.foreach(o => o.onComplete())
  }

  private def emitError(e: Throwable): Unit = synchronized {
    observers.foreach(o => o.onError(e))
  }

}
