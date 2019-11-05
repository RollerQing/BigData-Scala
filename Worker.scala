package Akka.RpcTest

import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
class Worker(val host : String , val port : Int, val masterHost : String,
             val masterPort : Int, val memory : Int,  val cores : Int) extends Actor {
  val worker_id = UUID.randomUUID().toString
  var masterUrl: String = _
  val HEARTBEAT_INTERVAL = 10000
  var master : ActorSelection = _

  override def preStart(): Unit = {
    master = context.actorSelection(s"akka.tcp://${Master.MASTER_SYSTEM}" +
      s"@$masterHost:$masterPort/user/${Master.MASTER_ACTOR}")
    master ! RegisterWorker(worker_id, host, port, memory, cores)
  }
  override def receive: Receive = {
    //Worker接收到Master注册成功的反馈信息
    case RegisteredWorker(masterUrl) => {
      this.masterUrl = masterUrl
      //定时发送心跳， 心跳是一个case class
      //导入一个隐式转换， 才能启动定时器
      import context.dispatcher
      context.system.scheduler.schedule(0 millis, HEARTBEAT_INTERVAL millis,
        self, SendHeartbeat)
    }
    case SendHeartbeat => {
      //发送心跳之前要进行一些检查
      master ! Heartbeat(worker_id)
    }
  }
}

object Worker {
  val WORKER_SYSTEM = "WorkerSystem"
  val WORKER_ACTOR = "Worker"

  def main(args: Array[String]): Unit = {
//    val host = args(0)
//    val port = args(1).toInt
//    val masterHost = args(2)
//    val masterPort = args(3).toInt
//    val memory = args(4).toInt
//    val cores = args(5).toInt
    val host = "127.0.0.1"
    val port = 8888
    val masterHost = "127.0.0.1"
    val masterPort = 9999

    val memory = 512
    val cores = 2

    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
         |""".stripMargin
    val config = ConfigFactory.parseString(configStr)
    //先创建一个ActorSystem， 单例
    val actorSystem: ActorSystem = ActorSystem(WORKER_SYSTEM, config)
    actorSystem.actorOf(Props(new Worker(host, port,
      masterHost,masterPort, memory, cores)), WORKER_ACTOR)
    actorSystem.awaitTermination()
  }
}
