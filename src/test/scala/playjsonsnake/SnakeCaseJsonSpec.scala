package playjsonsnake

import org.specs2.mutable.Specification
import play.api.libs.json.{ OFormat, Json }

class SnakeCaseJsonSpec extends Specification {

  val testCase = TestCase("lowercase", "camelCase", "PascalCase", "camelCaseWithSeveralHumps", "kebab-case")
  val testJson = Json parse
    """
      |{
      |  "lowercase": "lowercase",
      |  "camel_case": "camelCase",
      |  "Pascal_case": "PascalCase",
      |  "camel_case_with_several_humps": "camelCaseWithSeveralHumps",
      |  "kebab-case": "kebab-case"
      |}
    """.stripMargin

  implicit val snakeCaseFormat: OFormat[TestCase] = SnakeCaseJson.format[TestCase]

  "SnakeCaseJson format" should {
    "convert camel case to snake case when writing Json" in {
      val json = Json.toJson(testCase)
      json should_=== testJson
    }
    "convert snake case to camel case when reading Json" in {
      val test = testJson.as[TestCase]
      test must_=== testCase
    }
  }

}

case class TestCase(lowercase: String,
                    camelCase: String,
                    PascalCase: String,
                    camelCaseWithSeveralHumps: String,
                    `kebab-case`: String)
