/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import java.util.{Timer, TimerTask}

import scala.concurrent.{Future, Promise}

object Utils {

  private val timer = new Timer(true)

  def delay[T](millis: Long, value: T): Future[T] = {
    val promise = Promise[T]()
    timer.schedule(new TimerTask {
      def run(): Unit = promise.success(value)
    }, millis)
    promise.future
  }

  def delayError[T](millis: Long, e: Exception): Future[T] = {
    val promise = Promise[T]()
    timer.schedule(new TimerTask {
      def run(): Unit = promise.failure(e)
    }, millis)
    promise.future
  }

}
