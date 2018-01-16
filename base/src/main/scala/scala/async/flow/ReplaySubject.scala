package scala.async.flow

import org.reactivestreams.Subscriber

import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable.ListBuffer
import scala.util.{Try, Success, Failure}


/* Threadsafe
 */
class ReplaySubject[T] extends Publisher[T] {

  // Guards all state
  private val lock = new ReentrantLock

  private var subscribers: List[Subscriber[_ >: T]] = List()

  // Buffer of all published events
  private val buffer = new ListBuffer[T]

  // Completion status
  private var status: Option[Try[Boolean]] = None

  /**
    * Immediately start producing for subscriber `s`.
    *
    * The main issue to address here is that we could have concurrent
    * calls to `subscribe` and `emit`.  Each new subscriber needs to
    * first receive the prior global events.  Thus: if `subscribe`
    * acquires a lock before `emit`, then we have to ensure that the
    * prior global events are all emitted to the new subscriber before
    * we take care of the event passed to `emit`.
    */
  def subscribe(s: Subscriber[_ >: T]): Unit = {
    /*
     Approach: emit prior global events while holding `lock`.

     Could locking strategy cause any issues?
     Only if a thread T1 is holding a lock L1, and then T1 calls subscribe
     which acquires lock L2 (of the ReplaySubject), and then the call to
     onNext causes a different thread T2 to try to acquire L2.
     */

    // emit all previous events to `s`.
    // key: handle new concurrent events.
    lock.lock()
    try {
      buffer.foreach(evt => s.onNext(evt))
      status match {
        case Some(Success(_)) => s.onComplete()
        case Some(Failure(t)) => s.onError(t)
        case None => subscribers = s :: subscribers
      }
    } finally {
      lock.unlock()
    }
  }

  /** Emits a new event from `this` publisher to all its `subscribers`.
    * Event `evt` happens after all previously published events.
    */
  def emit(evt: T): Unit = {
    lock.lock()
    try {
      if (status.nonEmpty) {
        // do nothing (already terminated)
      } else {
        // append to `buffer` for later subscribers:
        buffer += evt
        // emit to current subscribers:
        for (s <- subscribers)
          s.onNext(evt)
      }
    } finally {
      lock.unlock()
    }
  }

  def emitDone(): Unit = {
    lock.lock()
    try {
      if (status.nonEmpty) {
        // do nothing (already terminated)
      } else {
        for (s <- subscribers)
          s.onComplete()
        status = Some(Success(true))
      }
    } finally {
      lock.unlock()
    }
  }

  def emitError(t: Throwable): Unit = {
    lock.lock()
    try {
      if (status.nonEmpty) {
        // do nothing (already terminated)
      } else {
        for (s <- subscribers)
          s.onError(t)
        status = Some(Failure(t))
      }
    } finally {
      lock.unlock()
    }
  }

  /* Old implementation of `subscribe`:
   * Subscribers do not receive any events until `this` publisher is done.
   */
  def subscribe1(o: Subscriber[_ >: T]): Unit = {
    var events: List[T] = null
    lock.lock()
    try {
      if (status.nonEmpty) {
        events = buffer.toList
      } else {
        subscribers = o :: subscribers
      }
    } finally {
      lock.unlock()
    }
    if (events != null) {
      events.foreach(evt => o.onNext(evt))
      o.onComplete()
    }
  }

}
