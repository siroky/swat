package swat.js.workers

import swat.js.communication.MessageExchanger

trait DedicatedWorkerGlobalScope extends WorkerGlobalScope with MessageExchanger
