/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak

trait SetakFlatSpec extends SetakTest with org.scalatest.FlatSpec with org.scalatest.BeforeAndAfterEach {

  override def beforeEach() {
    super.superBeforeEach()
    setUp()
  }

  override def afterEach() {
    tearDown()
    super.superAfterEach()
  }

  def setUp() {}
  def tearDown() {}

}

trait SetakWordSpec extends SetakTest with org.scalatest.WordSpec with org.scalatest.BeforeAndAfterEach {

  override def beforeEach() {
    super.superBeforeEach()
    setUp()
  }

  override def afterEach() {
    tearDown()
    super.superAfterEach()
  }

  def setUp() {}
  def tearDown() {}

}