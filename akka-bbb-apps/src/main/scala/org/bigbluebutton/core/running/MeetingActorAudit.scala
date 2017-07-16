package org.bigbluebutton.core.running

import java.io.{ PrintWriter, StringWriter }

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Resume
import scala.concurrent.duration._
import org.bigbluebutton.SystemConfiguration
import org.bigbluebutton.common2.domain.DefaultProps
import org.bigbluebutton.core.OutMessageGateway
import org.bigbluebutton.core.api._
import org.bigbluebutton.core.bus.{ BigBlueButtonEvent, IncomingEventBus }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ Deadline, FiniteDuration }

object MeetingActorAudit {
  def props(
    props:    DefaultProps,
    eventBus: IncomingEventBus,
    outGW:    OutMessageGateway
  ): Props =
    Props(classOf[MeetingActorAudit], props, eventBus, outGW)
}

// This actor is an internal audit actor for each meeting actor that
// periodically sends messages to the meeting actor
class MeetingActorAudit(
  val props:    DefaultProps,
  val eventBus: IncomingEventBus, val outGW: OutMessageGateway
)
    extends Actor with ActorLogging with SystemConfiguration with AuditHelpers {

  object AuditMonitorInternalMsg

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case e: Exception => {
      val sw: StringWriter = new StringWriter()
      sw.write("An exception has been thrown on MeetingActorInternal, exception message [" + e.getMessage() + "] (full stacktrace below)\n")
      e.printStackTrace(new PrintWriter(sw))
      log.error(sw.toString())
      Resume
    }
  }

  private val MonitorFrequency = 10 seconds

  context.system.scheduler.schedule(5 seconds, MonitorFrequency, self, AuditMonitorInternalMsg)

  // Query to get voice conference users
  getUsersInVoiceConf(props, outGW)

  if (props.meetingProp.isBreakout) {
    // This is a breakout room. Inform our parent meeting that we have been successfully created.
    /**TODO Need to add a 2.0 notification somehow */
    log.error("****** MeetingActorInternal still needs to be fixed with 2.0 breakout messages ******")
    /*eventBus.publish(BigBlueButtonEvent(
      props.breakoutProps.parentId,
      BreakoutRoomCreated(props.breakoutProps.parentId, props.meetingProp.intId)))
     */
  }

  def receive = {
    case AuditMonitorInternalMsg => handleMonitor()
  }

  def handleMonitor() {
    handleMonitorNumberOfWebUsers()
  }

  def handleMonitorNumberOfWebUsers() {
    eventBus.publish(BigBlueButtonEvent(props.meetingProp.intId, MonitorNumberOfUsers(props.meetingProp.intId)))

    // Trigger updating users of time remaining on meeting.
    eventBus.publish(BigBlueButtonEvent(props.meetingProp.intId, SendTimeRemainingUpdate(props.meetingProp.intId)))

    if (props.meetingProp.isBreakout) {
      /**TODO Need to add a 2.0 notification somehow */
      log.error("******* MeetingActorInternal still needs to be fixed with 2.0 breakout messages *******")
      // This is a breakout room. Update the main meeting with list of users in this breakout room.
      //eventBus.publish(BigBlueButtonEvent(props.meetingProp.intId, SendBreakoutUsersUpdate(props.meetingProp.intId)))
    }

  }

}
