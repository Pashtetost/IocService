package model

sealed trait Action

object Action {
  final case object open extends Action

  final case object close extends Action

  final case object login extends Action

  final case object logout extends Action

  final case object access extends Action

  final case object unknown extends Action
}