package tyrian.cmds

import cats.effect.kernel.Async
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.document
import org.scalajs.dom.html
import tyrian.Cmd

import scala.concurrent.Promise
import scala.scalajs.js

/** Given the id of a file input field that has had a file selected, this Cmd will read either an image or text file to
  * return an `HTMLImageElement` or `String` respectively.
  */
object FileReader:

  /** Reads an input file from an input field as base64 encoded image data */
  def readImage[F[_]: Async, Msg](inputFieldId: String)(resultToMessage: Result[String] => Msg): Cmd[F, Msg] =
    val cast: Result[js.Any] => Result[String] =
      case Result.Error(msg) => Result.Error(msg)
      case Result.File(n, p, d) =>
        try Result.File(n, p, d.asInstanceOf[String])
        catch case _ => Result.Error("File is not a base64 string of image data")

    readFromInputField(inputFieldId, false)(cast andThen resultToMessage)

  /** Reads an input file as base64 encoded image data */
  def readImage[F[_]: Async, Msg](file: dom.File)(resultToMessage: Result[String] => Msg): Cmd[F, Msg] =
    val cast: Result[js.Any] => Result[String] =
      case Result.Error(msg) => Result.Error(msg)
      case Result.File(n, p, d) =>
        try Result.File(n, p, d.asInstanceOf[String])
        catch case _ => Result.Error("File is not a base64 string of image data")

    readFile(file, false)(cast andThen resultToMessage)

  /** Reads an input file from an input field as plain text */
  def readText[F[_]: Async, Msg](inputFieldId: String)(resultToMessage: Result[String] => Msg): Cmd[F, Msg] =
    val cast: Result[js.Any] => Result[String] =
      case Result.Error(msg) => Result.Error(msg)
      case Result.File(n, p, d) =>
        try Result.File(n, p, d.asInstanceOf[String])
        catch case _ => Result.Error("File is not text")

    readFromInputField(inputFieldId, true)(cast andThen resultToMessage)

  /** Reads an input file as plain text */
  def readText[F[_]: Async, Msg](file: dom.File)(resultToMessage: Result[String] => Msg): Cmd[F, Msg] =
    val cast: Result[js.Any] => Result[String] =
      case Result.Error(msg) => Result.Error(msg)
      case Result.File(n, p, d) =>
        try Result.File(n, p, d.asInstanceOf[String])
        catch case _ => Result.Error("File is not text")

    readFile(file, true)(cast andThen resultToMessage)

  private def readFromInputField[F[_]: Async, Msg](fileInputFieldId: String, isText: Boolean)(
      resultToMessage: Result[js.Any] => Msg
  ): Cmd[F, Msg] =
    val files = document.getElementById(fileInputFieldId).asInstanceOf[html.Input].files
    if files.length == 0 then Cmd.None
    else readFile(files.item(0), isText)(resultToMessage)

  private def readFile[F[_]: Async, Msg](file: dom.File, isText: Boolean)(
      resultToMessage: Result[js.Any] => Msg
  ): Cmd[F, Msg] =
    val task = Async[F].delay {
      val p          = Promise[Result[js.Any]]()
      val fileReader = new dom.FileReader()
      fileReader.addEventListener(
        "load",
        (e: Event) =>
          p.success(
            Result.File(
              name = file.name,
              path = e.target.asInstanceOf[js.Dynamic].result.asInstanceOf[String],
              data = fileReader.result
            )
          ),
        false
      )
      fileReader.onerror = _ => p.success(Result.Error(s"Error reading from file"))

      if isText then fileReader.readAsText(file)
      else fileReader.readAsDataURL(file)

      p.future
    }

    Cmd.Run(Async[F].fromFuture(task), resultToMessage)

  enum Result[A]:
    case Error(message: String)
    case File(name: String, path: String, data: A)
