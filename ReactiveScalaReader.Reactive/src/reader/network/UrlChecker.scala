package reader.network

import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException

import react.events.Event
import react.events.Observable

class UrlChecker {
  type CheckArg = String
  type CheckResult = Either[String, URL]
  type AfterCheck = (CheckArg, CheckResult)
  
  /**
   * Try to increase confidence that the String is a valid feed url
   * by performing some simple checks.
   *
   * @param url The string to check
   * @return Nothing is returned but events are fired, see below
   */
  val check = Observable {
    // Tries to create a url from the string and returns it in Right
    // if not successful, a Left with an error message is returned
    (url: String) =>
      try {
        val u = new URL(url)
        u.getContent
        Right(u)
      }
      catch {
        case e: UnknownHostException => Left(errorMessage(url, e))
        case e: MalformedURLException => Left(errorMessage(url, e))
      }
  }
  
  private lazy val checkSuccessful: Event[CheckResult] =
    check.after && { t: AfterCheck => t._2.isRight } map { t: AfterCheck => t._2 } //#EVT //#EF //#EF
  
  private lazy val checkFailed: Event[CheckResult] =
    check.after && { t: AfterCheck => t._2.isLeft } map { t: AfterCheck => t._2 } //#EVT //#EF //#EF
  
  private lazy val checkedOption: Event[Option[URL]] =
    (checkSuccessful || checkFailed) map { (_: CheckResult) match { //#EVT //#EF //#EF
      case Right(u) => Some(u)
      case Left(_)  => None
    }}

  /** Fired for every valid url checked */
  lazy val checkedURL: Event[URL] = checkedOption && //#EVT //#EF //#EF
    { t: Option[URL] => t.isDefined } map { t: Option[URL] => t.get }
  
  /** Only fires if the checked url is valid */
  lazy val urlIsValid: Event[Unit] = checkSuccessful.dropParam //#EVT //#EF
  
  /** Only fires if the checked url is invalid */
  lazy val urlIsInvalid: Event[Unit] = checkFailed.dropParam  //#EVT //#EF
  
  private def errorMessage(url: String, e: Exception): String =
    "Error while checking '" + url + "' - " + e.getMessage
}
