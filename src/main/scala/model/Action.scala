package model

sealed trait Action

case object open extends Action

case object close extends Action

case object login extends Action

case object logout extends Action

case object access extends Action

case object unknown extends Action