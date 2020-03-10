package de.qaware.findfacts.theoryimporter.steps.impl

import com.typesafe.scalalogging.Logger
import de.qaware.findfacts.common.dt.{FactEt, Kind}
import de.qaware.findfacts.theoryimporter.steps.impl.thy.{ProofExtractor, PropExtractor}
import de.qaware.findfacts.theoryimporter.steps.impl.util.IdBuilder
import de.qaware.findfacts.theoryimporter.steps.{ImportStep, StepContext}
import de.qaware.findfacts.theoryimporter.{ImportError, TheoryView}

/** Extracts the facts from a theory view.
  *
  * @param idBuilder to build entity ids
  * @param propExtractor to extract proposition data
  * @param proofExtractor to extract proof data
  */
class ExtractFactsStep(idBuilder: IdBuilder, propExtractor: PropExtractor, proofExtractor: ProofExtractor)
    extends ImportStep {
  private val logger = Logger[ExtractFactsStep]

  override def apply(theory: TheoryView.Theory)(implicit ctx: StepContext): List[ImportError] = {
    logger.debug(s"Importing ${theory.axioms.size} axioms and ${theory.thms.size} thms...")

    // Filter out definition axioms as they are aggregated in their entity types
    val defNames: Set[String] = (theory.constdefs.map(_.axiomName) ++ theory.typedefs.map(_.axiomName)).toSet

    // Add axioms
    theory.axioms.filterNot(ax => defNames.contains(ax.entity.name)) map { axiom =>
      val uses = idBuilder.getIds(Kind.Type, propExtractor.referencedTypes(axiom.prop).toList) ++
        idBuilder.getIds(Kind.Constant, propExtractor.referencedConsts(axiom.prop).toList)

      ctx.facts.addBinding(axiom.entity.pos, new FactEt(axiom.entity.name, uses))
    }

    // Add theorems
    theory.thms map { thm =>
      val name = thm.entity.name

      // Find usages
      val usedTypes = idBuilder.getIds(
        Kind.Type,
        (propExtractor.referencedTypes(thm.prop) ++ proofExtractor.referencedTypes(thm.proof)).toList)

      val usedConsts = idBuilder.getIds(
        Kind.Constant,
        (propExtractor.referencedConsts(thm.prop) ++ proofExtractor.referencedConsts(thm.proof)).toList)

      val usedFacts = idBuilder.getIds(
        Kind.Fact,
        (thm.deps ++ proofExtractor.referencedFacts(thm.proof).filterNot(name.equals)).distinct)

      ctx.facts.addBinding(thm.entity.pos, new FactEt(name, usedTypes ++ usedConsts ++ usedFacts))
    }

    logger.debug("Finished importing facts")
    List.empty
  }
}