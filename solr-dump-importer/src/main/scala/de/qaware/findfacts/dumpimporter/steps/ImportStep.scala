package de.qaware.findfacts.dumpimporter.steps

import de.qaware.findfacts.common.solr.Record.Id
import de.qaware.findfacts.common.solr.{ConstRecord, DocRecord, FactRecord, Record, TheoryRecord, TypeRecord}
import de.qaware.findfacts.dumpimporter.Config

import scala.collection.mutable

/** Step of the import process. */
trait ImportStep {

  /** Configuration parameter */
  val config: Config

  /** Applies the step, mutating the context.
    *
    * @param ctx context that is mutated in the step
    */
  def apply(ctx: StepContext): Unit
}

/** Holds mutable context shared throughout the steps.
  *
  * @param _serialsById serial id for each entity, consistent for a single run
  * @param _consts intermediate constant entities
  * @param _types intermediate type entities
  * @param _facts intermediate fact entities
  * @param _docs intermediate documentation entities
  */
final class StepContext(
    _serialsById: mutable.MultiMap[Id, Long] = new mutable.HashMap[Id, mutable.Set[Long]]
    with mutable.MultiMap[Id, Long],
    _consts: mutable.Set[ConstRecord] = mutable.Set.empty,
    _types: mutable.Set[TypeRecord] = mutable.Set.empty,
    _facts: mutable.Set[FactRecord] = mutable.Set.empty,
    _docs: mutable.Set[DocRecord] = mutable.Set.empty) {

  private def addToSet(entity: Record): Unit = entity match {
    case e: ConstRecord => _consts.add(e)
    case t: TypeRecord => _types.add(t)
    case f: FactRecord => _facts.add(f)
    case d: DocRecord => _docs.add(d)
  }

  private def removeFromSet(entity: Record): Unit = entity match {
    case e: ConstRecord => _consts.remove(e)
    case t: TypeRecord => _types.remove(t)
    case f: FactRecord => _facts.remove(f)
    case d: DocRecord => _docs.remove(d)
  }

  /** Adds an entity and all its isabelle serials to the context.
    *
    * @param entity to add
    * @param serials of corresponding isabelle entities
    */
  def addEntity(entity: Record, serials: Seq[Long] = Seq.empty): Unit = {
    serials.map(_serialsById.addBinding(entity.id, _))
    addToSet(entity)
  }

  /** Updates an entity in the context.
    *
    * @param old entity object
    * @param entity updated
    */
  def updateEntity(old: Record, entity: Record): Unit = {
    // Id has to remain stable!
    if (old.id != entity.id) {
      throw new IllegalArgumentException("Id must not change when updating entities!")
    }

    // Update serial mapping
    val serials = _serialsById.remove(old.id).getOrElse(mutable.Set.empty)
    _serialsById.put(entity.id, serials)
    removeFromSet(old)
    addToSet(entity)
  }

  /** Accessor for immutable view on consts.
    *
    * @return immutable consts set
    */
  def consts: Set[ConstRecord] = _consts.to[Set]

  /** Accessor for immutable view on types.
    *
    * @return immutable types set
    */
  def types: Set[TypeRecord] = _types.to[Set]

  /** Accessor for immutable view on facts.
    *
    * @return immutable facts set
    */
  def facts: Set[FactRecord] = _facts.to[Set]

  /** Accessor for immutable view on docs.
    *
    * @return immutable docs set
    */
  def docs: Set[DocRecord] = _docs.to[Set]

  /** Accessor to look up isabelle an immutable view of serials belonging to an entity.
    *
    * @param id unique id of the entity to look up
    * @return immutable set of ids
    */
  def serialsById(id: Id): Set[Long] = _serialsById.getOrElse(id, mutable.Set.empty).to[Set]

  /** Gives an immutable set of all semantic theory entities.
    *
    * @return a set of all semantic theory entities
    */
  def theoryEntities: Set[TheoryRecord] = consts ++ types ++ facts

  /** Gives an immutable set of all entities.
    *
    * @return a set of all entities
    */
  def allEntities: Set[Record] = consts ++ facts ++ types ++ docs
}
object StepContext {

  /** Builds an empty context.
    *
    * @return a new context
    */
  def empty: StepContext = new StepContext()
}
