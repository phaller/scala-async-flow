/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

class AsyncFuture[A](fut: Future[A]) extends Async[A] {

  def onComplete[U](handler: Try[A] => U) given (executor: ExecutionContext): Unit = {
    fut.onComplete(handler)(executor)
  }

  def getCompleted: Try[A] = {
    if (fut.isCompleted)
      fut.value.get
    else
      null
  }

  def getCompletedOnly: Try[A] = {
    if (fut.isCompleted)
      fut.value.get
    else
      null
  }
}
