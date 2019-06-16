/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.concurrent.{Flow => JFlow}
import java.util.concurrent.locks.ReentrantLock

import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.util.{Try, Success, Failure}
import scala.collection.mutable.Queue

final class BufferedSubscription[A](obs: Flow[_]) extends DynamicSubscription[A] with JFlow.Subscriber[A] {

  private val lock = new ReentrantLock

  // guarded by `lock`
  private val waiters = Queue.empty[Promise[Option[A]]]

  // guarded by `lock`
  private val emitted = Queue.empty[Try[Option[A]]]

  // guarded by `lock`
  private var enabled = true

  def disable(): Unit = {
    lock.lock()
    enabled = false
    lock.unlock()
  }

  def enable(): Unit = {
    lock.lock()
    enabled = true
    lock.unlock()
  }

  /* If this `BufferedSubscription` is disabled, event `tr` is dropped.
   */
  private def put(tr: Try[Option[A]]): Unit = {
    lock.lock()
    if (enabled) {
      if (waiters.isEmpty) {
        emitted.enqueue(tr)
      } else {
        val p = waiters.dequeue()
        if (tr.isFailure) {
          p.failure(tr.asInstanceOf[Failure[Option[A]]].exception)
        } else {
          p.success(tr.get)
        }
      }
    }
    lock.unlock()
  }

  def onNext(value: A): Unit =
    put(Success(Some(value)))

  def onError(error: Throwable): Unit =
    put(Failure[Option[A]](error))

  def onComplete(): Unit =
    put(Success(None))

  def onSubscribe(s: JFlow.Subscription): Unit = ???

  def onComplete[U](f: Try[Option[A]] => U) given ExecutionContext: Unit = {
    val p = Promise[Option[A]]()
    waiters.enqueue(p)
    p.future.onComplete((tr: Try[Option[A]]) => {
      obs.undropAll()
      f(tr)
    })
    lock.unlock()
  }

  def getCompleted: Try[Option[A]] = {
    lock.lock()
    if (emitted.nonEmpty) {
      val reply: Try[Option[A]] = emitted.dequeue()
      lock.unlock()
      reply
    } else {
      // keep holding lock, release within onComplete which is guaranteed to be called
      null
    }
  }

  // called when events from all other streams should be dropped
  // while waiting for next event on this `BufferedSubscription`.
  def getCompletedOnly: Try[Option[A]] = {
    lock.lock()
    if (emitted.nonEmpty) {
      val reply: Try[Option[A]] = emitted.dequeue()
      lock.unlock()
      reply
    } else {
      // tell `obs` to temporarily drop events from other streams
      obs.dropExcept(this)
      // keep holding lock, release within onComplete which is guaranteed to be called
      null
    }
  }

  def receive(): Future[Option[A]] = {
    lock.lock()
    if (emitted.nonEmpty) {
      val reply = emitted.dequeue()
      lock.unlock()
      Future.successful(reply.get)
    } else {
      val p = Promise[Option[A]]()
      waiters.enqueue(p)
      lock.unlock()
      p.future
    }
  }

}
