# playjsonsnake

Use SnakeCaseJson to create a play-json Format or individual Reads and Writes with automatic conversion from snake case to camel case and vice versa.

```scala
case class TestCase(camelCase: String)

implicit val snakeCaseFormat = SnakeCaseJson.format(Json.format[TestCase])

val testCase = TestCase("camelCase")
val json = Json.toJson(testCase)
```
