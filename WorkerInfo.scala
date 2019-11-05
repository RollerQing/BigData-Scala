package Akka.RpcTest

/**
 * 用于封装Worker的信息
 * @param id
 * @param host
 * @param port
 * @param memory
 * @param cores
 */
class WorkerInfo(val id : String, val host : String,
                 val port : Int, val memory : Int, val cores : Int) {
    var lastHeartbearTime : Long = _
}
