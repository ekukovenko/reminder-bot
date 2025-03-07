package com.bot

import cats.effect.unsafe.IORuntime
import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative._
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods._
import com.services.ReminderService
import org.joda.time.format.DateTimeFormat
import com.github.nscala_time.time.Imports._

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class RemindBot(val token: String)(implicit ec: ExecutionContext, runtime: IORuntime)
  extends TelegramBot
  with Polling
  with Commands[Future] {

  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  def remind(userId: Long, reminderText: String): Future[Unit] = {
    val message = s"Напоминаю: $reminderText"
    request(SendMessage(userId, message)).map(_ => ())
  }

  private def parseTime(timeString: String): Option[Long] = {
    try {
      val formatter = DateTimeFormat.forPattern("HH:mm")
      val time = formatter.parseLocalTime(timeString)

      val currentDateTime = DateTime.now()

      val nextReminderTime = currentDateTime
        .withHourOfDay(time.getHourOfDay)
        .withMinuteOfHour(time.getMinuteOfHour)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)

      val nextReminderTimeWithCorrectDate = if (nextReminderTime.isBeforeNow) {
        nextReminderTime.plusDays(1)
      } else
        nextReminderTime
      Some(nextReminderTimeWithCorrectDate.getMillis)
    } catch {
      case _: Exception => None
    }
  }

  onCommand("/remind") { implicit msg =>
    withArgs { args =>
      val repeat = args.contains("repeat")
      val timeIndex = if (repeat) args.length - 2 else args.length - 1
      val timeString = args(timeIndex)

      val reminderText = if (repeat) {
        args.dropRight(2).mkString(" ")
      } else {
        args.dropRight(1).mkString(" ")
      }

      parseTime(timeString) match {
        case Some(timestamp) =>
          ReminderService.addReminder(msg.source, reminderText, timestamp, repeat).unsafeToFuture().map { _ =>
            reply(
              s"Напоминание успешно добавлено: $reminderText на ${new Date(timestamp)}${if (repeat) " (повтор)" else ""}"
            )
          }
        case None =>
          reply("Пожалуйста, укажите корректное время в формате HH:mm!").map(_ => ())
      }
    }
  }

  onCommand("/list") { implicit msg =>
    ReminderService.getAllReminders(msg.source).unsafeToFuture().map { reminders =>
      if (reminders.nonEmpty) {
        val reminderList = reminders.zipWithIndex
          .map { case (reminder, index) =>
            s"${index + 1}. ${reminder.message} на ${new Date(reminder.timestamp)}${
                if (reminder.repeat) " (повтор)" else ""
              }"
          }
          .mkString("\n")
        reply(s"Ваши напоминания:\n$reminderList")
      } else {
        reply("У вас нет активных напоминаний.")
      }
    }
  }

  onCommand("/delete") { implicit msg =>
    withArgs { args =>
      args.headOption match {
        case Some(indexStr) =>
          val index = indexStr.toIntOption.getOrElse(-1)
          if (index > 0) {
            ReminderService.getAllReminders(msg.source).unsafeToFuture().map { reminders =>
              if (index <= reminders.size) {
                val reminderToDelete = reminders(index - 1)
                ReminderService.deleteOneReminder(reminderToDelete.id).unsafeToFuture().map { _ =>
                  reply(s"Напоминание '${reminderToDelete.message}' удалено.")
                }
              } else {
                reply("Напоминание с указанным номером не найдено.")
              }
            }
          } else
            {
              reply("Укажите корректный номер напоминания для удаления.")
            }.void
        case None =>
          reply("Пожалуйста, укажите номер напоминания для удаления. Например: /delete 1").void
      }
    }
  }

  onCommand("/help") { implicit msg =>
    reply(
      """
        |Список доступных команд:
        |
        |/remind <текст> <время HH:mm> [repeat] - добавить напоминание
        |
        |/list - показать список активных напоминаний
        |
        |/delete <номер> - удалить напоминание по его номеру
        |
        |Пример: /remind обеденный перерыв 13:00 repeat
        |""".stripMargin
    ).void
  }

  onCommand("/start") { implicit msg =>
    reply(
      "Здравствуйте, напишите '/help', чтобы увидеть перечень команд "
    ).void
  }
}
