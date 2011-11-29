
package akka.setack
import akka.setack.core.TestExecutionManager
import org.junit.After
import org.junit.Before

/**
 * @author <a href="http://www.cs.illinois.edu/homes/tasharo1">Samira Tasharofi</a>
 */
trait SetackJUnit extends SetackTest {

  @Before
  def superSetUp() {
    super.superBeforeEach()
  }

  @After
  def superTearDown() {
    super.superAfterEach()
  }

}

