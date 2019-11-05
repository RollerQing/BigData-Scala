package Akka.RpcTest

trait RemoteMsg extends Serializable
// Worker -> Master
case class RegisterWorker(id : String, host : String,
                            port : Int, memory : Int, cores : Int) extends  RemoteMsg

case class Heartbeat(workerId : String) extends  RemoteMsg

//Master -> Worker
case class RegisteredWorker(masterUrl : String) extends RemoteMsg

//Worker -> self
case object SendHeartbeat

//Master -> self
case object CheckTimeOutWorker


