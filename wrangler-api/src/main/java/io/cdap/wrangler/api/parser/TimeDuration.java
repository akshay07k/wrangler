/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Represents a token that encapsulates a time duration value.
 * This class parses a string (e.g., "150ms", "2.1s") and converts it into its canonical
 * representation in milliseconds.
 */
@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;

    public TimeDuration(String value) {
        this.milliseconds = parseTimeDuration(value);
    }

    private long parseTimeDuration(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Time duration cannot be null or empty");
        }

        // Extract the unit and numeric part
        String unit = value.replaceAll("[0-9.]+", "").toLowerCase().trim();
        String numberPart = value.replaceAll("[^0-9.]+", "").trim();

        if (unit.isEmpty() || numberPart.isEmpty()) {
        throw new IllegalArgumentException("Invalid TimeDuration value: " + value);
        }

        double num = Double.parseDouble(numberPart);
        switch (unit) {
            case "ms":
                return (long) num;
            case "s":
            case "sec":
                return (long) (num * 1000);
            case "m":
            case "min":
                return (long) (num * 60 * 1000);
            case "h":
            case "hr":
                return (long) (num * 3600 * 1000);
            case "d":
            case "day":
                return (long) (num * 86400 * 1000);
        default:
            throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    public long getMilliseconds() {
        return this.milliseconds;
    }

    @Override
    public Object value() {
        return this.milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.type().name());
        object.addProperty("milliseconds", this.milliseconds);
        return object;
    }
}
