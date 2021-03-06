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

package enmasse.systemtest;

import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TopicTest extends VertxTestBase{

    @Test
    public void testMultipleSubscribers() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        scale(dest, 4);
        EnMasseClient client = createTopicClient();
        List<String> msgs = TestUtils.generateMessages(1000);

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()));

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    public void testDurableLinkRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        scale(dest, 4);

        Source source = new TopicTerminusFactory().getSource("locate/" + dest.getAddress());
        source.setDurable(TerminusDurability.UNSETTLED_STATE);

        EnMasseClient client = createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        Future<List<String>> recvResults = client.recvMessages(source, batch1.size());
        assertThat(client.sendMessages(dest.getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat(recvResults.get(1, TimeUnit.MINUTES), is(batch1));

        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat(client.sendMessages(dest.getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        source.setAddress("locate/" + dest.getAddress());
        //at present may get one or more of the first three messages
        //redelivered due to DISPATCH-595, so use more lenient checks
        recvResults = client.recvMessages(source, message -> {
                String body = (String) ((AmqpValue) message.getBody()).getValue();
                System.out.println("received " + body);
                return "twelve".equals(body);
            });
        assertTrue(recvResults.get(1, TimeUnit.MINUTES).containsAll(batch2));
    }

    @Test
    public void testDurableMessageRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("mytopic");
        deploy(dest);
        scale(dest, 4);
        String address = "myaddress";

        EnMasseClient ctrlClient = createQueueClient();
        EnMasseClient client = createTopicClient();

        Message sub = Message.Factory.create();
        sub.setAddress("$subctrl");
        sub.setCorrelationId(address);
        sub.setSubject("subscribe");
        sub.setBody(new AmqpValue(dest.getAddress()));

        ctrlClient.sendMessages("$subctrl", sub).get(5, TimeUnit.MINUTES);

        List<String> msgs = TestUtils.generateMessages(122);
        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        Future<List<String>> recvResult = client.recvMessages(address, 61);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(61));

        // Do scaledown and 'reconnect' receiver and verify that we got everything

        scale(dest, 1);

        recvResult = client.recvMessages(address, 61);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(61));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        sub.setBody(new AmqpValue(dest.getAddress()));
        unsub.setSubject("unsubscribe");
        ctrlClient.sendMessages("$subctrl", unsub).get(5, TimeUnit.MINUTES);
    }
}
