/*
 * University of Illinois/NCSA Open Source License
 *
 * Copyright (c) 2011 Samira Tasharofi
 * All rights reserved.
 *
 * Developed by:  Samira Tasharofi <tasharo1@illinois.edu>
 *                University of Illinois at Urbana-Champaign
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal with the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimers.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the names of Samira Tasharofi, University of Illinois at Urbana-
 *    Champaign, nor the names of its contributors may be used to endorse
 *    or promote products derived from this Software without specific prior
 *    written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package akka.setack

import akka.event.EventHandler
import akka.event.EventHandler.{ Event, Error }
import akka.actor.Actor

sealed trait TestEvent

object TestEvent {
  object Mute {
    def apply(filter: EventFilter, filters: EventFilter*): Mute = new Mute(filter +: filters.toSeq)
  }
  case class Mute(filters: Seq[EventFilter]) extends TestEvent
  object UnMute {
    def apply(filter: EventFilter, filters: EventFilter*): UnMute = new UnMute(filter +: filters.toSeq)
  }
  case class UnMute(filters: Seq[EventFilter]) extends TestEvent
  case object UnMuteAll extends TestEvent
}

trait EventFilter {
  def apply(event: Event): Boolean
}

object EventFilter {

  def apply[A <: Throwable: Manifest](): EventFilter =
    ErrorFilter(manifest[A].erasure)

  def apply[A <: Throwable: Manifest](message: String): EventFilter =
    ErrorMessageFilter(manifest[A].erasure, message)

  def apply[A <: Throwable: Manifest](source: AnyRef): EventFilter =
    ErrorSourceFilter(manifest[A].erasure, source)

  def apply[A <: Throwable: Manifest](source: AnyRef, message: String): EventFilter =
    ErrorSourceMessageFilter(manifest[A].erasure, source, message)

  def custom(test: (Event) ⇒ Boolean): EventFilter =
    CustomEventFilter(test)
}

case class ErrorFilter(throwable: Class[_]) extends EventFilter {
  def apply(event: Event) = event match {
    case Error(cause, _, _) ⇒ throwable isInstance cause
    case _                  ⇒ false
  }
}

case class ErrorMessageFilter(throwable: Class[_], message: String) extends EventFilter {
  def apply(event: Event) = event match {
    case Error(cause, _, _) if !(throwable isInstance cause) ⇒ false
    case Error(cause, _, null) if cause.getMessage eq null   ⇒ cause.getStackTrace.length == 0
    case Error(cause, _, null)                               ⇒ cause.getMessage startsWith message
    case Error(cause, _, msg) ⇒
      (msg.toString startsWith message) || (cause.getMessage startsWith message)
    case _ ⇒ false
  }
}

case class ErrorSourceFilter(throwable: Class[_], source: AnyRef) extends EventFilter {
  def apply(event: Event) = event match {
    case Error(cause, instance, _) ⇒ (throwable isInstance cause) && (source eq instance)
    case _                         ⇒ false
  }
}

case class ErrorSourceMessageFilter(throwable: Class[_], source: AnyRef, message: String) extends EventFilter {
  def apply(event: Event) = event match {
    case Error(cause, instance, _) if !((throwable isInstance cause) && (source eq instance)) ⇒ false
    case Error(cause, _, null) if cause.getMessage eq null ⇒ cause.getStackTrace.length == 0
    case Error(cause, _, null) ⇒ cause.getMessage startsWith message
    case Error(cause, _, msg) ⇒
      (msg.toString startsWith message) || (cause.getMessage startsWith message)
    case _ ⇒ false
  }
}

case class CustomEventFilter(test: (Event) ⇒ Boolean) extends EventFilter {
  def apply(event: Event) = test(event)
}

class TestEventListener extends EventHandler.DefaultListener {
  import TestEvent._

  var filters: List[EventFilter] = Nil

  override def receive: Receive = ({
    case Mute(filters)                 ⇒ filters foreach addFilter
    case UnMute(filters)               ⇒ filters foreach removeFilter
    case UnMuteAll                     ⇒ filters = Nil
    case event: Event if filter(event) ⇒
  }: Receive) orElse super.receive

  def filter(event: Event): Boolean = filters exists (f ⇒ try { f(event) } catch { case e: Exception ⇒ false })

  def addFilter(filter: EventFilter): Unit = filters ::= filter

  def removeFilter(filter: EventFilter): Unit = {
    @scala.annotation.tailrec
    def removeFirst(list: List[EventFilter], zipped: List[EventFilter] = Nil): List[EventFilter] = list match {
      case head :: tail if head == filter ⇒ tail.reverse_:::(zipped)
      case head :: tail                   ⇒ removeFirst(tail, head :: zipped)
      case Nil                            ⇒ filters // filter not found, just return original list
    }
    filters = removeFirst(filters)
  }

}
