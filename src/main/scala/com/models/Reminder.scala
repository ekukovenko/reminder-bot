package com.models

case class Reminder(id: Long, message: String, userId: Long, timestamp: Long, repeat: Boolean)
