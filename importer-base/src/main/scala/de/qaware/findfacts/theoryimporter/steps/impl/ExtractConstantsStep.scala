package de.qaware.findfacts.theoryimporter.steps.impl

import scala.collection.mutable.ListBuffer

import com.typesafe.scalalogging.Logger
import de.qaware.findfacts.common.dt.{ConstantEt, Kind}
import de.qaware.findfacts.theoryimporter.TheoryView.Const
import de.qaware.findfacts.theoryimporter.steps.impl.thy.{PropExtractor, TypExtractor}
import de.qaware.findfacts.theoryimporter.steps.impl.util.IdBuilder
import de.qaware.findfacts.theoryimporter.steps.{ImportStep, StepContext}
import de.qaware.findfacts.theoryimporter.{ImportError, TheoryView}

/** Step to extract constants from theory view.
  *
  * @param idBuilder to build entity ids
  * @param typExtractor to extract types
  * @param propExtractor to extract references from propositions
  */
class ExtractConstantsStep(idBuilder: IdBuilder, typExtractor: TypExtractor, propExtractor: PropExtractor)
    extends ImportStep {
  private val logger = Logger[ExtractConstantsStep]

  override def apply(theory: TheoryView.Theory)(implicit ctx: StepContext): List[ImportError] = {
    logger.debug(s"Importing ${theory.consts.size} constants with ${theory.constdefs.size} def axioms...")

    // Store names in set for fast lookup
    val axiomNames: Set[String] = theory.constdefs.map(_.axiomName).toSet

    val errors = ListBuffer.empty[ImportError]

    // Group relevant axioms by their names
    val axiomsByName = theory.axioms
      .filter(ax => axiomNames.contains(ax.entity.name))
      .groupBy(_.entity.name)
      .toList
      .map {
        case (name, List(axiom)) => name -> axiom
        case (name, axioms) =>
          errors += ImportError(this, name, "Multiple axioms for name", s"${axioms.mkString(",")}")
          name -> axioms.head
      }
      .toMap

    // Convert mapping to Map
    val axiomNamesByConst = theory.constdefs
      .groupBy(_.name)
      .mapValues(_.map(_.axiomName))

    // Build entities
    theory.consts foreach { const =>
      val name = const.entity.name

      // Axioms may be empty for a constant BUT if a constant is in constdefs then the axiom must be present
      val axioms = axiomNamesByConst
        .getOrElse(name, List.empty)
        .flatMap(axName => axiomsByName.get(axName).orElse(logNotFound(axName, const, errors)))

      val props = axioms.map(_.prop)

      // Collect uses
      val usedTypes = (typExtractor.referencedTypes(const.typ) ++ props.flatMap(propExtractor.referencedTypes)).toList
      val usedConsts = props.flatMap(propExtractor.referencedConsts).filterNot(name.equals)

      val uses = idBuilder.getIds(Kind.Type, usedTypes) ++ idBuilder.getIds(Kind.Constant, usedConsts)

      // Add entity to context
      val et = new ConstantEt(name, uses, typExtractor.prettyPrint(const.typ))
      ctx.consts.addBinding(const.entity.pos, et)
    }

    logger.debug(s"Finished importing constants with ${errors.size} errors")
    errors.toList
  }

  private def logNotFound(axName: String, const: Const, errors: ListBuffer[ImportError]): Option[Nothing] = {
    errors += ImportError(this, axName, "Axiom for constdef not found", s"Const: $const")
    None
  }
}
