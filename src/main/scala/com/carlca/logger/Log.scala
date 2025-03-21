package com.carlca
package logger

import java.io.*
import java.net.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import config.Config
import config.OS

object Log:
  private var writer: Option[BufferedWriter] = None
  private[logger] var socket: Option[Socket] = None
  cls

  def cls: this.type = send("\u001B[H\u001B[2J")

  def blank: this.type = send("")

  def line: this.type = send(String.valueOf('-').repeat(80))

  def time: this.type = send(LocalTime.now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))

  def send(msg: String, args: Any*): this.type =
    if initSockets then
      sendMessage(if (args.isEmpty) msg else s"$msg: ${args.mkString(", ")}").closeSockets
    this

  def sendColor(msg: String, r: Int, g: Int, b: Int): this.type =
    if initSockets then
      sendMessage(s"\u001B[38;2;${r};${g};${b}m$msg\u001B[0m").closeSockets
    this

  private def sendMessage(msg: String): this.type =
    if writer != null then
      // check for last character = '~' and remove it
      if msg.endsWith("~") then
        writer.get.write(msg.dropRight(1))
      else
        writer.get.write(msg + System.lineSeparator)
      writer.get.flush()
    this

  private def initSockets: Boolean =
    if Config.getOs == OS.WINDOWS then return false
    val port = Config.getLogPort
    if port > 0 then
      socket = Some(new Socket("localhost", port))
      val outputStream = socket.get.getOutputStream
      writer = Some(new BufferedWriter(new OutputStreamWriter(outputStream)))
    port > 0

  private def closeSockets: Unit =
    socket = socket.map(_.close()).map(_ => None).getOrElse(None)
    writer = writer.map(_.close()).map(_ => None).getOrElse(None)

end Log
