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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.test.RecipeSpec;

/** Minimal Maven models isolate coordinate alignment from transitive/API resolution. */
public final class ProductVersionTestSupport {
    public static final String CORE = CamelTestUtil.getCamelTargetVersion();
    public static final String KAMELETS = CamelTestUtil.getString("camel.kamelets.version", "Missing Kamelets target version");
    public static final String CSB = CamelTestUtil.getString("camel.springboot.version", "Missing Spring Boot target version");
    private static Path repository;

    private ProductVersionTestSupport() {
    }

    public static void configure(RecipeSpec spec) {
        var ctx = new InMemoryExecutionContext(t -> { throw new AssertionError(t); });
        MavenExecutionContextView.view(ctx)
                .setAddCentralRepository(false)
                .setAddLocalRepository(false)
                .setRepositories(List.of(MavenRepository.builder().id("version-fixtures")
                        .uri(repository().toUri().toString())
                        .releases("true").snapshots("true").knownToExist(true).build()));
        spec.executionContext(ctx).recipeExecutionContext(ctx);
    }

    private static synchronized Path repository() {
        if (repository != null) {
            return repository;
        }
        try {
            Path root = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "product-version-fixtures-");
            Map<String, List<String>> artifacts = new LinkedHashMap<>();
            List<String> coreVersions = List.of("4.14.2", "4.21.0", "4.22.0", "4.22.1", CORE);
            List<String> csbVersions = List.of("4.14.2", "4.22.0", CSB);
            for (String artifact : List.of("camel-platform-http-main", "camel-platform-http-vertx", "camel-parent", "camel-maven-plugin")) {
                artifacts.put("org.apache.camel:" + artifact, coreVersions);
            }
            artifacts.put("org.apache.camel.kamelets:camel-kamelets", List.of("4.14.2", "4.21.0", "4.22.0", "4.22.1", KAMELETS));
            artifacts.put("org.apache.camel.springboot:camel-timer-starter", csbVersions);
            artifacts.put("com.redhat.camel.springboot.platform:camel-spring-boot-bom", csbVersions);
            artifacts.put("org.example:unrelated", List.of("4.21.0"));
            for (var entry : artifacts.entrySet()) {
                String[] ga = entry.getKey().split(":");
                String group = ga[0];
                String artifact = ga[1];
                Path directory = root.resolve(group.replace('.', '/')).resolve(artifact);
                for (String version : entry.getValue()) {
                    Path versionDir = Files.createDirectories(directory.resolve(version));
                    String pom = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
                            + "<groupId>" + group + "</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version>";
                    if (artifact.equals("camel-parent") || artifact.equals("camel-spring-boot-bom")) {
                        pom += "<packaging>pom</packaging>";
                    } else {
                        // OpenRewrite requires a nonempty JAR beside file-repository JAR POMs.
                        try (var zip = new ZipOutputStream(Files.newOutputStream(versionDir.resolve(artifact + "-" + version + ".jar")))) {
                            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
                            zip.closeEntry();
                        }
                    }
                    if (artifact.equals("camel-spring-boot-bom")) {
                        pom += "<dependencyManagement><dependencies>";
                        for (String dependency : List.of("org.apache.camel:camel-platform-http-main", "org.apache.camel:camel-platform-http-vertx",
                                "org.apache.camel.kamelets:camel-kamelets")) {
                            String[] parts = dependency.split(":");
                            String managed = version.equals(CSB)
                                    ? (parts[0].equals("org.apache.camel.kamelets") ? KAMELETS : CORE)
                                    : "4.14.2";
                            pom += "<dependency><groupId>" + parts[0] + "</groupId><artifactId>" + parts[1]
                                    + "</artifactId><version>" + managed + "</version></dependency>";
                        }
                        pom += "</dependencies></dependencyManagement>";
                    }
                    Files.writeString(versionDir.resolve(artifact + "-" + version + ".pom"), pom + "</project>");
                }
                StringBuilder metadata = new StringBuilder("<metadata><groupId>" + group + "</groupId><artifactId>" + artifact
                        + "</artifactId><versioning><versions>");
                for (String version : entry.getValue()) {
                    metadata.append("<version>").append(version).append("</version>");
                }
                Files.writeString(directory.resolve("maven-metadata.xml"), metadata + "</versions></versioning></metadata>");
            }
            repository = root;
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String project(String parent, String body) {
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n<modelVersion>4.0.0</modelVersion>\n"
                + parent + "<groupId>com.foo</groupId><artifactId>acme</artifactId><version>1.0-SNAPSHOT</version>\n"
                + body + "</project>";
    }
}
