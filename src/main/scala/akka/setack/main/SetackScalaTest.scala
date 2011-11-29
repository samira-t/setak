/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.setack

trait SetackFlatSpec extends SetackTest with org.scalatest.FlatSpec with org.scalatest.BeforeAndAfterEach {

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

trait SetackWordSpec extends SetackTest with org.scalatest.WordSpec with org.scalatest.BeforeAndAfterEach {

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