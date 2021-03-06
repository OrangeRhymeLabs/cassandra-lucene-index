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
package com.stratio.cassandra.lucene.search.condition.builder;

import com.stratio.cassandra.lucene.search.condition.GeoBBoxCondition;
import com.stratio.cassandra.lucene.search.condition.GeoDistanceCondition;
import com.stratio.cassandra.lucene.util.GeoDistance;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * {@link ConditionBuilder} for building a new {@link GeoBBoxCondition}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class GeoDistanceConditionBuilder extends ConditionBuilder<GeoDistanceCondition, GeoDistanceConditionBuilder> {

    /** The name of the field to be matched. */
    @JsonProperty("field")
    String field;

    /** The latitude of the reference point. */
    @JsonProperty("latitude")
    double latitude;

    /** The longitude of the reference point. */
    @JsonProperty("longitude")
    double longitude;

    /** The min allowed distance. */
    @JsonProperty("min_distance")
    String minDistance;

    /** The max allowed distance. */
    @JsonProperty("max_distance")
    String maxDistance;

    /**
     * Returns a new {@link GeoDistanceConditionBuilder} with the specified field reference point.
     *
     * @param field       The name of the field to be matched.
     * @param latitude    The latitude of the reference point.
     * @param longitude   The longitude of the reference point.
     * @param maxDistance The max allowed distance.
     */
    @JsonCreator
    public GeoDistanceConditionBuilder(@JsonProperty("field") String field,
                                       @JsonProperty("latitude") double latitude,
                                       @JsonProperty("longitude") double longitude,
                                       @JsonProperty("max_distance") String maxDistance) {
        this.field = field;
        this.longitude = longitude;
        this.latitude = latitude;
        this.maxDistance = maxDistance;
    }

    /**
     * Sets the min allowed {@link GeoDistance}.
     *
     * @param minDistance The min allowed {@link GeoDistance}.
     * @return This.
     */
    public GeoDistanceConditionBuilder setMinDistance(String minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    /**
     * Returns the {@link GeoDistanceCondition} represented by this builder.
     *
     * @return The {@link GeoDistanceCondition} represented by this builder.
     */
    @Override
    public GeoDistanceCondition build() {
        return new GeoDistanceCondition(boost, field, latitude, longitude, minDistance, maxDistance);
    }
}
