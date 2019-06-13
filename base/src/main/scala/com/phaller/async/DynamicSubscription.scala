/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

import scala.concurrent.ExecutionContext
import scala.util.Try

/* In contrast to other subscriptions, it permits registering new
 * event handlers dynamically.
 */
trait DynamicSubscription[T] extends Async[Option[T]] {
  def onComplete[U](handler: Try[Option[T]] => U) given (executor: ExecutionContext): Unit
  def getCompleted: Try[Option[T]]
  def getCompletedOnly: Try[Option[T]]
}
