/*
 * Copyright 2015, Stratio.
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
package com.stratio.cassandra.lucene.search.sort.builder;

import com.stratio.cassandra.lucene.search.sort.SortField;
import com.stratio.cassandra.lucene.util.JsonSerializer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Class for testing {@link SortFieldBuilder}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class SortFieldBuilderTest {

    @Test
    public void testBuild() {
        String field = "field";
        SortFieldBuilder builder = new SortFieldBuilder(field).reverse(true);
        SortField sortField = builder.build();
        assertNotNull(sortField);
        assertEquals(field, sortField.getField());
        assertEquals(true, sortField.isReverse());
    }

    @Test
    public void testBuildDefault() {
        String field = "field";
        SortFieldBuilder builder = new SortFieldBuilder(field);
        SortField sortField = builder.build();
        assertNotNull(sortField);
        assertEquals(field, sortField.getField());
        assertEquals(SortField.DEFAULT_REVERSE, sortField.isReverse());
    }

    @Test
    public void testBuildReverse() {
        String field = "field";
        SortFieldBuilder builder = new SortFieldBuilder(field).reverse(false);
        SortField sortField = builder.build();
        assertNotNull(sortField);
        assertEquals(field, sortField.getField());
        assertEquals(false, sortField.isReverse());
    }

    @Test
    public void testJson() throws IOException {
        String json1 = "{field:\"field\",reverse:false}";
        SortFieldBuilder sortFieldBuilder = JsonSerializer.fromString(json1, SortFieldBuilder.class);
        String json2 = JsonSerializer.toString(sortFieldBuilder);
        assertEquals(json1, json2);
    }

    @Test
    public void testJsonDefault() throws IOException {
        String json1 = "{field:\"field\"}";
        SortFieldBuilder sortFieldBuilder = JsonSerializer.fromString(json1, SortFieldBuilder.class);
        String json2 = JsonSerializer.toString(sortFieldBuilder);
        assertEquals("{field:\"field\",reverse:false}", json2);
    }

    @Test
    public void testJsonReverse() throws IOException {
        String json1 = "{field:\"field\",reverse:true}";
        SortFieldBuilder sortFieldBuilder = JsonSerializer.fromString(json1, SortFieldBuilder.class);
        String json2 = JsonSerializer.toString(sortFieldBuilder);
        assertEquals(json1, json2);
    }
}
