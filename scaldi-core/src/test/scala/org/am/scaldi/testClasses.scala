package org.am.scaldi

trait Server

case class HttpServer(host: String, port: Int) extends Server