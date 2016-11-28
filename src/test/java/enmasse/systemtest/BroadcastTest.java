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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class BroadcastTest extends VertxTestBase {

    @Test
    public void testMultipleRecievers() throws Exception {
        Destination dest = Destination.broadcast("broadcast");
        deploy(dest);
        EnMasseClient client = createTopicClient();
        List<String> msgs = Arrays.asList("foo");

        List<Future<List<String>>> recvResults = Arrays.asList(
            client.recvMessages(dest.getAddress(), msgs.size()),
            client.recvMessages(dest.getAddress(), msgs.size()),
            client.recvMessages(dest.getAddress(), msgs.size()));

        long end = System.currentTimeMillis() + 60_000;
        boolean isDone = false;
        while (System.currentTimeMillis() < end && !isDone) {
            assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));
            Thread.sleep(1000);
            isDone = recvResults.get(0).isDone() && recvResults.get(1).isDone() && recvResults.get(2).isDone();
        }

        assertTrue(recvResults.get(0).get(1, TimeUnit.MINUTES).size() >= msgs.size());
        assertTrue(recvResults.get(1).get(1, TimeUnit.MINUTES).size() >= msgs.size());
        assertTrue(recvResults.get(2).get(1, TimeUnit.MINUTES).size() >= msgs.size());
    }
}