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
package org.apache.camel.upgrade;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Tests for migrating from Camel 4.18.1 to 4.18.3.
 * Most changes are header renames that were introduced in 4.18.x and reused from 4.21 recipes.
 */
public class CamelUpdate418_3Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_18_3)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_18_1, "camel-api",
                        "camel-core-model", "camel-support"))
                .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void testJiraHeadersMigrationJava() {
        String jiraPom = """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <maven.compiler.release>17</maven.compiler.release>
                            </properties>
                            <repositories>
                                <repository>
                                    <id>atlassian</id>
                                    <url>https://packages.atlassian.com/artifactory/maven-atlassian-all/</url>
                                    <name>atlassian external repo</name>
                                    <snapshots>
                                        <enabled>false</enabled>
                                    </snapshots>
                                    <releases>
                                        <enabled>true</enabled>
                                    </releases>
                                </repository>
                            </repositories>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-jira</artifactId>
                                    <version>%s</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """.formatted(CamelTestUtil.getCamelLatestVersion());
        //language=java
        rewriteRun(
                mavenProject("test-jira",
                        pomXml(jiraPom),
                        java(
                        """
                        import org.apache.camel.Exchange;
                        import org.apache.camel.builder.RouteBuilder;

                        class Test extends RouteBuilder {
                            public void configure() {
                                from("direct:start")
                                    .process(exchange -> {
                                        exchange.getIn().setHeader("IssueKey", "CAMEL-12345");
                                        exchange.getIn().setHeader("IssueSummary", "Bug fix");
                                        exchange.getIn().setHeader("ProjectKey", "CAMEL");
                                        exchange.getIn().setHeader("linkType", "Relates");
                                        exchange.getIn().setHeader("minutesSpent", 30);
                                    })
                                    .to("jira:addIssue");
                            }
                        }
                        """,
                        """
                        import org.apache.camel.Exchange;
                        import org.apache.camel.builder.RouteBuilder;

                        class Test extends RouteBuilder {
                            public void configure() {
                                from("direct:start")
                                    .process(exchange -> {
                                        exchange.getIn().setHeader("CamelJiraIssueKey", "CAMEL-12345");
                                        exchange.getIn().setHeader("CamelJiraIssueSummary", "Bug fix");
                                        exchange.getIn().setHeader("CamelJiraIssueProjectKey", "CAMEL");
                                        exchange.getIn().setHeader("CamelJiraLinkType", "Relates");
                                        exchange.getIn().setHeader("CamelJiraMinutesSpent", 30);
                                    })
                                    .to("jira:addIssue");
                            }
                        }
                        """
                )
                )
        );
    }

}
