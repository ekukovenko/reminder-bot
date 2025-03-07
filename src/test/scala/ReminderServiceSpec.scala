import cats.effect.unsafe.IORuntime
import com.repositories.ReminderRepository
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.services.ReminderService

import scala.concurrent.ExecutionContext

class ReminderServiceSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime = IORuntime.global

  override def beforeAll(): Unit = {
    ReminderRepository.initializeSchema(ec).unsafeRunSync()
  }

  override def beforeEach(): Unit = {
    ReminderRepository.deleteAllReminders(ec).unsafeRunSync()
    ReminderRepository.resetIdSequence(ec).unsafeRunSync()
  }

  "ReminderService" should "add and retrieve reminders correctly" in {
    ReminderService.addReminder(1L, "Test message", 1234567890L, repeat = false).unsafeRunSync()
    val reminders = ReminderService.getAllReminders(1L).unsafeRunSync()

    reminders should have size 1
    reminders.head.message shouldBe "Test message"

    ReminderService.deleteOneReminder(reminders.head.id).unsafeRunSync()
  }

  it should "retrieve all unique user IDs" in {
    ReminderService.addReminder(1L, "Message 1", 1234567890L, repeat = false).unsafeRunSync()
    ReminderService.addReminder(2L, "Message 2", 1234567891L, repeat = true).unsafeRunSync()

    val userIds = ReminderService.getAllUserIds().unsafeRunSync()
    userIds should contain allOf (1L, 2L)
  }

  it should "update the timestamp of a reminder" in {
    ReminderService.addReminder(1L, "Message 1", 1234567890L, repeat = false).unsafeRunSync()
    val remindersBeforeUpdate = ReminderService.getAllReminders(1L).unsafeRunSync()
    val reminderId = remindersBeforeUpdate.head.id

    ReminderService.updateReminderTimestamp(reminderId, 9876543210L).unsafeRunSync()
    val remindersAfterUpdate = ReminderService.getAllReminders(1L).unsafeRunSync()

    remindersAfterUpdate should have size 1
    remindersAfterUpdate.head.timestamp shouldBe 9876543210L
  }

  it should "delete a reminder successfully" in {
    ReminderService.addReminder(1L, "Message to delete", 1234567890L, repeat = false).unsafeRunSync()
    val remindersBeforeDelete = ReminderService.getAllReminders(1L).unsafeRunSync()
    val reminderId = remindersBeforeDelete.head.id

    ReminderService.deleteOneReminder(reminderId).unsafeRunSync()
    val remindersAfterDelete = ReminderService.getAllReminders(1L).unsafeRunSync()

    remindersAfterDelete shouldBe empty
  }

  it should "reset ID sequence after clearing the table" in {
    ReminderService.addReminder(1L, "First reminder", 1234567890L, repeat = false).unsafeRunSync()

    ReminderRepository.deleteAllReminders(ec).unsafeRunSync()
    ReminderRepository.resetIdSequence(ec).unsafeRunSync()

    ReminderService.addReminder(1L, "Second reminder", 1234567891L, repeat = true).unsafeRunSync()
    val secondReminder = ReminderService.getAllReminders(1L).unsafeRunSync().head

    secondReminder.id shouldBe 1
  }
}
