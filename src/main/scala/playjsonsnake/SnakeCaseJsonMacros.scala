package playjsonsnake

import play.api.libs.json._

import scala.reflect.macros.blackbox

@macrocompat.bundle final class SnakeCaseJsonMacros(val c: blackbox.Context) {
  // Adapted from play.api.libs.json.JsMacroImpl

  import c.universe._

  def readsImpl[A: WeakTypeTag]: Expr[Reads[A]] =
    macroImpl[A, Reads, Reads]("read", "map", reads = true, writes = false)

  def writesImpl[A: WeakTypeTag]: Expr[OWrites[A]] =
    macroImpl[A, OWrites, Writes]("write", "contramap", reads = false, writes = true)

  def formatImpl[A: WeakTypeTag]: Expr[OFormat[A]] =
    macroImpl[A, OFormat, Format]("format", "inmap", reads = true, writes = true)

  def macroImpl[A, M[_], N[_]](
    methodName: String, mapLikeMethod: String, reads: Boolean, writes: Boolean
  )(implicit
    atag: WeakTypeTag[A], matag: WeakTypeTag[M[A]], natag: WeakTypeTag[N[A]]
  ): Expr[M[A]] = {

    import c.universe.definitions.TupleClass

    def abort(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

    val companion = weakTypeOf[A].typeSymbol
    val companionObject = companion.companion
    val companionType = companionObject.typeSignature

    def conditionalList[T](ifReads: T, ifWrites: T): List[T] =
      (if (reads) List(ifReads) else Nil) ::: (if (writes) List(ifWrites) else Nil)

    val     syntax = q"_root_.play.api.libs.functional.syntax"
    val     unlift = q"_root_.play.api.libs.functional.syntax.unlift"
    val     JsPath = q"_root_.play.api.libs.json.JsPath"
    val      Reads = q"_root_.play.api.libs.json.Reads"
    val     Writes = q"_root_.play.api.libs.json.Writes"
    val LazyHelper = q"_root_.play.api.libs.json.util.LazyHelper"

    val    unapply = companionType.decl(TermName("unapply"))
    val unapplySeq = companionType.decl(TermName("unapplySeq"))
    val hasVarArgs = unapplySeq != NoSymbol

    val effectiveUnapply = Seq(unapply, unapplySeq).find(_ != NoSymbol) match {
      case None    => abort("No unapply or unapplySeq function found")
      case Some(s) => s.asMethod
    }

    val unapplyReturnTypes: Option[List[Type]] = effectiveUnapply.returnType match {
      case TypeRef(_, _, Nil) =>
        abort(s"Unapply of $companionObject has no parameters. Are you using an empty case class?")

      case TypeRef(_, _, args) =>
        args.head match {
          case t @ TypeRef(_, _, Nil)  => Some(List(t))
          case t @ TypeRef(_, _, args) =>
            if (!TupleClass.seq.exists(tupleSym => t.baseType(tupleSym) ne NoType)) Some(List(t))
            else if (t <:< typeOf[Product]) Some(args)
            else None
          case _ => None
        }

      case _ => None
    }

    val applies =
      companionType.decl(TermName("apply")) match {
        case NoSymbol => abort("No apply function found")
        case s        => s.asTerm.alternatives
      }

    // searches apply method corresponding to unapply
    val apply = applies.collectFirst {
      case apply: MethodSymbol if hasVarArgs && {
        val someApplyTypes = apply.paramLists.headOption.map(_.map(_.asTerm.typeSignature))
        val someInitApply = someApplyTypes.map(_.init)
        val someApplyLast = someApplyTypes.map(_.last)
        val someInitUnapply = unapplyReturnTypes.map(_.init)
        val someUnapplyLast = unapplyReturnTypes.map(_.last)
        val initsMatch = someInitApply == someInitUnapply
        val lastMatch = (for {
          lastApply <- someApplyLast
          lastUnapply <- someUnapplyLast
        } yield lastApply <:< lastUnapply).getOrElse(false)
        initsMatch && lastMatch
      } => apply
      case apply: MethodSymbol
        if apply.paramLists.headOption.map(_.map(_.asTerm.typeSignature)) == unapplyReturnTypes => apply
    }

    val params = apply match {
      case Some(apply) => apply.paramLists.head // verify there is a single parameter group
      case None        => abort("No apply function found matching unapply parameters")
    }

    final case class Implicit(
      paramName: Name, paramType: Type, neededImplicit: Tree, isRecursive: Boolean, tpe: Type)

    val createImplicit = { (name: Name, implType: c.universe.type#Type) =>
      val (isRecursive, tpe) = implType match {
        case TypeRef(_, t, args) =>
          val isRec = args.exists(_.typeSymbol == companion)
          // Option[_] needs special treatment because we need to use XXXOpt
          val tp = if (implType.typeConstructor <:< typeOf[Option[_]].typeConstructor) args.head else implType
          (isRec, tp)
        case TypeRef(_, t, _) =>
          (false, implType)
      }

      // builds M implicit from expected type
      val neededImplicitType = appliedType(natag.tpe.typeConstructor, tpe :: Nil)
      // infers implicit
      val neededImplicit = c.inferImplicitValue(neededImplicitType)
      Implicit(name, implType, neededImplicit, isRecursive, tpe)
    }

    val applyParamImplicits = params.map(param => createImplicit(param.name, param.typeSignature))

    val effectiveInferredImplicits =
      if (hasVarArgs) {
        val varArgsImplicit = createImplicit(applyParamImplicits.last.paramName, unapplyReturnTypes.get.last)
        applyParamImplicits.init :+ varArgsImplicit
      } else
        applyParamImplicits

    val missingImplicits =
      effectiveInferredImplicits.collect { case Implicit(_, t, impl, rec, _) if impl == EmptyTree && !rec => t }

    if (missingImplicits.nonEmpty)
      abort(s"No implicit format for ${missingImplicits.mkString(", ")} available.")

    var hasRec = false

    val         method = TermName(methodName)
    val nullableMethod = TermName(s"${methodName}Nullable")
    val     lazyMethod = TermName(s"lazy${methodName.capitalize}")

    // combines all reads into CanBuildX
    val canBuild = effectiveInferredImplicits.map {
      case Implicit(name, t, impl, rec, tpe) =>
        val jspathTree = q"""$JsPath \ ${SnakeCaseJson.snakeCase(name.decodedName.toString)}"""
        if (!rec) {
          val callMethod =
            if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor) nullableMethod else method
          q"$jspathTree.$callMethod($impl)"
        } else {
          hasRec = true

          def readsWritesHelper(methodName: String): List[Tree] =
            conditionalList(Reads, Writes).map(s => q"$s.${TermName(methodName)}(this.lazyStuff)")

          if (t.typeConstructor <:< typeOf[Option[_]].typeConstructor)
            q"$jspathTree.$nullableMethod($JsPath.$lazyMethod(this.lazyStuff))"
          else {
            val arg =
              if (tpe.typeConstructor <:< typeOf[List[_]].typeConstructor)
                readsWritesHelper("list")
              else if (tpe.typeConstructor <:< typeOf[Set[_]].typeConstructor)
                readsWritesHelper("set")
              else if (tpe.typeConstructor <:< typeOf[Seq[_]].typeConstructor)
                readsWritesHelper("seq")
              else if (tpe.typeConstructor <:< typeOf[Map[_, _]].typeConstructor)
                readsWritesHelper("map")
              else List(q"this.lazyStuff")
            q"$jspathTree.$lazyMethod(..$arg)"
          }
        }
    }.reduceLeft { (acc, r) =>
      q"$acc.and($r)"
    }

    val applyFunction = {
      if (hasVarArgs) {

        val applyParams = params.foldLeft(List[Tree]())((l, e) =>
          l :+ Ident(TermName(e.name.encodedName.toString))
        )
        val vals = params.foldLeft(List[Tree]())((l, e) =>
          // Let type inference infer the type by using the empty type, TypeTree()
          l :+ q"val ${TermName(e.name.encodedName.toString)}: ${TypeTree()}"
        )

        q"(..$vals) => $companionObject.apply(..${applyParams.init}, ${applyParams.last}: _*)"
      } else {
        q"$companionObject.apply _"
      }
    }

    val unapplyFunction = q"$unlift($companionObject.$effectiveUnapply)"

    // if case class has one single field, needs to use inmap instead of canbuild.apply
    val applyOrMap = TermName(if (params.length > 1) "apply" else mapLikeMethod)
    val finalTree = q"""
      import $syntax._
      $canBuild.$applyOrMap(..${conditionalList(applyFunction, unapplyFunction)})
    """

    val lazyFinalTree = if (!hasRec) {
      finalTree
    } else {
      q"""
        new $LazyHelper[${matag.tpe.typeSymbol}, ${atag.tpe.typeSymbol}] {
          override lazy val lazyStuff: ${matag.tpe.typeSymbol}[${atag.tpe}] = $finalTree
        }.lazyStuff
      """
    }

    c.Expr[M[A]](lazyFinalTree)
  }
}
