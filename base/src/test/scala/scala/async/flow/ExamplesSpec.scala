package scala.async.flow

import org.scalatest._

import scala.concurrent.{Future, Promise, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.async.internal._
import RAY._
import Awaitable._

import Publisher._
import Flow._

class Examples {

  //def awaitFixedNumber(stream: Publisher[Int], num: Int) = rasync(stream) {
  //def awaitFixedNumber(stream: Publisher[Int], num: Int) = flow[List[Int]](async {
  def awaitFixedNumber(stream: Publisher[Int], num: Int) = Flow[List[Int], Int](stream)(implicit ctx => async {
    var events: List[Int] = List()
    while (events.size < 5) {
      val event = await(stream)
      events = event.get :: events
    }
    events
  })

  //def awaitTermination(stream: Publisher[Int]) = rasync(stream) {
  //def awaitTermination(stream: Publisher[Int]) = flow[List[Int]](async {
  def awaitTermination(stream: Publisher[Int]) = Flow[List[Int], Int](stream)(implicit ctx => async {
    var events: List[Int] = List()
    var next: Option[Int] = await(stream)
    while (next.nonEmpty) {
      events = next.get :: events
      next = await(stream)
    }
    events
  })

  def fwd(s: Publisher[Int]) = Flow[Int, Int](s)(implicit ctx => async {
    var x: Option[Int] = None
    x = await(s)
    x.get
  })

}

class ExamplesSpec extends FlatSpec with Matchers {

  "example 1" should "work" in {
    val p = Promise[Boolean]()

    val stream = PublisherUtils.fromList(List(1, 2, 3, 4, 5, 6, 7))
    val ex = new Examples
    val obs = ex.awaitFixedNumber(stream, 5)

    PublisherUtils.collectWhenDone(obs) { (vals: List[List[Int]]) =>
      vals should be (List(List(5, 4, 3, 2, 1)))
      p.success(true)
    }

    Await.ready(p.future, 10.seconds)
  }

  "example 2" should "work" in {
    val p = Promise[Boolean]()

    val stream = PublisherUtils.fromList(List(1, 2, 3, 4, 5, 6, 7))
    val ex = new Examples
    val obs = ex.awaitTermination(stream)

    PublisherUtils.collectWhenDone(obs) { (vals: List[List[Int]]) =>
      vals should be (List(List(7, 6, 5, 4, 3, 2, 1)))
      p.success(true)
    }

    Await.ready(p.future, 10.seconds)
  }

}
