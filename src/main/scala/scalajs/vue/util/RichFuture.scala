package scalajs.vue.util

// https://github.com/jducoeur/jsext

import scala.concurrent.{ Future, Promise, TimeoutException }
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import scala.scalajs.js
import js.timers.setTimeout
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Useful enhancements to Future.
 */
class RichFuture[ T ]( fut : Future[ T ] ) {
  /**
   * Cause this Future to fail with a FutureTimeoutException, with the given message,
   * if it hasn't otherwise resolved in the given time. Note that it is an error if the
   * Future resolves *after* this time -- that will cause a double-resolution. So this
   * is mainly intended as a backstop to prevent hangs.
   */
  def failAfter( duration : FiniteDuration, msg : String ) : Future[ T ] = {
    val promise = Promise[ T ]
    fut.onComplete {
      case Success( s ) => promise.success( s )
      case Failure( f ) => promise.failure( f )
    }
    setTimeout( duration ) {
      if ( !fut.isCompleted ) {
        promise.failure( new TimeoutException( msg ) )
      }
    }
    promise.future
  }

  /**
   * Simpler version of failAfter(), which will fail after 1 second.
   */
  def withTimeout( msg : String ) : Future[ T ] = failAfter( 1 second, msg )
  /**
   * Simplest version of failAfter(): fails after 1 second, with a standard message.
   * Useful for sanity-checking, especially in testing.
   */
  def withTimeout : Future[ T ] = withTimeout( "Future timed out" )

  /**
   * Guarantees that this Future will *not* complete synchronously. If it is already completed,
   * this returns a new Future that will return the same value after a minimal delay.
   *
   * This is mainly useful for simplifying code paths so that you don't have to deal with
   * Futures that return synchronously only during, eg, testing.
   */
  def notYet : Future[ T ] = {
    if ( fut.isCompleted ) {
      val promise = Promise[ T ]
      setTimeout( 1 ) {
        promise.completeWith( fut )
      }
      promise.future
    } else
      fut
  }
}
