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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for {@code org.apache.camel.upgrade.undertow.MigrateToUndertowSpringBootStarter}, the recipe chained
 * from the Camel Spring Boot 4.22 migration that completes the move from Spring Boot 3's Undertow integration
 * to {@code io.undertow:undertow-spring-boot-starter}.
 */
class UndertowMigrationTest implements RewriteTest {

    // Minimal stand-ins for the Spring Boot 3 Undertow API, which is not on the test classpath.
    private static final String UNDERTOW_BUILDER_CUSTOMIZER_STUB = """
            package org.springframework.boot.web.embedded.undertow;
            public interface UndertowBuilderCustomizer {
                void customize(Object builder);
            }
            """;

    private static final String UNDERTOW_SERVLET_FACTORY_STUB = """
            package org.springframework.boot.web.embedded.undertow;
            public class UndertowServletWebServerFactory {
                public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
                }
            }
            """;

    private static final String SERVER_PROPERTIES_STUB = """
            package org.springframework.boot.autoconfigure.web;
            public class ServerProperties {
                public Undertow getUndertow() {
                    return new Undertow();
                }
                public static class Undertow {
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
                        .scanYamlResources()
                        .build()
                        .activateRecipes("org.apache.camel.upgrade.undertow.MigrateToUndertowSpringBootStarter"))
                .parser(JavaParser.fromJavaVersion()
                        .logCompilationWarningsAndErrors(true)
                        .dependsOn(UNDERTOW_BUILDER_CUSTOMIZER_STUB, UNDERTOW_SERVLET_FACTORY_STUB,
                                SERVER_PROPERTIES_STUB));
    }

    @DocumentExample
    @Test
    void migratesDirectUndertowDependenciesToUndertowEE() {
        //language=xml
        rewriteRun(
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <dependencies>
                                <dependency>
                                    <groupId>io.undertow</groupId>
                                    <artifactId>undertow-core</artifactId>
                                    <version>2.3.18.Final</version>
                                </dependency>
                                <dependency>
                                    <groupId>io.undertow</groupId>
                                    <artifactId>undertow-servlet</artifactId>
                                    <version>2.3.18.Final</version>
                                </dependency>
                                <dependency>
                                    <groupId>io.undertow</groupId>
                                    <artifactId>undertow-websockets-jsr</artifactId>
                                    <version>2.3.18.Final</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <dependencies>
                                <dependency>
                                    <groupId>io.undertow</groupId>
                                    <artifactId>undertow-core</artifactId>
                                    <version>2.3.18.Final</version>
                                </dependency>
                                <dependency>
                                    <groupId>io.undertow.ee</groupId>
                                    <artifactId>undertow-servlet</artifactId>
                                    <version>2.0.2.Final</version>
                                </dependency>
                                <dependency>
                                    <groupId>io.undertow.ee</groupId>
                                    <artifactId>undertow-websockets</artifactId>
                                    <version>2.0.2.Final</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """
                )
        );
    }

    @Test
    void removesUnsupportedPropertiesAndKeepsDeprecatedKeys() {
        //language=properties
        rewriteRun(
                properties(
                        """
                        server.port=8080
                        server.undertow.buffers-per-region=1024
                        server.undertow.allow-encoded-slash=true
                        server.undertow.threads.io=4
                        """,
                        """
                        server.port=8080
                        # server.undertow.allow-encoded-slash has no replacement in undertow-spring-boot-starter; review undertow.server.decode-slash instead
                        # server.undertow.allow-encoded-slash=true
                        server.undertow.threads.io=4
                        """
                )
        );
    }

    @Test
    void removesUnsupportedYamlKeysAndKeepsDeprecatedKeys() {
        //language=yaml
        rewriteRun(
                yaml(
                        """
                        server:
                          port: 8080
                          undertow:
                            buffers-per-region: 1024
                            allow-encoded-slash: true
                            threads:
                              io: 4
                        """,
                        """
                        server:
                          port: 8080
                          undertow:
                            # server.undertow.allow-encoded-slash has no replacement in undertow-spring-boot-starter; review undertow.server.decode-slash instead
                            # allow-encoded-slash: true
                            threads:
                              io: 4
                        """
                )
        );
    }

    @Test
    void migratesUndertowJavaApiAndMarksServerPropertiesUndertow() {
        //language=java
        rewriteRun(
                java(
                        """
                        import org.springframework.boot.autoconfigure.web.ServerProperties;
                        import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
                        import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;

                        class UndertowConfig {

                            UndertowBuilderCustomizer customizer() {
                                return builder -> {
                                };
                            }

                            void customize(UndertowServletWebServerFactory factory, ServerProperties properties) {
                                factory.addBuilderCustomizers(customizer());
                                ServerProperties.Undertow undertow = properties.getUndertow();
                            }
                        }
                        """,
                        """
                        import io.undertow.springboot.UndertowBuilderCustomizer;
                        import io.undertow.springboot.servlet.UndertowServletWebServerFactory;
                        import org.springframework.boot.autoconfigure.web.ServerProperties;

                        class UndertowConfig {

                            UndertowBuilderCustomizer customizer() {
                                return builder -> {
                                };
                            }

                            void customize(UndertowServletWebServerFactory factory, ServerProperties properties) {
                                factory.addBuilderCustomizers(customizer());
                                /*~~>*/ServerProperties.Undertow undertow = /*~~>*/properties.getUndertow();
                            }
                        }
                        """
                )
        );
    }
}
