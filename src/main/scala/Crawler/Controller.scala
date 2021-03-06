package Crawler


import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}

import scala.concurrent.duration._


object Controller {
  case class Check(url: String, depth: Int)
  case class Result(links: Set[String])
}


class Controller extends Actor with ActorLogging {

  import Crawler.Controller._

  var cache = Set.empty[String]
  var children = Set.empty[ActorRef]

  context.setReceiveTimeout(10.seconds)

  def receive = {
    case Check(url, depth) =>
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth > 0)
        children += context.actorOf(Props(new Getter(url, depth - 1)))
      cache += url
    case Getter.Done =>
      children -= sender
      if (children.isEmpty)
        context.parent ! Result(cache)
    case ReceiveTimeout =>
      context.children foreach (_ ! Getter.Abort)
  }

}