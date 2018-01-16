package scala.async.flow

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.async.internal.Awaitable

/* In contrast to other subscriptions, it permits registering new
 * event handlers dynamically.
 */
trait DynamicSubscription[T] extends Awaitable[Option[T]] {
  def onComplete[U](f: Try[Option[T]] => U)(implicit ec: ExecutionContext): Unit
  def getCompleted: Try[Option[T]]
  def getCompletedOnly: Try[Option[T]]
}
