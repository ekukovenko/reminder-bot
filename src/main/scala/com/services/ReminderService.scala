package com.services

import cats.effect.IO
import com.models.Reminder
import com.repositories.ReminderRepository

import scala.concurrent.ExecutionContext

object ReminderService {
  def addReminder(userId: Long, message: String, timestamp: Long, repeat: Boolean)(implicit
    ec: ExecutionContext
  ): IO[Unit] = {
    val reminder = Reminder(0, message, userId, timestamp, repeat)
    ReminderRepository.saveReminder(ec, reminder).map(_ => ())
  }

  def getAllReminders(userId: Long)(implicit ec: ExecutionContext): IO[List[Reminder]] = {
    ReminderRepository.getReminders(ec, userId).recover { case _: Exception =>
      List()
    }
  }

  def getAllUserIds()(implicit ec: ExecutionContext): IO[List[Long]] = {
    ReminderRepository.getUserIds(ec)
  }

  def updateReminderTimestamp(reminderId: Long, newTimestamp: Long)(implicit ec: ExecutionContext): IO[Unit] = {
    ReminderRepository.updateTimestamp(ec, reminderId, newTimestamp).map(_ => ())
  }

  def deleteOneReminder(reminderId: Long)(implicit ec: ExecutionContext): IO[Int] = {
    ReminderRepository.deleteReminder(ec, reminderId)
  }
}
