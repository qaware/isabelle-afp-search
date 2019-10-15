package de.qaware.yxml

import scala.util.parsing.input.Positional

/** Trait for the sum type of the yxml abstract syntax tree. */
sealed trait YXMLAST extends Positional

/** The whole yxml as abstract syntax forest.
  *
  * @param trees all root trees of the yxml
  */
case class YXML(trees: Seq[Tree] = Seq()) extends YXMLAST

/** A single yxml tree.
  *
  * @param elem the root tag of the tree
  * @param inner elements contained in the tree, i.e. text and more trees
  */
case class Tree(elem: Tag, inner: Seq[Inner] = Seq()) extends YXMLAST with Inner

/** A yxml tag.
  *
  * @param name of the element
  * @param kvPairs key=value pairs in the tag
  */
case class Tag(name: Value, kvPairs: Seq[KVPair] = Seq()) extends YXMLAST

/** Closing tag. Contains only positional information. */
case class Close() extends YXMLAST

/** A single key=value pair.
  *
  * @param key of the pair
  * @param value of the pair
  */
case class KVPair(key: Value, value: Value) extends YXMLAST

/** Trait for the sum type of the inner tree elements (text and more trees). */
sealed trait Inner extends YXMLAST

/** Text body, to be contained in an yxml tree.
  *
  * @param str body text
  */
case class Body(str: String) extends Inner

/** High-level text AST token.
  *
  * @param str identifier text
  */
case class Value(str: String) extends YXMLAST
