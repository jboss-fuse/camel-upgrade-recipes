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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class SeparateSharedKameletsVersion extends Recipe {
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{[^}]+}");

    @Override
    public String getDisplayName() {
        return "Separate shared Kamelets version references";
    }

    @Override
    public String getDescription() {
        return "Replaces shared Kamelets version property references with their current resolved value before independent version alignment.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final Set<String> otherReferences = new HashSet<>();

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                otherReferences.clear();
                collectOtherReferences(document.getRoot());
                return super.visitDocument(document, ctx);
            }

            private void collectOtherReferences(Xml.Tag tag) {
                tag.getValue().ifPresent(value -> {
                    Matcher matcher = PROPERTY.matcher(value);
                    while (matcher.find()) {
                        otherReferences.add(matcher.group());
                    }
                });
                boolean kamelets = "org.apache.camel.kamelets".equals(tag.getChildValue("groupId").orElse(null))
                        && "camel-kamelets".equals(tag.getChildValue("artifactId").orElse(null));
                for (Xml.Tag child : tag.getChildren()) {
                    if (!kamelets || !"version".equals(child.getName())) {
                        collectOtherReferences(child);
                    }
                }
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isDependencyTag("org.apache.camel.kamelets", "camel-kamelets")
                        && !isManagedDependencyTag("org.apache.camel.kamelets", "camel-kamelets")) {
                    return t;
                }
                Xml.Tag version = t.getChild("version").orElse(null);
                String reference = version == null ? "" : version.getValue().orElse("");
                // camel.version is also written by the alignment recipe even without a core dependency.
                if (!PROPERTY.matcher(reference).matches()
                        || (!"${camel.version}".equals(reference) && !otherReferences.contains(reference))) {
                    return t;
                }
                String resolved = getResolutionResult().getPom().getValue(reference);
                if (resolved == null || resolved.contains("${")) {
                    return t;
                }
                maybeUpdateModel();
                return t.withChildValue("version", resolved);
            }
        };
    }
}
