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

package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Collections;
import java.util.List;
 
/**
 * Directive to aggregate byte size and time duration.
 * <p>
 * This directive accumulates source column values (as ByteSize and TimeDuration),
 * computes either a total or average, converts to specified units, and outputs the result.
 * </p>
 */
public class AggregateSizeAndTime implements Directive {
    public static final String STORE_KEY_TOTAL_SIZE = "agg.total.size";
    public static final String STORE_KEY_TOTAL_TIME = "agg.total.time";
    public static final String STORE_KEY_ROW_COUNT = "agg.row.count";
    public static final String STORE_KEY_IS_FINALIZED = "agg.is.finalized";

    private String inputSizeCol;
    private String inputTimeCol;
    private String outputSizeCol;
    private String outputTimeCol;
    private String sizeUnit = "MB";
    private String timeUnit = "seconds";
    private String aggregationType = "total";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-size-time");
        builder.define("inputSizeCol", TokenType.COLUMN_NAME);
        builder.define("inputTimeCol", TokenType.COLUMN_NAME);
        builder.define("outputSizeCol", TokenType.COLUMN_NAME);
        builder.define("outputTimeCol", TokenType.COLUMN_NAME);
        builder.define("sizeUnit", TokenType.TEXT, Optional.TRUE);
        builder.define("timeUnit", TokenType.TEXT, Optional.TRUE);
        builder.define("aggregationType", TokenType.TEXT, Optional.TRUE);
        return builder.build();
    }

    @Override
    public void initialize(io.cdap.wrangler.api.Arguments arguments)
        throws DirectiveParseException {
        inputSizeCol = ((ColumnName) arguments.value("inputSizeCol")).value();
        inputTimeCol = ((ColumnName) arguments.value("inputTimeCol")).value();
        outputSizeCol = ((ColumnName) arguments.value("outputSizeCol")).value();
        outputTimeCol = ((ColumnName) arguments.value("outputTimeCol")).value();
 
        if (arguments.contains("sizeUnit")) {
            sizeUnit = ((Text) arguments.value("sizeUnit")).value();
        }
        if (arguments.contains("timeUnit")) {
            timeUnit = ((Text) arguments.value("timeUnit")).value();
        }
        if (arguments.contains("aggregationType")) {
            aggregationType = ((Text) arguments.value("aggregationType")).value();
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context)
        throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();
        Boolean isFinalized = store.get(STORE_KEY_IS_FINALIZED);

        if (isFinalized != null && isFinalized) {
            return Collections.emptyList();
        }

        Long totalSize = store.get(STORE_KEY_TOTAL_SIZE);
        Long totalTime = store.get(STORE_KEY_TOTAL_TIME);
        Long rowCount = store.get(STORE_KEY_ROW_COUNT);

        if (totalSize == null) {
            totalSize = 0L;
        }
        if (totalTime == null) {
            totalTime = 0L;
        }
        if (rowCount == null) {
            rowCount = 0L;
        }

        // Finalize aggregation when receiving an empty batch.
        if (rows.isEmpty()) {
            if ("average".equalsIgnoreCase(aggregationType) && rowCount > 0) {
                totalSize /= rowCount;
                totalTime /= rowCount;
            }
            double convertedSize = convertBytesTo(totalSize, sizeUnit);
            double convertedTime = convertMillisecondsTo(totalTime, timeUnit);
 
            Row result = new Row();
            result.add(outputSizeCol, convertedSize);
            result.add(outputTimeCol, convertedTime);
 
            store.set(TransientVariableScope.GLOBAL, STORE_KEY_IS_FINALIZED, true);
            return Collections.singletonList(result);
        }

        // Process each row.
        for (Row row : rows) {
            Object sizeObj = row.getValue(inputSizeCol);
            Object timeObj = row.getValue(inputTimeCol);

            if (sizeObj == null || timeObj == null) {
                continue;
            }

            try {
                ByteSize byteSize = new ByteSize(sizeObj.toString());
                TimeDuration timeDuration = new TimeDuration(timeObj.toString());

                totalSize += byteSize.getBytes();
                totalTime += timeDuration.getMilliseconds();
                rowCount++;
            } catch (Exception e) {
                throw new DirectiveExecutionException("Failed to parse input: " + e.getMessage());
            }
        }

        store.set(TransientVariableScope.GLOBAL, STORE_KEY_TOTAL_SIZE, totalSize);
        store.set(TransientVariableScope.GLOBAL, STORE_KEY_TOTAL_TIME, totalTime);
        store.set(TransientVariableScope.GLOBAL, STORE_KEY_ROW_COUNT, rowCount);
        return rows;
    }

    private double convertBytesTo(long bytes, String unit) {
        double value = bytes;
        switch (unit.toUpperCase()) {
            case "KB":
                return value / 1000;
            case "MB":
                return value / Math.pow(1000, 2);
            case "GB":
                return value / Math.pow(1000, 3);
            case "TB":
                return value / Math.pow(1000, 4);
            case "PB":
                return value / Math.pow(1000, 5);
            case "B":
                return value;
            default:
                throw new IllegalArgumentException("Invalid byte unit: " + unit);
        }
    }

    private double convertMillisecondsTo(long milliseconds, String unit) {
        double value = milliseconds;
        switch (unit.toLowerCase()) {
            case "ms":
                return value;
            case "s":
            case "sec":
            case "seconds":
                return value / 1000;
            case "m":
            case "min":
                return value / (60 * 1000);
            case "h":
            case "hr":
                return value / (3600 * 1000);
            case "d":
            case "day":
                return value / (86400 * 1000);
            default:
                throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    @Override
    public void destroy() {
        // No cleanup necessary.
    }
}
