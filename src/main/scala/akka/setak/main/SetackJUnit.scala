/**
 * Copyright (C) 2011 Samira Tasharofi
 */
package akka.setak
import akka.setak.core.TestExecutionManager
import org.junit.After
import org.junit.Before

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
trait setakJUnit extends setakTest {

  @Before
  def superSetUp() {
    super.superBeforeEach()
  }

  @After
  def superTearDown() {
    super.superAfterEach()
  }

}

