/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.lucene.schema;

import com.google.common.base.Objects;
import com.stratio.cassandra.lucene.schema.analysis.ClasspathAnalyzerBuilder;
import com.stratio.cassandra.lucene.schema.analysis.PreBuiltAnalyzers;
import com.stratio.cassandra.lucene.schema.column.Columns;
import com.stratio.cassandra.lucene.schema.mapping.Mapper;
import org.apache.cassandra.config.CFMetaData;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for several columns mappings between Cassandra and Lucene.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class Schema implements Closeable {

    private final Map<String, Mapper> mappers;

    private final Map<String, Analyzer> analyzers;

    private final Analyzer defaultAnalyzer;

    private final Analyzer analyzer;

    /**
     * Builds a new {@code Schema} for the specified {@link Mapper}s and {@link Analyzer}s.
     *
     * @param defaultAnalyzer The default {@link Analyzer} to be used.
     * @param mappers         The per field {@link Mapper}s builders to be used.
     * @param analyzers       The per field {@link Analyzer}s to be used.
     */
    public Schema(Analyzer defaultAnalyzer, Map<String, Mapper> mappers, Map<String, Analyzer> analyzers) {

        this.defaultAnalyzer = defaultAnalyzer != null ? defaultAnalyzer : PreBuiltAnalyzers.DEFAULT.get();
        this.mappers = mappers != null ? mappers : new HashMap<String, Mapper>();
        this.analyzers = analyzers != null ? analyzers : new HashMap<String, Analyzer>();

        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        for (Map.Entry<String, Mapper> entry : this.mappers.entrySet()) {
            String name = entry.getKey();
            Mapper mapper = entry.getValue();
            String analyzerName = mapper.getAnalyzer();
            Analyzer analyzer = getAnalyzer(analyzerName);
            perFieldAnalyzers.put(name, analyzer);
        }
        this.analyzer = new PerFieldAnalyzerWrapper(this.defaultAnalyzer, perFieldAnalyzers);
    }

    public Analyzer getDefaultAnalyzer() {
        return defaultAnalyzer;
    }

    /**
     * Returns the {@link Analyzer} identified by the specified name. If there is no analyzer with the specified name,
     * then it will be interpreted as a class name and it will be instantiated by reflection.
     *
     * {@link IllegalArgumentException} is thrown if there is no {@link Analyzer} with such name.
     *
     * @param name The name of the {@link Analyzer} to be returned.
     * @return The {@link Analyzer} identified by the specified name.
     */
    public Analyzer getAnalyzer(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Not null nor empty analyzer name required");
        }
        Analyzer analyzer = analyzers.get(name);
        if (analyzer == null) {
            analyzer = PreBuiltAnalyzers.get(name);
            if (analyzer == null) {
                try {
                    analyzer = (new ClasspathAnalyzerBuilder(name)).analyzer();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Not found analyzer: " + name);
                }
            }
            analyzers.put(name, analyzer);
        }
        return analyzer;
    }

    /**
     * Returns the used {@link Analyzer} wrapper.
     *
     * @return The used {@link Analyzer} wrapper.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Returns the {@link Mapper} identified by the specified field name, or {@code null} if not found.
     *
     * @param field A field name.
     * @return The {@link Mapper} identified by the specified field name, or {@code null} if not found.
     */
    public Mapper getMapper(String field) {
        String[] components = field.split("\\.");
        for (int i = components.length - 1; i >= 0; i--) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j <= i; j++) {
                sb.append(components[j]);
                if (j < i) sb.append('.');
            }
            Mapper mapper = mappers.get(sb.toString());
            if (mapper != null) return mapper;
        }
        return null;
    }

    /**
     * Adds to the specified {@link Document} the Lucene fields representing the specified {@link Columns}.
     *
     * @param document The Lucene {@link Document} where the fields are going to be added.
     * @param columns  The {@link Columns} to be added.
     */
    public void addFields(Document document, Columns columns) {
        for (Mapper mapper : mappers.values()) {
            mapper.addFields(document, columns);
        }
    }

    /**
     * Checks if this is consistent with the specified column family metadata.
     *
     * @param metadata A column family metadata.
     */
    public void validate(CFMetaData metadata) {
        for (Mapper mapper : mappers.values()) {
            mapper.validate(metadata);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        analyzer.close();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                      .add("mappers", mappers)
                      .add("analyzers", analyzers)
                      .add("defaultAnalyzer", defaultAnalyzer)
                      .add("analyzer", analyzer)
                      .toString();
    }
}
