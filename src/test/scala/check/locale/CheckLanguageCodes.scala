package check.locale

import java.util.Locale

/**
 * checking conversions between different standard language code
 */
object CheckLanguageCodes extends App {

  val loc1 = new Locale.Builder().setLanguage("it").build()

  println("LANG_2: " + loc1.getLanguage)
  println("LANG_3: " + loc1.getISO3Language)

  val loc2 = new Locale.Builder().setLanguage("ita").build()
  println("LANG_2: " + loc2.getLanguage)
  println("LANG_3: " + loc2.getISO3Language)

  val loc3 = LocaleHelper.toISO6391("ITA")
  println("ISO 2 : " + loc3)

  val loc4 = LocaleHelper.toISO6392T("IT")
  println("ISO 3 : " + loc4)

}

object LocaleHelper {

  // ISO-639-1 (2 characters)
  def toISO6391(lang: String) = {
    val locale = new Locale.Builder().setLanguage(lang).build()
    locale.getLanguage.substring(0, 2)
  }

  // ISO-639-2/T (3 characters)
  def toISO6392T(lang: String) = {
    val locale = new Locale.Builder().setLanguage(lang.toLowerCase()).build()
    locale.getISO3Language
  }

}