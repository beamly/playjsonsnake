package playjsonsnake

import scala.language.experimental.macros

import play.api.libs.json._

object SnakeCaseJson {
  @deprecated("Use version that changes casing at compile time", "1.1.0")
  def  reads[T](fjs:   Reads[T]):   Reads[T] = Reads.of[JsObject] map camelCaseFields andThen fjs
  @deprecated("Use version that changes casing at compile time", "1.1.0")
  def writes[T](tjs: OWrites[T]): OWrites[T] = OWrites[T](x => snakeCaseFields(tjs writes x))
  @deprecated("Use version that changes casing at compile time", "1.1.0")
  def format[T](f:   OFormat[T]): OFormat[T] = OFormat[T](reads(f), writes(f))

  def  reads[T]:   Reads[T] = macro SnakeCaseJsonMacros.readsImpl[T]
  def writes[T]: OWrites[T] = macro SnakeCaseJsonMacros.writesImpl[T]
  def format[T]: OFormat[T] = macro SnakeCaseJsonMacros.formatImpl[T]

  private def camelCaseFields(json: JsObject) = JsObject(json.fields map (kv => camelCase(kv._1) -> kv._2))
  private def snakeCaseFields(json: JsObject) = JsObject(json.fields map (kv => snakeCase(kv._1) -> kv._2))

  private[playjsonsnake] def camelCase(s: String) =
    (s.split("_").toList match {
      case head :: tail => head :: tail.map(_.capitalize)
      case x            => x
    }).mkString

  private[playjsonsnake] def snakeCase(s: String) =
    s.foldLeft(new StringBuilder) {
      case (s, c) if Character.isUpperCase(c) && s.nonEmpty => s append "_" append (Character toLowerCase c)
      case (s, c)                                           => s append c
    }.toString
}
