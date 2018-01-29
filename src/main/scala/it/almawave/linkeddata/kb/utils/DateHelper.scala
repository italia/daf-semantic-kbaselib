package it.almawave.linkeddata.kb.utils

import java.text.SimpleDateFormat
import java.util.Locale
import scala.util.Try
import java.util.Date

object DateHelper {

  def format(date: Date) = {
    if (date != null)
      new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(date)
    else
      null
  }

  def parseDate(source: String) = {

    val sdfs = List(
      new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
      new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY),
      new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
      new SimpleDateFormat("dd MMMM yyyy", Locale.ITALY),
      new SimpleDateFormat("MMMM dd yyyy", Locale.ENGLISH),
      new SimpleDateFormat("MMMM dd yyyy", Locale.ITALY))

    sdfs.map { sdf => Try { sdf.parse(source) } }
      .filterNot(_.isFailure)
      .map { d => d.get.asInstanceOf[Date] }
      .headOption.orNull

  }

}

object TestingDateParsing extends App {

  //  val source = "December 18 2017"
  val source = "12 Novembre 2017"
  val dates = DateHelper.parseDate(source)
  println(dates)

}