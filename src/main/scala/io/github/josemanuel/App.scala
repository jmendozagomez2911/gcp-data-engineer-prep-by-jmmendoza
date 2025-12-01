package io.github.josemanuel

import scala.util.Try

/**
 * @author ${user.name}
 */


object App {

  def foo(x: Array[String]): String = x.foldLeft("")((a, b) => a + b)

  def concatWithSpaces(args: Array[String]): String =
    args.mkString(" ")

  def sumNumericArgs(args: Array[String]): Int =
    args.flatMap(arg => Try(arg.toInt).toOption).sum

  def longestArg(args: Array[String]): Option[String] =
    args.reduceOption((a, b) => if (a.length >= b.length) a else b)

  def bucketByFirstLetter(args: Array[String]): Map[Char, Seq[String]] =
    args.toSeq.map(_.trim).filter(_.nonEmpty).groupBy(_.head.toLower)

  def main(args: Array[String]): Unit = {
    println("Hello World!")
    println("concat arguments = " + foo(args))

    println("spaced arguments = " + concatWithSpaces(args))
    println("numeric sum = " + sumNumericArgs(args))
    println("longest argument = " + longestArg(args).getOrElse("<none>"))

    println("bucketed by first letter:")
    bucketByFirstLetter(args).toSeq.sortBy(_._1).foreach {
      case (letter, words) =>
        println(s"  $letter -> ${words.mkString(", ")}")
    }
  }

}
