package it.almawave.linkeddata.kb.catalog.models

object TestingModels extends App {

  val version = Version("0.0.1", "2018-01-31", "en", "testing...", "testing://testing")
  println(version)

  val ok = getCCParams(version)
  println(ok)

  def getCCParams(cc: AnyRef) = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }

}