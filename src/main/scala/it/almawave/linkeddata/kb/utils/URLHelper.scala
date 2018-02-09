package it.almawave.linkeddata.kb.utils

import scala.util.Try
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLDecoder

object URLHelper {

  def follow_url(url: URL): Try[URL] = {

    val scheme = url.toURI().getScheme

    if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {

      val conn = url.openConnection().asInstanceOf[HttpURLConnection]

      conn.setConnectTimeout(15000)
      conn.setReadTimeout(15000)
      conn.setInstanceFollowRedirects(false) // Make the logic below easier to detect redirections
      conn.setRequestProperty("User-Agent", "testing...")

      val next: Try[URL] = conn.getResponseCode() match {

        case HttpURLConnection.HTTP_MOVED_TEMP | HttpURLConnection.HTTP_MOVED_PERM =>
          val location = URLDecoder.decode(conn.getHeaderField("Location"), "UTF-8")
          val next_url = new URL(location).toExternalForm()

          //          println("\nLOCATION TEMP " + location)
          //          println(s"equals? ${url} ? ${next_url}", url.equals(next_url))

          if (url.equals(next_url)) {
            //            println("still using url: " + url)
            Try { url }
          } else {
            //            println("now using url: " + next_url)
            follow_url(new URL(next_url))
          }

        case code => Try {
          //          println("CODE? " + code)
          url
        }

      }

      //    println(" NEXT URL : " + next)

      next
    } else if (scheme.equalsIgnoreCase("file")) {
      Try { url }
    } else {
      throw new RuntimeException("unrecognized redirect")
    }
  }

}