package examples.junit

import java.{ util => ju, lang => jl }
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(value = classOf[Parameterized])
class JUnit4ParameterizedTest(number: jl.Integer) {
  @Test def pushTest = println("number: " + number)
}

// NOTE: Defined AFTER companion class to prevent:
// Class com.openmip.drm.JUnit4ParameterizedTest has no public
// constructor TestCase(String name) or TestCase()
object JUnit4ParameterizedTest {

  // NOTE: Must return collection of Array[AnyRef] (NOT Array[Any]).
  @Parameters def parameters: ju.Collection[Array[jl.Integer]] = {
    val list = new ju.ArrayList[Array[jl.Integer]]()
    (1 to 10).foreach(n => list.add(Array(n)))
    list
  }
}


/**
 * source: https://stackoverflow.com/questions/4399881/parameterized-unit-tests-in-scala-with-junit4
 */