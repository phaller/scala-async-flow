/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async

/* Context should be a public trait which does not expose
   any implementation details.
 */
trait Context[T] {
  def yieldNext(event: T): Unit
  def yieldDone(): Unit
  def yieldError(error: Throwable): Unit
}
