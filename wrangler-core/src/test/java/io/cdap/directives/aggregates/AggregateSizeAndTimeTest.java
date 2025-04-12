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

import io.cdap.wrangler.api.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AggregateSizeAndTimeTest {

    private AggregateSizeAndTime directive;
    private io.cdap.wrangler.api.Arguments arguments;
    private io.cdap.wrangler.api.ExecutorContext context;
    private io.cdap.wrangler.api.TransientStore store;

    @BeforeEach
    public void setUp() throws Exception {
        directive = new AggregateSizeAndTime();
        arguments = Mockito.mock(io.cdap.wrangler.api.Arguments.class);
        context = Mockito.mock(io.cdap.wrangler.api.ExecutorContext.class);
        store = Mockito.mock(io.cdap.wrangler.api.TransientStore.class);

        Mockito.when(context.getTransientStore()).thenReturn(store);
        Mockito.when(arguments.value("inputSizeCol"))
                .thenReturn(new io.cdap.wrangler.api.parser.ColumnName("size"));
        Mockito.when(arguments.value("inputTimeCol"))
                .thenReturn(new io.cdap.wrangler.api.parser.ColumnName("time"));
        Mockito.when(arguments.value("outputSizeCol"))
                .thenReturn(new io.cdap.wrangler.api.parser.ColumnName("totalSize"));
        Mockito.when(arguments.value("outputTimeCol"))
                .thenReturn(new io.cdap.wrangler.api.parser.ColumnName("totalTime"));
        Mockito.when(arguments.contains("aggregationType")).thenReturn(true);
        Mockito.when(arguments.value("aggregationType"))
                .thenReturn(new io.cdap.wrangler.api.parser.Text("total"));

        directive.initialize(arguments);
    }

    @Test
    public void testTotalAggregation() throws Exception {
        List<Row> inputRows = Arrays.asList(
                new Row("size", "500KB").add("time", "1s"),
                new Row("size", "1.5MB").add("time", "2000ms"));

        Mockito.when(store.get("agg.total.size")).thenReturn(null);
        Mockito.when(store.get("agg.total.time")).thenReturn(null);
        Mockito.when(store.get("agg.row.count")).thenReturn(null);

        List<Row> midResult = directive.execute(inputRows, context);
        assertEquals(2, midResult.size());

        // Simulate store state for finalization.
        Mockito.when(store.get("agg.total.size"))
                .thenReturn(500_000L + 1_500_000L);
        Mockito.when(store.get("agg.total.time"))
                .thenReturn(1_000L + 2_000L);
        Mockito.when(store.get("agg.row.count")).thenReturn(2L);

        List<Row> finalResult = directive.execute(Collections.emptyList(), context);
        assertEquals(1, finalResult.size());

        Row finalRow = finalResult.get(0);
        double expectedMB = (500_000 + 1_500_000) / 1_000_000.0;
        double expectedSec = (1_000 + 2_000) / 1000.0;

        assertEquals(expectedMB, (Double) finalRow.getValue("totalSize"), 0.001);
        assertEquals(expectedSec, (Double) finalRow.getValue("totalTime"), 0.001);
    }
}
