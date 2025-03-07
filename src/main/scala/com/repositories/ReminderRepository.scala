package com.repositories

import cats.effect.{IO, Resource}
import com.config.DatabaseConfig
import com.models.Reminder
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import scala.concurrent.ExecutionContext

object ReminderRepository {

  private def transactor(ec: ExecutionContext): Resource[IO, HikariTransactor[IO]] = DatabaseConfig.createTransactor(ec)

  implicit val reminderRead: Read[Reminder] = Read[(Long, String, Long, Long, Boolean)].map {
    case (id, message, userId, timestamp, repeat) =>
      Reminder(id, message, userId, timestamp, repeat)
  }

  implicit val reminderWrite: Write[Reminder] = Write[(String, Long, Long, Boolean)].contramap { reminder =>
    (reminder.message, reminder.userId, reminder.timestamp, reminder.repeat)
  }

  def saveReminder(ec: ExecutionContext, reminder: Reminder): IO[Int] = {
    transactor(ec).use { xa =>
      for {
        count <- sql"SELECT COUNT(*) FROM reminders".query[Int].unique.transact(xa)
        _ <-
          if (count == 0) {
            sql"ALTER SEQUENCE reminders_id_seq RESTART WITH 1".update.run
              .transact(xa)
              .handleErrorWith(_ => IO.unit)
            sql"ALTER TABLE reminders ALTER COLUMN id RESTART WITH 1".update.run
              .transact(xa)
              .handleErrorWith(_ => IO.unit)
          } else IO.unit
        result <- sql"""
        INSERT INTO reminders (message, user_id, timestamp, repeat)
        VALUES (${reminder.message}, ${reminder.userId}, ${reminder.timestamp}, ${reminder.repeat})
      """.update.run.transact(xa)
      } yield result
    }
  }

  def deleteReminder(ec: ExecutionContext, reminderId: Long): IO[Int] = {
    transactor(ec).use { xa =>
      sql"""
        DELETE FROM reminders WHERE id = $reminderId
      """.update.run.transact(xa)
    }
  }

  def getReminders(ec: ExecutionContext, userId: Long): IO[List[Reminder]] = {
    transactor(ec).use { xa =>
      sql"""
        SELECT id, message, user_id, timestamp, repeat
        FROM reminders
        WHERE user_id = $userId
      """.query[Reminder].to[List].transact(xa)
    }
  }

  def getUserIds(ec: ExecutionContext): IO[List[Long]] = {
    transactor(ec).use { xa =>
      sql"""
        SELECT DISTINCT user_id FROM reminders
      """.query[Long].to[List].transact(xa)
    }
  }

  def updateTimestamp(ec: ExecutionContext, reminderId: Long, newTimestamp: Long): IO[Int] = {
    transactor(ec).use { xa =>
      sql"""
        UPDATE reminders SET timestamp = $newTimestamp WHERE id = $reminderId
      """.update.run.transact(xa)
    }
  }

  def initializeSchema(ec: ExecutionContext): IO[Unit] = {
    transactor(ec).use { xa =>
      for {
        _ <- sql"""
        CREATE TABLE IF NOT EXISTS reminders (
          id SERIAL PRIMARY KEY,
          message VARCHAR(255) NOT NULL,
          user_id BIGINT NOT NULL,
          timestamp BIGINT NOT NULL,
          repeat BOOLEAN NOT NULL
        )
      """.update.run.transact(xa)
        _ <- sql"""
        CREATE SEQUENCE IF NOT EXISTS reminders_id_seq START WITH 1 INCREMENT BY 1
      """.update.run.transact(xa)
      } yield ()
    }
  }

  def resetIdSequence(ec: ExecutionContext): IO[Unit] = {
    transactor(ec).use { xa =>
      sql"ALTER SEQUENCE reminders_id_seq RESTART WITH 1".update.run.transact(xa).map(_ => ())
    }
  }

  def deleteAllReminders(ec: ExecutionContext): IO[Int] = {
    transactor(ec).use { xa =>
      sql"DELETE FROM reminders".update.run.transact(xa)
    }
  }
}
