package playjsonsnake

import play.api.libs.json._

object SnakeCaseJson {
  def  reads[T](fjs:   Reads[T]):   Reads[T] = Reads.of[JsObject] map camelCaseFields andThen fjs
  def writes[T](tjs: OWrites[T]): OWrites[T] = OWrites[T](x => snakeCaseFields(tjs writes x))
  def format[T](f:   OFormat[T]): OFormat[T] = OFormat[T](reads(f), writes(f))

  private def camelCaseFields(json: JsObject) = JsObject(json.fields map (kv => camelCase(kv._1) -> kv._2))
  private def snakeCaseFields(json: JsObject) = JsObject(json.fields map (kv => snakeCase(kv._1) -> kv._2))

  private def camelCase(s: String) =
    (s.split("_").toList match {
      case head :: tail => head :: tail.map(_.capitalize)
      case x            => x
    }).mkString

  private def snakeCase(s: String) =
    s.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) && s.nonEmpty => s append "_" append (Character toLowerCase c)
      case (s, c)                                           => s append c
    }.toString
}
