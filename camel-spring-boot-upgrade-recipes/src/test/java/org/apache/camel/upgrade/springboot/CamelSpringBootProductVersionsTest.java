/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.upgrade.springboot;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.apache.camel.upgrade.ProductVersionTestSupport.CSB;
import static org.apache.camel.upgrade.ProductVersionTestSupport.CORE;
import static org.apache.camel.upgrade.ProductVersionTestSupport.KAMELETS;
import static org.apache.camel.upgrade.ProductVersionTestSupport.configure;
import static org.apache.camel.upgrade.ProductVersionTestSupport.project;
import static org.openrewrite.maven.Assertions.pomXml;

class CamelSpringBootProductVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        configure(spec);
        spec.recipeFromResources("org.apache.camel.upgrade.CamelSpringBootMigrationRecipe");
    }

    @Test
    void fullSpringBootRecipeAlignsAllGroupsAndBomIndependently() {
        String before = project("", """
            <properties><maven.compiler.release>17</maven.compiler.release></properties>
            <dependencyManagement><dependencies><dependency>
              <groupId>com.redhat.camel.springboot.platform</groupId><artifactId>camel-spring-boot-bom</artifactId><version>4.14.2</version><type>pom</type><scope>import</scope>
            </dependency></dependencies></dependencyManagement>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>4.22.1</version></dependency>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-vertx</artifactId><version>4.22.1</version></dependency>
              <dependency><groupId>org.apache.camel.springboot</groupId><artifactId>camel-timer-starter</artifactId><version>4.22.0</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>4.21.0</version></dependency>
            </dependencies>
            """);
        String after = before.replace("4.14.2", CSB)
                .replace("<version>4.22.1</version>", "")
                .replace("<version>4.21.0</version>", "")
                .replace("<version>4.22.0</version>", "<version>" + CSB + "</version>");
        rewriteRun(pomXml(before, after));
    }

    @Test
    void fullSpringBootRecipeAlignsExplicitCoreAndKamelets() {
        String before = project("", """
            <properties><maven.compiler.release>17</maven.compiler.release></properties>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>4.22.1</version></dependency>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-vertx</artifactId><version>4.22.1</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>4.21.0</version></dependency>
            </dependencies>
            """);
        String after = before.replace("<version>4.22.1</version>", "<version>" + CORE + "</version>")
                .replace("<version>4.21.0</version>", "<version>" + KAMELETS + "</version>");
        rewriteRun(pomXml(before, after));
    }

}
