package de.qaware.common.solr.dt

import de.qaware.common.solr.dt.SolrSchema._
import org.apache.solr.client.solrj.beans.Field

import scala.annotation.meta.field

// scalastyle:off
// Justification: solrj bean mapping requires no-args c-tors

/** Entity class for type definitions.
  *
  * @param id solr doc id
  * @param sourceFile relative path to file containing entity
  * @param startPos token in src at which entity begins
  * @param endPos token in src at which entity ends
  * @param kind kind of the entity, i.e. [[EntityKind.Type]] for this entity
  * @param typeName name of the defined type
  * @param constructors ids of constructors this type has
  */
@SuppressWarnings(Array("NullParameter")) // Justification: Entity classes are mapped into solr document
final case class TypeEntity(
    @(Field @field)(ID) id: String,
    @(Field @field)(SOURCE_FILE) sourceFile: String,
    @(Field @field)(START_POS) startPos: Int,
    @(Field @field)(END_POS) endPos: Int,
    @(Field @field)(NAME) typeName: String,
    @(Field @field)(USES) constructors: Array[String],
    @(Field @field)(KIND) kind: String = EntityKind.Type
) {
  def this() {
    this(null, null, -1, -1, null, null)
  }
}

/** Entity class for constants.
  *
  * @param id solr doc id
  * @param sourceFile relative path to file containing entity
  * @param startPos token in src at which entity begins
  * @param endPos token in src at which entity ends
  * @param kind kind of the entity, i.e. [[EntityKind.Constant]] for this entity
  * @param name name of the constant
  * @param constType functional type of the constant
  * @param definitions definition(s) term in string representation
  * @param uses ids of entities that the constant uses
  */
@SuppressWarnings(Array("NullParameter")) // Justification: Entity classes are mapped into solr document
final case class ConstEntity(
    @(Field @field)(ID) id: String,
    @(Field @field)(SOURCE_FILE) sourceFile: String,
    @(Field @field)(START_POS) startPos: Int,
    @(Field @field)(END_POS) endPos: Int,
    @(Field @field)(NAME) name: String,
    @(Field @field)(CONST_TYPE) constType: String,
    @(Field @field)(TERM) definitions: Array[String],
    @(Field @field)(USES) uses: Array[String],
    @(Field @field)(KIND) kind: String = EntityKind.Constant
) {
  def this() {
    this(null, null, -1, -1, null, null, null, null)
  }
}

/** Entity class for proven lemmas and theories.
  *
  * @param id solr doc id
  * @param sourceFile relative path to file containing entity
  * @param startPos token in src at which entity begins
  * @param endPos token in src at which entity ends
  * @param kind kind of the entity, i.e. [[EntityKind.Fact]] for this entity
  * @param term all terms of the fact as string representation
  * @param uses ids of entities that the fact uses
  */
@SuppressWarnings(Array("NullParameter")) // Justification: Entity classes are mapped into solr document
final case class FactEntity(
    @(Field @field)(ID) id: String,
    @(Field @field)(SOURCE_FILE) sourceFile: String,
    @(Field @field)(START_POS) startPos: Int,
    @(Field @field)(END_POS) endPos: Int,
    @(Field @field)(TERM) term: String,
    @(Field @field)(USES) uses: Array[String],
    @(Field @field)(KIND) kind: String = EntityKind.Fact
) {
  def this() {
    this(null, null, -1, -1, null, null)
  }
}

/** Entity class for documentation and comments.
  *
  * @param id solr doc id
  * @param sourceFile relative path to file containing entity
  * @param startPos token in src at which entity begins
  * @param endPos token in src at which entity ends
  * @param kind kind of the entity, i.e. [[EntityKind.Documentation]] for this entity
  * @param text text of the documentation
  */
@SuppressWarnings(Array("NullParameter")) // Justification: Entity classes are mapped into solr document
final case class DocumentationEntity(
    @(Field @field)(ID) id: String,
    @(Field @field)(SOURCE_FILE) sourceFile: String,
    @(Field @field)(START_POS) startPos: Int,
    @(Field @field)(END_POS) endPos: Int,
    @(Field @field)(TEXT) text: String,
    @(Field @field)(DOCTYPE) docType: String,
    @(Field @field)(KIND) kind: String = EntityKind.Documentation
) {
  def this() {
    this(null, null, -1, -1, null, null)
  }
}
