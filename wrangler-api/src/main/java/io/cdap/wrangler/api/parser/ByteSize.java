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
 * Represents a token that encapsulates a byte size value.
 * This class parses a string (e.g., "10KB", "1.5MB") and converts it to its canonical
 * representation in bytes.
 */
@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Byte size cannot be null or empty");
        }

        // Extract the unit and numeric part
        String unit = value.replaceAll("[0-9.]+", "").toUpperCase().trim();
        String numberPart = value.replaceAll("[^0-9.]+", "").trim();

        if (unit.isEmpty() || numberPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid ByteSize value: " + value);
        }
 
        double num = Double.parseDouble(numberPart);
        switch (unit) {
            case "KB":
                return (long) (num * 1000);
            case "MB":
                return (long) (num * Math.pow(1000, 2));
            case "GB":
                return (long) (num * Math.pow(1000, 3));
            case "TB":
                return (long) (num * Math.pow(1000, 4));
            case "PB":
                return (long) (num * Math.pow(1000, 5));
            case "B":
                return (long) num;
            default:
                throw new IllegalArgumentException("Invalid byte type: " + value);
        }
    }

    public long getBytes() {
        return this.bytes;
    }

    @Override
    public Object value() {
        return this.bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.type().name());
        object.addProperty("bytes", this.bytes);
        return object;
    }
}
