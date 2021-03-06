package org.template.viewedthenboughtproduct

import io.prediction.data.storage.BiMap

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class CooccurrenceAlgorithmTest
  extends FlatSpec with EngineTestSparkContext with Matchers {

  val params = CooccurrenceAlgorithmParams(n = 10)
  val algorithm = new CooccurrenceAlgorithm(params)

  val itemStringIntMap = BiMap(Map(
    "i0" -> 0,
    "i1" -> 1,
    "i2" -> 2,
    "i3" -> 3
  ))

  val sequences = Seq(
    Sequence("u0",
             List(SequenceElement("i0",1000010)),
             List(SequenceElement("i0",1000020))),
    Sequence("u1",
             List(SequenceElement("i0",1000010),SequenceElement("i1",1000020)),
             List(SequenceElement("i1",1000030),SequenceElement("i0",1000030))),
    Sequence("u1",
             List(SequenceElement("i2",1000010),SequenceElement("i1",1000020)),
             List(SequenceElement("i1",1000030))),
    Sequence("u2",
             List(SequenceElement("i1",1000010),SequenceElement("i2",1000020)),
             List(SequenceElement("i1",1000030),SequenceElement("i2",1000030))),
    Sequence("u3",
             List(SequenceElement("i1",1000010),SequenceElement("i3",1000020)),
             List(SequenceElement("i1",1000030))),
    Sequence("u4",
             List(SequenceElement("i3",1000010),SequenceElement("i0",1000020)),
             List(SequenceElement("i0",1000030),SequenceElement("i3",1000030))),
    Sequence("u5",
             List(SequenceElement("i3",1000010),SequenceElement("i4",1000020)),
             List(SequenceElement("i3",1000030))),
    Sequence("u5",
             List(SequenceElement("i1",1000010),SequenceElement("i4",1000020)),
             List(SequenceElement("i1",1000030),SequenceElement("i0",1000040))),
    Sequence("u6",
             List(SequenceElement("i0",1000010)),
             List(SequenceElement("i0",1000020),SequenceElement("i1",1000020))),
    Sequence("u6",
             List(SequenceElement("i3",1000010)),
             List(SequenceElement("i1",1000020),SequenceElement("i3",1000020),SequenceElement("i4",1000020)))
  )

  "trainCooccurrence" should "return top 10 correctly" in {

    val sequencesRDD = sc.parallelize(sequences)

    val topCooccurrences = algorithm.trainCooccurrence(sequencesRDD, 10, itemStringIntMap)

    val expected = Map(
      0 -> Array((0, 4), (1, 2), (3, 1)),
      1 -> Array((1, 5), (0, 2), (2, 1)),
      2 -> Array((1, 2), (2, 1)),
      3 -> Array((3, 3), (1, 2), (0, 1))
    )

    topCooccurrences(0) should be (expected(0))
    topCooccurrences(1) should be (expected(1))
    topCooccurrences(2) should be (expected(2))
    topCooccurrences(3) should be (expected(3))

  }

  "trainCooccurrence" should "return top 1 correctly" in {

    val sequencesRDD = sc.parallelize(sequences)

    val topCooccurrences = algorithm.trainCooccurrence(sequencesRDD, 1, itemStringIntMap)

    val expected = Map(
      0 -> Array((0, 4)),
      1 -> Array((1, 5)),
      2 -> Array((1, 2)),
      3 -> Array((3, 3))
    )

    topCooccurrences(0) should be (expected(0))
    topCooccurrences(1) should be (expected(1))
    topCooccurrences(2) should be (expected(2))
    topCooccurrences(3) should be (expected(3))

  }

  val model = new CooccurrenceModel(
    //
    topCooccurrences = Map(
      0 -> Array((1, 4), (2, 1)),
      1 -> Array((0, 4), (2, 3), (3, 1)),
      2 -> Array((1, 3), (3, 2), (0, 1)),
      3 -> Array((2, 2), (1, 1))
    ),
    itemStringIntMap = BiMap(Map(
      "i0" -> 0,
      "i1" -> 1,
      "i2" -> 2,
      "i3" -> 3
    )),
    items = Map(
      0 -> Item(categories = Some(List("c0", "c1"))),
      1 -> Item(categories = None),
      2 -> Item(categories = Some(List("c0", "c2"))),
      3 -> Item(categories = Some(List("c0,", "c2", "c3")))
    )
  )

  // very basic test only
  "predict top 10 items" should "return PredictedResult correctly" in {

    val query = Query(
      items = List("i1"),
      num = 10,
      categories = None,
      whiteList = None,
      blackList = None
    )

    val predictedResult = algorithm.predict(model, query)

    val expected = PredictedResult(
      Array(ItemScore("i0", 4.0), ItemScore("i2", 3.0), ItemScore("i3", 1.0))
    )

    // scalatest can't match array with equal if wrapped insider case class.
    // directly compare itemScores array instead to work around.
    predictedResult.itemScores should equal (expected.itemScores)

  }

  "predict top 2 items" should "return PredictedResult correctly" in {

    val query = Query(
      items = List("i1", "i2"),
      num = 10,
      categories = None,
      whiteList = None,
      blackList = None
    )

    val predictedResult = algorithm.predict(model, query)

    val expected = PredictedResult(
      Array(ItemScore("i0", 5.0), ItemScore("i3", 3.0))
    )

    // scalatest can't match array with equal if wrapped insider case class.
    // directly compare itemScores array instead to work around.
    predictedResult.itemScores should equal (expected.itemScores)

  }


}
