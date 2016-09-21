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

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IService;

public class Environment {
    public static final String user = System.getenv("OPENSHIFT_USER");
    public static final String token = System.getenv("OPENSHIFT_TOKEN");
    public static final String url = System.getenv("OPENSHIFT_URL");
    public static final String namespace = "enmasse-ci";
    public static final IClient client = new ClientBuilder(url).usingToken(token).withUserName(user).build();
    public static final IService service = client.get(ResourceKind.SERVICE, "messaging", namespace);
    public static final Endpoint endpoint = new Endpoint(service.getPortalIP(), service.getPort());
    //public static final Endpoint endpoint = new Endpoint("172.30.56.135", 5672);
}