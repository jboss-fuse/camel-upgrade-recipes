/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.camel.upgrade.springboot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelSpringBoot419Test {

    @Test
    void replacesUndertowWithCsbManagedDependency() throws IOException {
        var recipe = getClass().getClassLoader().getResourceAsStream("META-INF/rewrite/4.19.yaml");
        var recipeText = new String(recipe.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(recipeText.contains("oldGroupId: org.springframework.boot"));
        assertTrue(recipeText.contains("oldArtifactId: spring-boot-starter-undertow"));
        assertTrue(recipeText.contains("newGroupId: com.redhat.integration"));
        assertTrue(recipeText.contains("newArtifactId: spring-boot-starter-undertow"));
    }
}
