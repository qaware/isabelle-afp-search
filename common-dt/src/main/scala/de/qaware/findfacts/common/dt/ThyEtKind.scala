package de.qaware.findfacts.common.dt

import de.qaware.findfacts.common.utils.DefaultEnum
import enumeratum.EnumEntry

/** Union type for theory entity kinds. */
sealed trait ThyEtKind extends EnumEntry

/** Kinds of theory entities. */
object ThyEtKind extends DefaultEnum[ThyEtKind] {
  override final val values = findValues

  /** Type definitions. */
  case object Type extends Value

  /** Pure constants. */
  case object Constant extends Value

  /** Pure axioms and thms. */
  case object Fact extends Value
}