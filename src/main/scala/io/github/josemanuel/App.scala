package io.github.josemanuel

import scala.util.Try
import scala.math.BigInt

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

  def normalizeArgs(args: Array[String]): Seq[String] =
    args.toSeq.map(_.trim).filter(_.nonEmpty)

  def uniquePreserveOrder(words: Seq[String]): Seq[String] = {
    val (result, _) = words.foldLeft((Seq.empty[String], Set.empty[String])) {
      case ((acc, seen), word) =>
        val key = word.toLowerCase
        if (seen.contains(key)) (acc, seen) else (acc :+ word, seen + key)
    }
    result
  }

  def reverseEach(words: Seq[String]): Seq[String] =
    words.map(_.reverse)

  def wordFrequencies(words: Seq[String]): Map[String, Int] =
    words.groupBy(_.toLowerCase).view.mapValues(_.size).toMap

  def numbersFromArgs(args: Array[String]): Seq[Int] =
    args.flatMap(arg => Try(arg.trim.toInt).toOption)

  def average(numbers: Seq[Int]): Option[Double] =
    if (numbers.isEmpty) None else Some(numbers.sum.toDouble / numbers.size)

  def median(numbers: Seq[Int]): Option[Double] = {
    if (numbers.isEmpty) None
    else {
      val sorted = numbers.sorted
      val mid = sorted.length / 2
      if (sorted.length % 2 == 1) Some(sorted(mid).toDouble)
      else Some((sorted(mid - 1) + sorted(mid)).toDouble / 2.0)
    }
  }

  def minMax(numbers: Seq[Int]): Option[(Int, Int)] =
    if (numbers.isEmpty) None else Some((numbers.min, numbers.max))

  def evenOddSplit(numbers: Seq[Int]): (Seq[Int], Seq[Int]) =
    numbers.partition(_ % 2 == 0)

  def cumulativeSums(numbers: Seq[Int]): Seq[Int] =
    numbers.scanLeft(0)(_ + _).tail

  def factorial(n: Int): Option[BigInt] =
    if (n < 0) None else Some((1 to n).foldLeft(BigInt(1))((acc, value) => acc * value))

  def fibonacci(n: Int): Seq[BigInt] = {
    if (n <= 0) Seq.empty
    else if (n == 1) Seq(BigInt(0))
    else {
      val buffer = collection.mutable.ArrayBuffer(BigInt(0), BigInt(1))
      while (buffer.length < n) {
        val next = buffer.takeRight(2).sum
        buffer.append(next)
      }
      buffer.toSeq
    }
  }

  def groupByLength(words: Seq[String]): Map[Int, Seq[String]] =
    words.groupBy(_.length)

  def filterContainingLetter(words: Seq[String], letter: Char): Seq[String] =
    words.filter(_.toLowerCase.contains(letter.toLower))

  def sortDescending(words: Seq[String]): Seq[String] =
    words.sortBy(_.toLowerCase)(Ordering[String].reverse)

  def pairWithLength(words: Seq[String]): Seq[(String, Int)] =
    words.map(word => (word, word.length))

  def duplicateWords(words: Seq[String]): Seq[String] =
    words.flatMap(word => Seq(word, word))

  def slidingWindows(words: Seq[String], size: Int): Seq[Seq[String]] =
    if (size <= 0) Seq.empty else words.sliding(size).toSeq

  def sentencesFromWords(words: Seq[String], size: Int): Seq[String] =
    slidingWindows(words, size).map(window => window.mkString(" "))

  def rotateWords(words: Seq[String], shift: Int): Seq[String] = {
    if (words.isEmpty) Seq.empty
    else {
      val normalizedShift = ((shift % words.length) + words.length) % words.length
      words.drop(normalizedShift) ++ words.take(normalizedShift)
    }
  }

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

    val normalized = normalizeArgs(args)
    println(s"normalized arguments (${normalized.size}): ${normalized.mkString(", ")}")

    val unique = uniquePreserveOrder(normalized)
    println(s"unique arguments (${unique.size}): ${unique.mkString(", ")}")

    val reversed = reverseEach(unique)
    println(s"reversed words: ${reversed.mkString(", ")}")

    val freq = wordFrequencies(normalized)
    println("frequencies:")
    freq.toSeq.sortBy(_._1).foreach { case (word, count) => println(s"  $word -> $count") }

    val numbers = numbersFromArgs(args)
    println(s"numbers detected (${numbers.size}): ${numbers.mkString(", ")}")

    average(numbers).foreach(avg => println(f"average: $avg%.2f"))
    median(numbers).foreach(med => println(f"median: $med%.2f"))
    minMax(numbers).foreach { case (min, max) => println(s"min: $min, max: $max") }

    val (evens, odds) = evenOddSplit(numbers)
    println(s"even numbers: ${evens.mkString(", ")}")
    println(s"odd numbers: ${odds.mkString(", ")}")

    val cumulative = cumulativeSums(numbers)
    println(s"cumulative sums: ${cumulative.mkString(", ")}")

    factorial(numbers.headOption.getOrElse(5)).foreach { fact =>
      println(s"factorial of first number or 5: $fact")
    }

    val fibSeq = fibonacci(10)
    println(s"first 10 fibonacci numbers: ${fibSeq.mkString(", ")}")

    val groupedByLength = groupByLength(unique)
    println("grouped by length:")
    groupedByLength.toSeq.sortBy(_._1).foreach { case (length, values) =>
      println(s"  $length -> ${values.mkString(", ")}")
    }

    val filteredByA = filterContainingLetter(unique, 'a')
    println(s"words containing 'a': ${filteredByA.mkString(", ")}")

    val sortedDesc = sortDescending(unique)
    println(s"sorted descending: ${sortedDesc.mkString(", ")}")

    val paired = pairWithLength(unique)
    println("paired with length:")
    paired.foreach { case (word, length) => println(s"  $word -> $length") }

    val duplicated = duplicateWords(unique)
    println(s"duplicated words: ${duplicated.mkString(", ")}")

    val windows = sentencesFromWords(unique, 2)
    println(s"sentences of size 2: ${windows.mkString(" | ")}")

    val rotated = rotateWords(unique, 1)
    println(s"rotated words: ${rotated.mkString(", ")}")
  }

  // Explain how github copilot works
  // Github Copilot is an AI-powered code completion tool developed by GitHub in collaboration with OpenAI. It uses machine learning models trained on a vast amount of publicly available code to provide context-aware code suggestions and completions as developers write code. By analyzing the code being written, Copilot can suggest entire lines or blocks of code, helping to speed up the coding process and improve productivity. It supports multiple programming languages and integrates seamlessly with popular code editors like Visual Studio Code.
  // More info at https://copilot.github.com/


}
