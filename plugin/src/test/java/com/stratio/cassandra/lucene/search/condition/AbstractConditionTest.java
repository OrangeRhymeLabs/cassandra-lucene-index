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
package com.stratio.cassandra.lucene.search.condition;

import com.stratio.cassandra.lucene.schema.Schema;
import com.stratio.cassandra.lucene.schema.analysis.PreBuiltAnalyzers;
import com.stratio.cassandra.lucene.schema.mapping.Mapper;
import org.apache.lucene.analysis.Analyzer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class AbstractConditionTest {

    protected Schema mockSchema(String mapperName, Mapper mapper) {
        Schema schema = mock(Schema.class);
        when(schema.getAnalyzer()).thenReturn(PreBuiltAnalyzers.KEYWORD.get());
        when(schema.getMapper(mapperName)).thenReturn(mapper);
        return schema;
    }

    protected Schema mockSchema(String mapperName, Mapper mapper, Analyzer analyzer) {
        Schema schema = mock(Schema.class);
        when(schema.getAnalyzer()).thenReturn(analyzer);
        when(schema.getMapper(mapperName)).thenReturn(mapper);
        return schema;
    }

    protected Schema mockSchema(String mapperName, Mapper mapper, String analyzer) {
        Schema schema = mock(Schema.class);
        when(schema.getAnalyzer()).thenReturn(PreBuiltAnalyzers.get(analyzer));
        when(schema.getMapper(mapperName)).thenReturn(mapper);
        return schema;
    }
}
