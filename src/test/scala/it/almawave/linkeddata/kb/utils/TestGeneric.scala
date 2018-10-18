package it.almawave.linkeddata.kb.utils

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import it.almawave.linkeddata.kb.catalog.models.URIWithValue
import org.junit.Test

import java.net.URL

object TestGeneric extends App {


  var s:String ="2013/08/22"
  var simpleDateFormat:SimpleDateFormat = new SimpleDateFormat("yyyy/mm/dd");
  var date:Date = simpleDateFormat.parse(s);
  val ans = new SimpleDateFormat("dd/mm/yyyy").format(date)
//  println(ans)

//  println(this.format(new Date("2013/08/22")))

//  this.strregexversion("Current available 34850938509 version of ISTAT's official classification on legal status of organizations")

//  this.testUriWithValue()

  this.uriDepurata()

  def uriDepurata(): Unit = {

    val namespace1 = "https://w3id.org/italia/onto/CPV/"
    val namespace2 = "https://w3id.org/italia/onto/CPV"
    var _namespace = namespace1.substring(0, namespace1.lastIndexOf("/"))
    println("prima: " + namespace1)
    println("dopo : " + _namespace)
    println("##########################################################")
    println("namespace1 the last char : " + namespace1.charAt(namespace1.length-1))
    println("namespace1 check the last char: " + namespace1.charAt(namespace1.length-1).toString.equals("/"))
    println("namespace2 the last char : " + namespace2.charAt(namespace2.length-1))
    println("namespace2 check the last char: " + namespace2.charAt(namespace2.length-1).equals("/"))

  }

  def strregexversion(str: String) = {

    val numPattern0 = "^(.*?)\\s+-\\s+(.*?)\\s+-\\s+(.*?)$".r
    val numPattern1 = "^(0|[1-9][0-9]*)$".r
    val numPattern2 = "[0-9]+".r
    val numPattern3 = "^(0|[1-9][0-9]*|[1-9][0-9]{0,2}(,[0-9]{3,3})*)$".r
    val address = "Current available version of ISTAT's official classification on legal status of organizations"
//    val address = "1.1"

    val match0 = numPattern0.findFirstIn(address)
    println("regex_0: " + match0)

    val match1 = numPattern1.findFirstIn(address)
    println("regex_1: " + match1)

    val match2 = numPattern2.findFirstIn(address)
    println("regex_2: " + match2.nonEmpty)

    val match3 = numPattern3.findFirstIn(address)
    println("regex_3: " + match3)


//    var result = numPattern0(address) match {
//
//      case (g1, g2, g3) => g1
//        print((g1, g2, g3))
//    }



//    val pattern = "Scala".r
//    val str = "Scala is Scalable and cool"
//
//    println(pattern findFirstIn str)



//    val _vv = "Current available version of - ISTAT's official classification - on legal status of organizations"
//    val _matcher = "^.*?(\\d+\\.\\d+(\\.\\d+)*).*\\s+-\\s+(.*)\\s+-\\s+(.*)$"
//    val number = _vv.replaceAll(_matcher, "$1")
//    print(number)
//
//    val _s = "Current available version of ISTAT's official classification on legal status of organizations@en"




  }


  def format(date: Date) = {
    if (date != null)
      new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(date)
    else
      null
  }

  def testUriWithValue() = {

    val uri = "http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE"
    val value = uri.substring(uri.lastIndexOf("/")+1)

    val uriModel: URIWithValue = URIWithValue(value, uri)
    println(uriModel.value)
    println(uriModel.uri)
  }


}
