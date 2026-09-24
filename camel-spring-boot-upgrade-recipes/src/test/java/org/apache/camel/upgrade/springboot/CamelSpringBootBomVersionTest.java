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

import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Regression tests for BOM version upgrade in the CamelSpringBootMigrationRecipe.
 *
 * Covers three shapes found in real downstream POMs:
 * <ol>
 *   <li>Property-managed BOM: {@code ${camel-spring-boot-version}} — the shape used by
 *       jboss-fuse/camel-spring-boot-examples.</li>
 *   <li>Alternate property name: {@code ${camel.springboot.version}} — used in some upstream
 *       Apache POMs.</li>
 *   <li>Literal-version BOM: {@code com.redhat.camel.springboot.platform:camel-spring-boot-bom}
 *       with a hardcoded version — the shape used when maven-metadata.xml is absent in MRRC
 *       staging and UpgradeDependencyVersion would silently no-op on a property reference.</li>
 * </ol>
 */
class CamelSpringBootBomVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
                .scanYamlResources()
                .build()
                .activateRecipes("org.apache.camel.upgrade.CamelSpringBootMigrationRecipe"));
    }

    /**
     * Downstream example shape: BOM imported via {@code ${camel-spring-boot-version}} property.
     * UpgradeDependencyVersion cannot update this because it resolves the new version from
     * maven-metadata.xml; when that file is absent (MRRC staging), the recipe is a silent no-op.
     * ChangePropertyValue does not need metadata — it sets the value directly.
     */
    @DocumentExample
    @Test
    void propertyManagedBomIsUpdated() {
        //language=xml
        rewriteRun(
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <camel-spring-boot-version>4.18.1.redhat-00007</camel-spring-boot-version>
                                <spring-boot-version>3.5.0</spring-boot-version>
                            </properties>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>${camel-spring-boot-version}</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <camel-spring-boot-version>@camel-spring-boot-version@</camel-spring-boot-version>
                                <spring-boot-version>@spring-boot-version@</spring-boot-version>
                            </properties>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>${camel-spring-boot-version}</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """
                )
        );
    }

    /**
     * Literal-version shape: BOM uses a hardcoded Red Hat version string.
     * UpgradeDependencyVersion is used here because there is no property to update;
     * the coordinate and version are matched directly.
     */
    @Test
    void literalVersionBomIsUpdated() {
        //language=xml
        rewriteRun(
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>4.18.1.redhat-00007</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>@camel-spring-boot-version@</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """
                )
        );
    }

    /**
     * Alternate upstream property name {@code camel.springboot.version}.
     */
    @Test
    void alternateCamelSpringbootVersionPropertyIsUpdated() {
        //language=xml
        rewriteRun(
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <camel.springboot.version>4.18.1.redhat-00007</camel.springboot.version>
                            </properties>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>${camel.springboot.version}</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <camel.springboot.version>@camel-spring-boot-version@</camel.springboot.version>
                            </properties>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.redhat.camel.springboot.platform</groupId>
                                        <artifactId>camel-spring-boot-bom</artifactId>
                                        <version>${camel.springboot.version}</version>
                                        <type>pom</type>
                                        <scope>import</scope>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                        """
                )
        );
    }
}
