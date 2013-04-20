package swat.api.js.workers

import swat.api.js.communication.MessageExchanger

trait DedicatedWorkerGlobalScope extends WorkerGlobalScope with MessageExchanger

object DedicatedWorkerGlobalScope extends DedicatedWorkerGlobalScope
