package com.config

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor

import java.sql.Connection
import scala.concurrent.ExecutionContext

object DatabaseConfig {
  private val isTestEnvironment: Boolean = sys.props.getOrElse("env", "prod") == "test"

  private val config = new HikariConfig()
  if (isTestEnvironment) {
    Class.forName("org.h2.Driver")
    config.setJdbcUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
    config.setUsername("sa")
    config.setPassword("")
  } else {
    Class.forName("org.postgresql.Driver")
    config.setJdbcUrl("jdbc:postgresql://db:5432/postgres_db")
    config.setUsername("postgres_user")
    config.setPassword("postgres_pass")
  }
  config.setMaximumPoolSize(10)

  private val dataSource = new HikariDataSource(config)

  def getConnection: Connection = dataSource.getConnection

  def createTransactor(ec: ExecutionContext): Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.fromHikariConfig[IO](config)
  }
}
