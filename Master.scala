package Akka.RpcTest

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration._
//在Akka中负责监控和创建Actor的老大叫ActorSystem
//负责正在通信的叫Actor
class Master(val masterHost : String, val masterPort : Int)extends  Actor{

  //用来存储Worker的注册信息
  val idToWorker = new mutable.HashMap[String, WorkerInfo]()

  //用来存储Worker的信息
  val workers = new mutable.HashSet[WorkerInfo]()
  val CHECK_INTERVAL = 15000

  //preStart会被调用一次，在构造方法之后，receive方法之前
  override def preStart(): Unit = {
    //在preStart启动一个定时器 用于周期检查超时的Worker
    import  context.dispatcher
    context.system.scheduler.schedule(0 millis, CHECK_INTERVAL millis,
      self, CheckTimeOutWorker)
  }
  override def receive: Receive = {
    // Worker -> Master
    case RegisterWorker(id, host, port, memory, cores) => {
      if(!idToWorker.contains(id)){
        val workerInfo = new WorkerInfo(id, host, port, memory, cores)
        idToWorker += (id -> workerInfo)
        workers += workerInfo
        println("a worker registered")
        sender ! RegisteredWorker(s"akka.tcp://${Master.MASTER_SYSTEM}" +
          s"@$masterHost:$masterPort/user/${Master.MASTER_ACTOR}")
      }
    }
    case Heartbeat(workerId) => {
      val workerInfo = idToWorker(workerId)
      val current_time = System.currentTimeMillis()
      //更新最后一次心跳时间
      workerInfo.lastHeartbearTime = current_time
    }

    case CheckTimeOutWorker => {
      val currentTime = System.currentTimeMillis()
      val toRemove : mutable.HashSet[WorkerInfo] = workers
        .filter(w => currentTime - w.lastHeartbearTime > CHECK_INTERVAL)
      toRemove.foreach(deadWorker => {
        //将超时的worker从内存中移除掉
        idToWorker -= deadWorker.id
        workers -= deadWorker
      })
      println("num of workers " + workers.size)
    }
  }
}


object Master {
  val MASTER_SYSTEM = "MasterSystem"
  val MASTER_ACTOR = "Master"

  def main(args: Array[String]): Unit = {
    //如果想创建Actor 必须先创建它的老大 ActorSystem(单例的)

//    val host = args(0)
//    val port = args(1).toInt
    val host = "127.0.0.1"
    val port = 9999
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
         |""".stripMargin
    val config = ConfigFactory.parseString(configStr)

    //先创建一个ActorSystem， 单例
    val actorSystem: ActorSystem = ActorSystem(MASTER_SYSTEM, config)
    actorSystem.actorOf(Props(new Master(host, port)), MASTER_ACTOR)
    actorSystem.awaitTermination()
  }
}
