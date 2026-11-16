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
package org.apache.camel.upgrade;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.apache.camel.upgrade.ProductVersionTestSupport.CORE;
import static org.apache.camel.upgrade.ProductVersionTestSupport.CSB;
import static org.apache.camel.upgrade.ProductVersionTestSupport.KAMELETS;
import static org.apache.camel.upgrade.ProductVersionTestSupport.configure;
import static org.apache.camel.upgrade.ProductVersionTestSupport.project;
import static org.openrewrite.maven.Assertions.pomXml;

class CamelProductVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        configure(spec);
        spec.recipeFromResources("org.apache.camel.upgrade.AlignCamelVersions");
    }

    @Test
    void alignsExplicitCoreAndKameletsWithoutChangingOtherGroups() {
        String before = project("", """
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>4.22.0</version></dependency>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-vertx</artifactId><version>4.22.0</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>4.21.0</version></dependency>
              <dependency><groupId>org.example</groupId><artifactId>unrelated</artifactId><version>4.21.0</version></dependency>
            </dependencies>
            """);
        rewriteRun(pomXml(before, before
                .replace("<version>4.22.0</version>", "<version>" + CORE + "</version>")
                .replace("<artifactId>camel-kamelets</artifactId><version>4.21.0</version>",
                         "<artifactId>camel-kamelets</artifactId><version>" + KAMELETS + "</version>")));
    }

    @Test
    void updatesLocalPropertiesAndPreservesReferences() {
        String before = project("", """
            <properties><camel.version>4.14.2</camel.version><catalog.version>4.21.0</catalog.version></properties>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>${camel.version}</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>${catalog.version}</version></dependency>
            </dependencies>
            """);
        rewriteRun(pomXml(before, before.replace("4.14.2", CORE).replace("4.21.0", KAMELETS)));
    }

    @Test
    void keepsBomManagedDependenciesWithoutExplicitVersions() {
        String pom = project("", """
            <dependencyManagement><dependencies><dependency>
              <groupId>com.redhat.camel.springboot.platform</groupId><artifactId>camel-spring-boot-bom</artifactId><version>%s</version><type>pom</type><scope>import</scope>
            </dependency></dependencies></dependencyManagement>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId></dependency>
            </dependencies>
            """.formatted(CSB));
        rewriteRun(pomXml(pom));
    }

    @Test
    void alignsCoreParentAndPlugin() {
        String before = project("""
            <parent><groupId>org.apache.camel</groupId><artifactId>camel-parent</artifactId><version>4.22.0</version><relativePath/></parent>
            """, """
            <build><plugins><plugin><groupId>org.apache.camel</groupId><artifactId>camel-maven-plugin</artifactId><version>4.22.0</version></plugin></plugins></build>
            """);
        rewriteRun(pomXml(before, before.replace("4.22.0", CORE)));
    }

    @Test
    void alreadyAlignedPomIsUnchanged() {
        String pom = project("", """
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>%s</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>%s</version></dependency>
            </dependencies>
            """.formatted(CORE, KAMELETS));
        rewriteRun(pomXml(pom));
    }

    @Test
    void fullCamelRecipeAlignsCoreAndKamelets() {
        String before = project("", """
            <properties><maven.compiler.release>17</maven.compiler.release></properties>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>4.22.0</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>4.21.0</version></dependency>
            </dependencies>
            """);
        String after = before.replace("<version>4.22.0</version>", "<version>" + CORE + "</version>")
                .replace("<version>4.21.0</version>", "<version>" + KAMELETS + "</version>");
        rewriteRun(spec -> spec.recipeFromResources("org.apache.camel.upgrade.CamelMigrationRecipe"),
                pomXml(before, after));
    }

    @Test
    void separatesSharedCamelVersionBeforeApplyingIndependentTargets() {
        assertSharedVersionTargets("camel.version");
    }

    @Test
    void separatesSharedCustomVersionBeforeApplyingIndependentTargets() {
        assertSharedVersionTargets("dependencies.version");
    }

    private void assertSharedVersionTargets(String property) {
        String before = project("", """
            <properties><%1$s>4.21.0</%1$s></properties>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>${%1$s}</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>${%1$s}</version></dependency>
            </dependencies>
            """.formatted(property));
        String after = before.replace("4.21.0", CORE)
                .replace("<artifactId>camel-kamelets</artifactId><version>${" + property + "}</version>",
                        "<artifactId>camel-kamelets</artifactId><version>" + KAMELETS + "</version>");
        rewriteRun(spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1), pomXml(before, after));
    }

    @Test
    void separatesSharedVersionsInDependencyManagement() {
        String before = project("", """
            <properties><camel.version>4.21.0</camel.version></properties>
            <dependencyManagement><dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId><version>${camel.version}</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>${camel.version}</version></dependency>
            </dependencies></dependencyManagement>
            <dependencies>
              <dependency><groupId>org.apache.camel</groupId><artifactId>camel-platform-http-main</artifactId></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId></dependency>
            </dependencies>
            """);
        String after = before.replace("4.21.0", CORE)
                .replace("<artifactId>camel-kamelets</artifactId><version>${camel.version}</version>",
                        "<artifactId>camel-kamelets</artifactId><version>" + KAMELETS + "</version>");
        rewriteRun(spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1), pomXml(before, after));
    }

    @Test
    void sharedKameletsPropertyDoesNotChangeUnrelatedDependencies() {
        String before = project("", """
            <properties><shared.version>4.21.0</shared.version></properties>
            <dependencies>
              <dependency><groupId>org.example</groupId><artifactId>unrelated</artifactId><version>${shared.version}</version></dependency>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>${shared.version}</version></dependency>
            </dependencies>
            """);
        String after = before.replace("<artifactId>camel-kamelets</artifactId><version>${shared.version}</version>",
                "<artifactId>camel-kamelets</artifactId><version>" + KAMELETS + "</version>");
        rewriteRun(spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1), pomXml(before, after));
    }

    @Test
    void separatesCamelVersionEvenWithoutACoreDependency() {
        String before = project("", """
            <properties><camel.version>4.21.0</camel.version></properties>
            <dependencies>
              <dependency><groupId>org.apache.camel.kamelets</groupId><artifactId>camel-kamelets</artifactId><version>${camel.version}</version></dependency>
            </dependencies>
            """);
        String after = before.replace("4.21.0", CORE)
                .replace("<artifactId>camel-kamelets</artifactId><version>${camel.version}</version>",
                        "<artifactId>camel-kamelets</artifactId><version>" + KAMELETS + "</version>");
        rewriteRun(spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1), pomXml(before, after));
    }

}
