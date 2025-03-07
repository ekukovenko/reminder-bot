import cats.effect.unsafe.IORuntime
import com.bot.RemindBot
import com.schedulers.ReminderScheduler

import scala.concurrent.ExecutionContext.global
import scala.io.StdIn

object Main extends App {
  val token = "7288543748:AAH8iPXHJkkfGu7B8Lk60isL6DirDT0qGE0"

  implicit val runtime: IORuntime = IORuntime.global

  val bot = new RemindBot(token)(global, runtime)
  ReminderScheduler.start(bot)

  bot.run()

  println("Бот запущен. Нажмите [Enter] для завершения.")
  StdIn.readLine()

  bot.shutdown()
  ReminderScheduler.stop()
}
