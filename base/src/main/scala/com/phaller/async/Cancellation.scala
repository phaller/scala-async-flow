package com.phaller.async

/** Unforgeable cancellation tag used to cancel asynchronous
  * publishers.
  */
final class CancellationTag private[async] () {
  @volatile
  private var cancelled = false

  def cancel(): Unit = {
    cancelled = true
  }

  def isCancelled: Boolean =
    cancelled
}

class CancellationTagSource {
  def mkTag: CancellationTag =
    new CancellationTag()
}

class CancellationException(val token: CancellationTag) extends RuntimeException
