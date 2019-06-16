/*
 * Copyright Philipp Haller
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 */
package com.phaller.async
package test

import org.junit.Test

import scala.concurrent.{Future, Promise, ExecutionContext, Await}
import scala.concurrent.duration._

import Async._
import delegate Async._

import Utils.{delay, delayError}

class SampleSpec {

  delegate for ExecutionContext = ExecutionContext.global

  def nameOfMonth(num: Int): Future[String] = {
    val mon = num match {
      case 1 => "Jan"
      case 2 => "Feb"
      case 3 => "Mar"
      case 4 => "Apr"
      case 5 => "May"
      case 6 => "Jun"
      case 7 => "Jul"
      case 8 => "Aug"
      case 9 => "Sep"
      case 10 => "Oct"
      case 11 => "Nov"
      case 12 => "Dec"
      case _ => "whatever"
    }
    Future.successful(mon)
  }

  @Test def testDates(): Unit = {
    val date = """(\d+)/(\d+)""".r

    val p = Promise[Boolean]()

    val dateStream = rasync {
      val dates = List("01/10", "04/11", "07/12", null, "10/13", "12/14")
      for (date <- dates)
        yieldNext(date)
      yieldDone()
    }

    val stream = rasync[String] {
      var dateOpt = dateStream.await
      while (dateOpt.nonEmpty) {
        dateOpt.get match {
          case date(month, day) =>
            yieldNext(s"It's ${nameOfMonth(month.toInt).await}!")
          case _ =>
            yieldNext("Not a date, mate!")
        }
        dateOpt = dateStream.await
      }
      yieldDone()
    }

    PublisherUtils.collectWhenDone(stream) { (vals: List[String]) =>
      val expected =
        for (ms <- List("Jan", "Apr", "Jul", "", "Oct", "Dec"))
        yield if (ms == "") "Not a date, mate!"
        else s"It's $ms!"

      assert(vals == expected)
      p.success(true)
    }

    Await.ready(p.future, 5.seconds)
  }


}
