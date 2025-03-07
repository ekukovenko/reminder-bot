package com.schedulers

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import cats.effect.unsafe.IORuntime
import com.bot.RemindBot
import com.services.ReminderService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ReminderScheduler {
  private val system: ActorSystem = ActorSystem("ReminderScheduler")
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val runtime: IORuntime = IORuntime.global

  private class ReminderActor(bot: RemindBot) extends Actor {
    def receive: Receive = { case CheckReminders =>
      checkReminders()
    }

    private def checkReminders(): Unit = {
      ReminderService.getAllUserIds().unsafeRunAsync {
        case Right(userIds) =>
          userIds.foreach { userId =>
            ReminderService.getAllReminders(userId).unsafeRunAsync {
              case Right(reminders) =>
                val currentTime = System.currentTimeMillis()
                reminders.filter(_.timestamp <= currentTime).foreach { reminder =>
                  println(s"Напоминаю пользователю ${reminder.userId}: ${reminder.message}")
                  bot.remind(reminder.userId, reminder.message)
                  if (reminder.repeat) {
                    val nextReminderTime = reminder.timestamp + (24L * 60 * 60 * 1000) // + 1 день
                    ReminderService.updateReminderTimestamp(reminder.id, nextReminderTime).unsafeRunSync()
                  } else {
                    ReminderService.deleteOneReminder(reminder.id).unsafeRunSync()
                  }
                }
              case Left(exception) =>
                println(s"Ошибка при получении напоминаний: ${exception.getMessage}")
            }
          }
        case Left(exception) =>
          println(s"Ошибка при получении пользователей: ${exception.getMessage}")
      }
    }
  }

  private case object CheckReminders

  def start(bot: RemindBot): Unit = {
    val reminderActor = system.actorOf(Props(new ReminderActor(bot)))

    system.scheduler.scheduleWithFixedDelay(0.seconds, 1.minute) { () =>
      {
        println("Проверка напоминаний...")
        reminderActor ! CheckReminders
      }
    }
  }

  def stop(): Unit = {
    system.terminate()
  }
}
