package tyrian.runtime

import tyrian.Cmd
import tyrian.Task

class CmdRunnerTests extends munit.FunSuite  {
  
  test("run a cmd") {

    var output: Int = -1

    val cmd: Cmd[Int] =
      Cmd.RunTask[String, Int, Int](Task.Succeeded(10), (res: Either[String, Int]) => res.toOption.getOrElse(0))
    val callback: Int => Unit = (i: Int) => {
      output = i
      ()
    }
    val async: (=> Unit) => Unit = thing => thing

    val actual = 
      CmdRunner.runCmd(cmd, callback, async)

    assertEquals(output, 10)
  }

}
