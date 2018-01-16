package scala.async.flow

trait Context[T] {
  def yieldNext(evt: T): Unit
  def yieldDone(): Unit
  def subscribe[S](pub: Publisher[S]): DynamicSubscription[S]
  def isCancelled: Boolean
}

object Context {
  @volatile
  var r: AnyRef = _
}
