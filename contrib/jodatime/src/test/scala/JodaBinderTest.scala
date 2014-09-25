package org.w3.banana.contrib.jodatime

import org.w3.banana._, binder._, diesel._
import org.scalatest._
import org.joda.time.DateTime
import JodaImplicits._

abstract class JodaBinderTest[Rdf <: RDF](implicit ops: RDFOps[Rdf])
extends WordSpec with Matchers {

  import ops._

  "serializing and deserialiazing Joda DateTime" in {
    val dateTime = DateTime.now()
    dateTime.toPG.as[DateTime].get.compareTo(dateTime) should be(0)
  }

}

import org.w3.banana.jena.Jena

class JodaBinderTestWithJena extends JodaBinderTest[Jena]
