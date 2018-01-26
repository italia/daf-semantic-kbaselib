package it.almawave.linkeddata.kb.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature

object JSONHelper {

  private val json_mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)

    .configure(SerializationFeature.CLOSE_CLOSEABLE, true)
    .configure(SerializationFeature.EAGER_SERIALIZER_FETCH, true)
    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
    .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, true)
    .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
    .configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true)
    .configure(SerializationFeature.INDENT_OUTPUT, true)
    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
    .configure(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID, false)

    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  private val json_writer = json_mapper.writerWithDefaultPrettyPrinter()
  private val json_reader = json_mapper.reader()

  def parse(json: String): JsonNode = {
    val content = json.replaceAll("(,)\\s+]", "]") // hack for removing trailing commas (invalid JSON)
    json_reader.readTree(content)
  }

  def writeToString(tree: Any): String = {
    json_writer.writeValueAsString(tree)
  }

  def read(json: String): JsonNode = {
    val content = json.replaceAll("(,)\\s+]", "]") // hack for removing trailing commas (invalid JSON)
    json_reader.readTree(content)
  }

  def pretty(json: String): String = {
    //    val tree = json_reader.readTree(json)
    val tree = read(json)
    json_writer.writeValueAsString(tree)
  }

  // REFACTORIZATION ........................................

  //  def write(obj: Any): String = {
  //    json_writer.writeValueAsString(obj)
  //  }

  //  val render_options = ConfigRenderOptions.concise()
  //    .setJson(true)
  //    .setFormatted(true)

  //  def write(json_tree: JsonNode): String = {
  //    json_writer.writeValueAsString(json_tree)
  //  }

  //  def pretty(json: String): String = {
  //    val tree = read(json)
  //    write(tree)
  //  }

}