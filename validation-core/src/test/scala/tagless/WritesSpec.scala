package jto.validation
package v3.tagless

import org.scalatest._
import org.scalactic.{Equality, Prettifier}

trait WritesSpec[T] extends WordSpec with Matchers {

  type To

  val testCases: TestCases[To]
  val grammar: Grammar[T, types.op[Write]#λ]
  def transform: grammar.Out => To

  implicit def equality: Equality[To] = Equality.default
  implicit def prettifier: Prettifier = Prettifier.default

  import grammar._

  case class Contact(firstname: String,
                     lastname: String,
                     company: Option[String],
                     informations: Seq[ContactInformation])

  case class ContactInformation(
      label: String, email: Option[String], phones: Seq[String])

  val contact = Contact(
      "Julien",
      "Tournay",
      None,
      Seq(
          ContactInformation("Personal",
                             Some("fakecontact@gmail.com"),
                             Seq("01.23.45.67.89", "98.76.54.32.10"))))

  "Writes" should {

    "support primitives types" when {
      "Int" in {
        import testCases.int._
        transform(at(Path \ "n").is(req[Int]).writes(4)) should equal (ok)
        transform(at(Path \ "n" \ "o").is(req[Int]).writes(4)) should equal (noOK)
        transform(at(Path \ "n" \ "o" \ "p").is(req[Int]).writes(4)) should equal (nopOK)
      }

      "Short" in {
        import testCases.int._
        transform(at(Path \ "n").is(req[Short]).writes(4)) should equal (ok)
        transform(at(Path \ "n" \ "o").is(req[Short]).writes(4)) should equal (noOK)
        transform(at(Path \ "n" \ "o" \ "p").is(req[Short]).writes(4)) should equal (nopOK)
      }

      "Long" in {
        import testCases.int._
        transform(at(Path \ "n").is(req[Long]).writes(4)) should equal (ok)
        transform(at(Path \ "n" \ "o").is(req[Long]).writes(4)) should equal (noOK)
        transform(at(Path \ "n" \ "o" \ "p").is(req[Long]).writes(4)) should equal (nopOK)
      }

      "Float" in {
        import testCases.int.{float => f}
        transform(at(Path \ "n").is(req[Float]).writes(4.5f)) should equal (f)
      }

      "Double" in {
        import testCases.int.{float => f}
        transform(at(Path \ "n").is(req[Double]).writes(4.5d)) should equal (f)
      }

      "java BigDecimal" in {
        import java.math.{BigDecimal => JBigDecimal}
        import testCases.int.{bigdecimal => f}
        transform(at(Path \ "n").is(req[JBigDecimal]).writes(new JBigDecimal("4.5"))) should equal (f)
      }

      "scala BigDecimal" in {
        import testCases.int.{bigdecimal => f}
        transform(at(Path \ "n").is(req[BigDecimal]).writes(BigDecimal("4.5"))) should equal (f)
      }

      "Boolean" in {
        import testCases.boolean._
        transform(at(Path \ "n").is(req[Boolean]).writes(true)) should equal (ok)
      }

      "String" in {
        import testCases.string._
        transform(at(Path \ "n").is(req[String]).writes("foo")) should equal (foo)
        transform(at(Path \ "o" \ "n").is(req[String]).writes("foo")) should equal (onFoo)
      }

      "Option" in {
        import testCases.option._
        transform(at(Path \ "foo").is(opt[String]).writes(Option("bar"))) should equal (fooBar)
        transform(at(Path \ "foo").is(opt[String]).writes(None)) should equal (none)
        transform(at(Path \ "foo" \ "bar").is(opt[String]).writes(None)) should equal (none)
      }

      "Map[String, Seq[V]]" in {
        import testCases.map._
        transform(at(Path \ "n").is(req[Map[String, Seq[String]]])
          .writes(Map("foo" -> Seq("bar")))) should equal (foobar)

        transform(at(Path \ "n").is(req[Map[String, Seq[Int]]])
          .writes(Map("foo" -> Seq(4), "bar" -> Seq(5)))) should equal (ints)
      }


      "Seq" in {
        import testCases.seq._
        transform(at(Path \ "n").is(req[Seq[String]]).writes(Seq("foo"))) should equal (foos)
        transform(at(Path \ "foo" \ "foo").is(req[Seq[String]]).writes(Seq("bar"))) should equal (foofoobars)
        transform(at(Path \ "n").is(req[Seq[Int]]).writes(Seq(1, 2, 3))) should equal (ints)
      }

      "List" in {
        import testCases.seq._
        transform(at(Path \ "n").is(req[List[String]]).writes(List("foo"))) should equal (foos)
        transform(at(Path \ "foo" \ "foo").is(req[List[String]]).writes(List("bar"))) should equal (foofoobars)
        transform(at(Path \ "n").is(req[List[Int]]).writes(List(1, 2, 3))) should equal (ints)
      }

      "Array" in {
        import testCases.seq._
        transform(at(Path \ "n").is(req[Array[String]]).writes(Array("foo"))) should equal (foos)
        transform(at(Path \ "foo" \ "foo").is(req[Array[String]]).writes(Array("bar"))) should equal (foofoobars)
        transform(at(Path \ "n").is(req[Array[Int]]).writes(Array(1, 2, 3))) should equal (ints)
      }
    }

    "compose" in {
      // TODO: Add a test of required list that can be empty
      // and validate that it works in XML
      import testCases.base._
      val w =
        (
          at(Path \ "label").is(opt[String]) ~:
          at(Path \ "email").is(opt[String]) ~:
          at(Path \ "phones").is(opt[Seq[String]].contramap[Seq[String]]{ ss => ss.headOption.map(_ => ss) }) ~:
          knil
        )

      import shapeless._

      val v = Some("Personal") :: Some("fakecontact@gmail.com") :: Seq("01.23.45.67.89", "98.76.54.32.10") :: HNil
      transform(w.writes(v)) should equal (testCases.base.info)
      transform(w.writes(None :: None :: Nil :: HNil)) should equal (noInfo)
    }

    // "write Invalid" in {
    //   val f = Invalid[(Path, Seq[ValidationError]), String](Seq(Path \ "n" -> Seq(ValidationError("validation.type-mismatch", "Int"))))

    //   implicitly[Write[(Path, Seq[ValidationError]), JsObject]]
    //   implicitly[Write[Invalid[(Path, Seq[ValidationError]), String], JsObject]]

    //   val error =
    //     Json.obj("errors" ->
    //       Json.obj("/n" -> Json.arr(
    //         Json.obj(
    //           "msg" -> "validation.type-mismatch",
    //           "args" -> Seq("Int")))))

    //   (Path \ "errors").write[Invalid[(Path, Seq[ValidationError]), String], JsObject]
    //     .writes(f) should equal((error))
    // }

    "write Map" in {
      import testCases.base._

      val contactInformation =
        {
          at(Path \ "label").is(req[String]) ~:
          at(Path \ "email").is(opt[String]) ~:
          at(Path \ "phones").is(req[Seq[String]]) ~:
          knil
        }.from[ContactInformation]

      val contactWrite =
        {
          at(Path \ "firstname").is(req[String]) ~:
          at(Path \ "lastname").is(req[String]) ~:
          at(Path \ "company").is(opt[String]) ~:
          at(Path \ "informations").is(req(seq(contactInformation))) ~:
          knil
        }.from[Contact]

      // TODO: use solver ?
      transform(contactWrite.writes(contact)) should equal (jto)
    }

    "write recursive" when {
      case class RecUser(name: String, friends: Seq[RecUser] = Nil)
      val u = RecUser("bob", Seq(RecUser("tom")))

      case class User1(name: String, friend: Option[User1] = None)
      val u1 = User1("bob", Some(User1("tom")))

      "using explicit notation" in {
        import testCases.rec._
        lazy val w: Write[RecUser, Out] =
          {
            at(Path \ "name").is(req[String]) ~:
            at(Path \ "friends").is(opt(seq(w)).contramap{ (ns: Seq[RecUser]) => ns.headOption.map(_ => ns) }) ~:
            knil
          }.from[RecUser]

        transform(w.writes(u)) should equal (bobAndFriends)

        lazy val w2: Write[User1, Out] =
          {
            at(Path \ "name").is(req[String]) ~:
            at(Path \ "friend").is(opt(w2)) ~:
            knil
          }.from[User1]

        transform(w2.writes(u1)) should equal (bobAndFriend)
      }

      "using implicit notation" in {
        import testCases.rec._
        implicit lazy val w: Write[RecUser, Out] =
          {
            at(Path \ "name").is(req[String]) ~:
            at(Path \ "friends").is(opt[Seq[RecUser]].contramap{ (ns: Seq[RecUser]) => ns.headOption.map(_ => ns) }) ~:
            knil
          }.from[RecUser]

        transform(w.writes(u)) should equal (bobAndFriends)

        implicit lazy val w2: Write[User1, Out] =
          {
            at(Path \ "name").is(req[String]) ~:
            at(Path \ "friend").is(opt[User1]) ~:
            knil
          }.from[User1]

        transform(w2.writes(u1)) should equal (bobAndFriend)
      }
    }

    "support write of value class" in {
      import testCases.base._
      import TestValueClass._

      val w = at(Path \ "id").is(Id.writes andThen req[String])
      transform(w.writes(Id("1"))) should equal (id)
    }
  }

}

object TestValueClass {
  case class Id(value: String) extends AnyVal
  object Id {
    implicit val writes: Write[Id, String] = Write(id => id.value)
  }
}