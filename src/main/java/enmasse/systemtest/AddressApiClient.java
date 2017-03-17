package enmasse.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.json.Json.mapper;

public class AddressApiClient {
    private final HttpClient httpClient;
    private final Endpoint endpoint;
    private final boolean isMultitenant;

    public AddressApiClient(Vertx vertx, Endpoint endpoint, boolean isMultitenant) {
        this.httpClient = vertx.createHttpClient();
        this.endpoint = endpoint;
        this.isMultitenant = isMultitenant;
    }

    public void close() {
        httpClient.close();
    }

    public void deployInstance(String instanceName) throws JsonProcessingException, InterruptedException {
        if (isMultitenant) {
            ObjectNode config = mapper.createObjectNode();
            config.put("apiVersion", "v3");
            config.put("kind", "Instance");
            ObjectNode metadata = config.putObject("metadata");
            metadata.put("name", instanceName);
            ObjectNode spec = config.putObject("spec");
            spec.put("namespace", instanceName);

            CountDownLatch latch = new CountDownLatch(1);
            HttpClientRequest request;
            request = httpClient.post(endpoint.getPort(), endpoint.getHost(), "/v3/instance");
            request.putHeader("content-type", "application/json");
            request.handler(event -> {
                if (event.statusCode() >= 200 && event.statusCode() < 300) {
                    latch.countDown();
                }
            });
            request.end(Buffer.buffer(mapper.writeValueAsBytes(config)));
            latch.await(30, TimeUnit.SECONDS);
        }
    }

    public void deploy(String instanceName, Destination ... destinations) throws Exception {
        ObjectNode config = mapper.createObjectNode();
        config.put("apiVersion", "v3");
        config.put("kind", "AddressList");
        ArrayNode items = config.putArray("items");
        for (Destination destination : destinations) {
            ObjectNode entry = items.addObject();
            ObjectNode metadata = entry.putObject("metadata");
            metadata.put("name", destination.getAddress());
            ObjectNode spec = entry.putObject("spec");
            spec.put("store_and_forward", destination.isStoreAndForward());
            spec.put("multicast", destination.isMulticast());
            spec.put("group", destination.getGroup());
            destination.getFlavor().ifPresent(e -> spec.put("flavor", e));
        }

        CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest request;
        if (isMultitenant) {
            request = httpClient.put(endpoint.getPort(), endpoint.getHost(), "/v3/instance/" + instanceName + "/address");
        } else {
            request = httpClient.put(endpoint.getPort(), endpoint.getHost(), "/v3/address");
        }
        request.putHeader("content-type", "application/json");
        request.handler(event -> {
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            }
        });
        request.end(Buffer.buffer(mapper.writeValueAsBytes(config)));
        latch.await(30, TimeUnit.SECONDS);
    }
}