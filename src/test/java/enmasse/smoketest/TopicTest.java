/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.smoketest;

import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TopicTest extends VertxTestBase{

    @After
    public void teardownReplicas() throws InterruptedException {
        TestUtils.setReplicas("mytopic", "mytopic", 1, 10, TimeUnit.MINUTES);
    }

    public void testMultipleSubscribers() throws InterruptedException, TimeoutException, ExecutionException {
        TestUtils.setReplicas("mytopic", "mytopic", 4, 10, TimeUnit.MINUTES);
        String dest = "mytopic";
        waitUntilReady(dest, 5, TimeUnit.MINUTES);
        EnMasseClient client = createTopicClient();
        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest, msgs.size()),
                client.recvMessages(dest, msgs.size()),
                client.recvMessages(dest, msgs.size()));

        assertThat(client.sendMessages(dest, msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    public void testSubscriptionService() throws Exception {
        String topic = "mytopic";
        String address = "myaddress";

        EnMasseClient ctrlClient = createQueueClient();
        EnMasseClient client = createTopicClient();

        waitUntilReady(topic, 5, TimeUnit.MINUTES);
        Message sub = Message.Factory.create();
        sub.setAddress("$subctrl");
        sub.setCorrelationId(address);
        sub.setSubject("subscribe");
        sub.setApplicationProperties(new ApplicationProperties(Collections.singletonMap("root_address", topic)));
        ctrlClient.sendMessages("$subctrl", sub).get(5, TimeUnit.MINUTES);

        Thread.sleep(10000);

        List<String> msgs = Arrays.asList("foo", "bar", "baz");
        Future<List<String>> recvResult = client.recvMessages(address, msgs.size());

        assertThat(client.sendMessages(topic, msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        unsub.setApplicationProperties(new ApplicationProperties(Collections.singletonMap("root_address", topic)));
        unsub.setSubject("unsubscribe");
        ctrlClient.sendMessages("$subctrl", unsub).get(5, TimeUnit.MINUTES);
    }

    public void testScaledown() throws Exception {
        String dest = "mytopic";
        TestUtils.setReplicas(dest, dest, 2, 10, TimeUnit.MINUTES);
        waitUntilReady(dest, 5, TimeUnit.MINUTES);

        EnMasseClient client = createDurableTopicClient();
        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest, msgs.size()),
                client.recvMessages(dest, msgs.size()),
                client.recvMessages(dest, msgs.size()));

        assertThat(client.sendMessages(dest, msgs.subList(0, 2)).get(1, TimeUnit.MINUTES), is(2));

        Thread.sleep(5000);

        TestUtils.setReplicas(dest, dest, 1, 10, TimeUnit.MINUTES);
        waitUntilReady(dest, 5, TimeUnit.MINUTES);

        Thread.sleep(5000);

        assertThat(client.sendMessages(dest, msgs.subList(2, 3)).get(1, TimeUnit.MINUTES), is(1));

        Thread.sleep(5000);
        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }
}