package de.qaware.findfacts.common.solr

import scala.collection.JavaConverters._

import de.qaware.findfacts.common.dt.{BaseEt, BlockEt, ConstantEt, DocKind, DocumentationEt, FactEt, TypeEt}
import de.qaware.findfacts.common.solr.mapper.{FromSolrDoc, ToSolrDoc}
import org.apache.solr.client.solrj.SolrQuery
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class SolrMappingIT extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  final val solrClient = ITSolr.apply().solrConnection()

  override def beforeEach(): Unit = {
    solrClient.deleteByQuery("*:*")
    val status = solrClient.commit()
    status.getStatus should (be(200) or be(0))
  }

  override def afterAll(): Unit = solrClient.close()

  test("Writing to and reading from solr") {
    val const = new ConstantEt("const", "const _ = ...", List("Hol.equiv", "Hol.inJ"), List("Nat"), "'a => Nat")
    val doc = new DocumentationEt("file", 6, 7, "(* comment *)", DocKind.Meta)
    val fact = new FactEt("const_is_pos", "const _ > 0", List("Const.const"), List("Fact.Hol.Stuff", "Fact.Hol.other"))
    val typ = new TypeEt("Nat", "Nat = 0 | Suc Nat", List("Nat"))
    val block = new BlockEt("file", 1, 6, "src text").copy(entities = List(const, fact))
    val block1 = new BlockEt("file1", 2, 5, "other src text").copy(entities = List(typ))

    val toMapper = ToSolrDoc[BaseEt]
    val fromMapper = FromSolrDoc[BaseEt]
    val docs = List(block, block1, doc).map(toMapper.toSolrDoc)

    // Add docs
    solrClient.add(docs.asJava)
    solrClient.commit()

    // Read docs from solrClient
    val resp = solrClient.query(new SolrQuery("*:*"))
    val resultDocs = resp.getResults.asScala.toList
    val result = resultDocs.map(fromMapper.fromSolrDoc).map(_.get)

    // Child docs are empty since *:* query does not join
    result should have size 6
    result should contain allElementsOf (List(const, doc, fact, typ))
    result.collect { case c: BlockEt => c } map (_.id) should contain theSameElementsAs List(block, block1).map(_.id)
  }
}
