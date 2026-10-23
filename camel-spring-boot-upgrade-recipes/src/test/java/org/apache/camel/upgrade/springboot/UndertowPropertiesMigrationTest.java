/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.upgrade.springboot;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for the opt-in {@code org.apache.camel.upgrade.undertow.MigrateUndertowProperties} recipe, which renames
 * the deprecated {@code server.undertow.*} and {@code management.server.undertow.*} keys that
 * {@code undertow-spring-boot-starter} still accepts. Camel's own
 * {@code camel.component.platform-http.server.undertow.*} keys must not be touched.
 * <p>
 * In YAML the moved keys are written as dotted top-level keys ({@code undertow.server:}), which Spring Boot binds
 * exactly like a nested {@code undertow: server:} block.
 */
class UndertowPropertiesMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
                .scanYamlResources()
                .build()
                .activateRecipes("org.apache.camel.upgrade.undertow.MigrateUndertowProperties"));
    }

    @DocumentExample
    @Test
    void renamesDeprecatedUndertowProperties() {
        //language=properties
        rewriteRun(
                properties(
                        """
                        server.port=8080
                        server.undertow.threads.io=4
                        server.undertow.accesslog.enabled=true
                        server.undertow.options.server.ENABLE_STATISTICS=true
                        server.undertow.buffers-per-region=1024
                        management.server.port=9090
                        management.server.undertow.accesslog.prefix=mgmt_
                        camel.component.platform-http.server.undertow.accesslog.use-camel-logging=true
                        """,
                        """
                        server.port=8080
                        undertow.server.threads.io=4
                        undertow.server.accesslog.enabled=true
                        undertow.server.options.server.ENABLE_STATISTICS=true
                        management.server.port=9090
                        undertow.management.accesslog.prefix=mgmt_
                        camel.component.platform-http.server.undertow.accesslog.use-camel-logging=true
                        """
                )
        );
    }

    @Test
    void renamesDeprecatedUndertowYamlKeys() {
        //language=yaml
        rewriteRun(
                yaml(
                        """
                        server:
                          port: 8080
                          undertow:
                            threads:
                              io: 4
                            accesslog:
                              enabled: true
                        management:
                          server:
                            port: 9090
                            undertow:
                              accesslog:
                                prefix: mgmt_
                        camel:
                          component:
                            platform-http:
                              server:
                                undertow:
                                  accesslog:
                                    use-camel-logging: true
                        """,
                        """
                        server:
                          port: 8080
                        management:
                          server:
                            port: 9090
                        camel:
                          component:
                            platform-http:
                              server:
                                undertow:
                                  accesslog:
                                    use-camel-logging: true
                        undertow.management:
                          accesslog:
                            prefix: mgmt_
                        undertow.server:
                          threads:
                            io: 4
                          accesslog:
                            enabled: true
                        """
                )
        );
    }
}
