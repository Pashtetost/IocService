package model

import ternarytree.TernaryTree
import zio.Ref

case class IocData(hashTree: Ref[Map[String, TernaryTree[Ioc]]], capabIoc: Ref[Map[Int, Int]])
