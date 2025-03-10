import cats.effect.unsafe.IORuntime
import com.bot.RemindBot
import com.schedulers.ReminderScheduler

import scala.concurrent.ExecutionContext.global
import scala.io.StdIn

object Main extends App {
  val token = "your_token"

  implicit val runtime: IORuntime = IORuntime.global

  val bot = new RemindBot(token)(global, runtime)
  ReminderScheduler.start(bot)

  bot.run()

  println("Бот запущен. Нажмите [Enter] для завершения.")
  StdIn.readLine()

  bot.shutdown()
  ReminderScheduler.stop()
}
