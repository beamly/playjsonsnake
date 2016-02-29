# [playjsonsnake][] [![Apache 2 badge][]](http://www.apache.org/licenses/LICENSE-2.0)

`playjsonsnake` is a micro-library that adds snake case support to `play-json`, the JSON library in [Play][].

## Setup

Add this to your sbt build definitions, such as in `build.sbt`:

```scala
libraryDependencies += "com.beamly" %% "playjsonsnake" % "1.0.1"
```

For other build systems see the dependency information on Maven Central:

* For Scala 2.11: http://search.maven.org/#artifactdetails|com.beamly|playjsonsnake_2.11|1.0.1|jar
* For Scala 2.10: http://search.maven.org/#artifactdetails|com.beamly|playjsonsnake_2.10|1.0.1|jar

## How to Use

Wrap `play-json`'s `OFormat` (such as one returned by `Json.format`) with `SnakeCaseJson.format`.

The returned `OFormat` will serialise to and deserialise from snake case JSON field names:

```scala
scala> :pa
// Entering paste mode (ctrl-D to finish)

import playjsonsnake._
import play.api.libs.json._

final case class Foo(i: Int, abcDefGhi: Int)
object Foo {
  implicit val jsonFormat: OFormat[Foo] = SnakeCaseJson.format(Json.format[Foo])
}

// Exiting paste mode, now interpreting.

import playjsonsnake._
import play.api.libs.json._
defined class Foo
defined object Foo

scala> Json toJson Foo(123, 456)
res0: play.api.libs.json.JsValue = {"i":123,"abc_def_ghi":456}

scala> Json.fromJson[Foo](Json parse """{ "i" : 123, "abc_def_ghi" : 456 }""")
res1: play.api.libs.json.JsResult[Foo] = JsSuccess(Foo(123,456),)
```

There are also wrappers for `Reads` and `OWrites`, in the form of `SnakeCaseJson.reads` and `SnakeCaseJson.
writes`.

## Dependencies

* Scala 2.11.x or 2.10.x
* play-json 2.4.x

## Licence

Copyright 2016 Beamly

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[playjsonsnake]: https://github.com/beamly/playjsonsnake
[Apache 2 badge]: http://img.shields.io/:license-Apache%202-red.svg
[Play]: https://www.playframework.com/
