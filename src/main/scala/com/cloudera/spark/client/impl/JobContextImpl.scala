/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.spark.client.impl

import org.apache.spark.{FutureAction, SparkContext}

import com.cloudera.spark.client._

class JobContextImpl(ctx: SparkContext) extends JobContext {

  private val monitorCb = new ThreadLocal[FutureAction[_] => Unit]()

  override def sc: SparkContext = ctx

  override def monitor[T](job: FutureAction[T]) = {
    monitorCb.get()(job)
    job
  }

  private[impl] def setMonitorCb(cb: FutureAction[_] => Unit) = monitorCb.set(cb)

  private[impl] def stop() = {
    sc.stop()
  }

}
