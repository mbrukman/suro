/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.suro.queue;

import com.google.inject.Inject;
import com.netflix.suro.server.ServerConfig;
import com.netflix.suro.thrift.TMessageSet;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Queue4Server {
    static Logger logger = LoggerFactory.getLogger(Queue4Server.class);

    private BlockingQueue<TMessageSet> queue;
    private boolean isFile;

    @Inject
    public Queue4Server(ServerConfig config) {
        if (config.getQueueType().equals("memory")) {
            queue = new ArrayBlockingQueue<TMessageSet>(config.getQueueSize());
        } else {
            isFile = true;
            try {
                queue = new FileBlockingQueue<TMessageSet>(
                        config.getFileQueuePath(),
                        config.getFileQueueName(),
                        new Period(config.getFileQueueGCPeriod()).toStandardSeconds().getSeconds(),
                        new MessageSetSerDe(),
                        true); // auto-commit needed due to MessageSet consumers are multithreaded
            } catch (IOException e) {
                logger.error("Exception on initializing Queue4Client: " + e.getMessage(), e);
            }
        }
    }

    public boolean offer(TMessageSet msg) {
        return queue.offer(msg);
    }

    public TMessageSet poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return queue.poll(timeout, timeUnit);
    }

    public int drain(int batchSize, List<TMessageSet> msgList) {
        return queue.drainTo(msgList, batchSize);
    }

    public void close() {
        if (isFile) {
            ((FileBlockingQueue<TMessageSet>) queue).close();
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public long size() {
        return queue.size();
    }
}