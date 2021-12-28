package ternarytree

import scala.annotation.tailrec

/**
 *
 * take from: https://gist.github.com/alirezameskin/95f79c4ed5ecded2929a52da1cf817bc
 */


sealed trait TernaryTree[+A] {
  def insert[B >: A](key: String, value: B): TernaryTree[B] = TernaryTree.insert(this, key, value, 0)

  def search(key: String): Option[A] = TernaryTree.search(this, key, 0)

  def keys: List[String] = TernaryTree.keys(this)

  def keysWithPrefix(prefix: String): List[String] = TernaryTree.keys(this, prefix)
}

case class Node[D](value: Option[D], char: Char, left: TernaryTree[D], middle: TernaryTree[D], right: TernaryTree[D]) extends TernaryTree[D]

case object Leaf extends TernaryTree[Nothing]

object TernaryTree {
  def apply[V]: TernaryTree[V] = Leaf

  private def keys[A](root: TernaryTree[A]): List[String] = collect(root, "")

  private def keys[A](root: TernaryTree[A], prefix: String): List[String] =
    get(root, prefix, 0) match {
      case None => Nil
      case Some(node) =>
        collect(node, prefix.dropRight(1))
    }

  private def collect[A](node: TernaryTree[A], prefix: String): List[String] =
    node match {
      case Leaf => Nil
      case node: Node[A] if node.value.isDefined =>
        (prefix + node.char) +: (collect(node.left, prefix) ++ collect(node.middle, prefix + node.char) ++ collect(node.right, prefix))
      case node: Node[A] =>
        collect(node.left, prefix) ++ collect(node.middle, prefix + node.char) ++ collect(node.right, prefix)
    }

  @tailrec
  private def get[A](root: TernaryTree[A], prefix: String, step: Int): Option[TernaryTree[A]] = root match {
    case Leaf => None
    case node: Node[A] if node.char > prefix.charAt(step) => get(node.left, prefix, step)
    case node: Node[A] if node.char < prefix.charAt(step) => get(node.right, prefix, step)
    case node: Node[A] if step < prefix.length - 1 => get(node.middle, prefix, step + 1)
    case node: Node[A] => Some(node)
  }

  @tailrec
  private def search[A](root: TernaryTree[A], key: String, step: Int): Option[A] = root match {
    case Leaf => None
    case node: Node[A] if node.char > key.charAt(step) => search(node.left, key, step)
    case node: Node[A] if node.char < key.charAt(step) => search(node.right, key, step)
    case node: Node[A] if step < key.length - 1 => search(node.middle, key, step + 1)
    case node: Node[A] => node.value
  }

  private def insert[A](root: TernaryTree[A], key: String, value: A, step: Int): TernaryTree[A] = root match {
    case Leaf =>
      val node = Node(None, key.charAt(step), Leaf, Leaf, Leaf)
      insert(node, key, value, step)

    case node: Node[A] if node.char > key.charAt(step) =>
      val left = insert(node.left, key, value, step)
      node.copy(left = left)

    case node: Node[A] if node.char < key.charAt(step) =>
      val right = insert(node.right, key, value, step)
      node.copy(right = right)

    case node: Node[A] if step < key.length - 1 =>
      val mid = insert(node.middle, key, value, step + 1)
      node.copy(middle = mid)

    case node: Node[A] =>
      node.copy(value = Some(value))
  }

}
