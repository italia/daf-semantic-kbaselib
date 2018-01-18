package utilities

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.config.Config
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigRenderOptions

object JSONHelper {

  val render_options = ConfigRenderOptions.concise()
    .setJson(true)
    .setFormatted(true)

  private val json_mapper = new ObjectMapper
  json_mapper.registerModule(DefaultScalaModule)
  private val json_writer = json_mapper.writerWithDefaultPrettyPrinter()
  private val json_reader = json_mapper.reader()

  def read(json: String): JsonNode = {
    val content = json.replaceAll("(,)\\s+]", "]") // hack for removing trailing commas (invalid JSON)
    json_reader.readTree(content)
  }

  def write(obj: Any): String = {
    json_writer.writeValueAsString(obj)
  }

  def write(json_tree: JsonNode): String = {
    json_writer.writeValueAsString(json_tree)
  }

  def pretty(json: String): String = {
    val tree = read(json)
    write(tree)
  }

}