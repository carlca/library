package com.carlca.logger

import com.carlca.config.Config
import com.carlca.utils.ConsoleUtils

import java.io.*
import java.net.ServerSocket
import scala.util.Try
import scala.util.Using

object Receiver:

  def main(args: Array[String]): Unit = try
    val port         = findFreePort.get
    val serverSocket = new ServerSocket(port)
    outputMessage("Receiver listening on port " + port)
    outputMessage("")
    Config.setLogPort(port)
    // handle ctrl/c
    Runtime.getRuntime.addShutdownHook(
      new Thread(() =>
        try
          Config.setLogPort(0)
          outputMessage("Receiver port reset to 0")
          serverSocket.close
        catch
          case e: IOException =>
            System.exit(0)
    ))
    while !Thread.interrupted do
      val socket      = serverSocket.accept
      val inputStream = socket.getInputStream
      val reader      = new BufferedReader(new InputStreamReader(inputStream))
      val message     = reader.readLine
      outputMessage(message)
      reader.close
    serverSocket.close
  catch
    case e: IOException =>
      Config.setLogPort(0)
      System.exit(0)

  private def outputMessage(msg: String): Unit =
    if ConsoleUtils.hasFormattingPlaceholders(msg) then printf(msg)
    else System.out.println(msg)

  private def findFreePort: Try[Int] = Using(new ServerSocket(0))(_.getLocalPort)
