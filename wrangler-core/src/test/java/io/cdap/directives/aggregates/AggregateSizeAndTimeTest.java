/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

 package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.*;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AggregateSizeAndTimeTest {

    private AggregateSizeAndTime directive;
    private Arguments arguments;
    private ExecutorContext context;
    private TransientStore store;

    @BeforeEach
    public void setUp() throws Exception {
        directive = new AggregateSizeAndTime();
        arguments = mock(Arguments.class);
        context = mock(ExecutorContext.class);
        store = mock(TransientStore.class);

        when(context.getTransientStore()).thenReturn(store);

        when(arguments.value("inputSizeCol")).thenReturn(new ColumnName("size"));
        when(arguments.value("inputTimeCol")).thenReturn(new ColumnName("time"));
        when(arguments.value("outputSizeCol")).thenReturn(new ColumnName("totalSize"));
        when(arguments.value("outputTimeCol")).thenReturn(new ColumnName("totalTime"));
        when(arguments.contains("aggregationType")).thenReturn(true);
        when(arguments.value("aggregationType")).thenReturn(new Text("total"));

        directive.initialize(arguments);
    }

    @Test
    public void testTotalAggregation() throws Exception {
        List<Row> inputRows = Arrays.asList(
                new Row("size", "500KB").add("time", "1s"),
                new Row("size", "1.5MB").add("time", "2000ms")
        );

        when(store.get("agg.total.size")).thenReturn(null);
        when(store.get("agg.total.time")).thenReturn(null);
        when(store.get("agg.row.count")).thenReturn(null);

        List<Row> midResult = directive.execute(inputRows, context);
        assertEquals(2, midResult.size());

        // simulate store state for finalization
        when(store.get("agg.total.size")).thenReturn(500_000L + 1_500_000L);
        when(store.get("agg.total.time")).thenReturn(1_000L + 2_000L);
        when(store.get("agg.row.count")).thenReturn(2L);

        List<Row> finalResult = directive.execute(Collections.emptyList(), context);
        assertEquals(1, finalResult.size());

        Row finalRow = finalResult.get(0);

        double expectedMB = (500_000 + 1_500_000) / 1_000_000.0;
        double expectedSec = (1_000 + 2_000) / 1000.0;

        assertEquals(expectedMB, finalRow.getValue("totalSize"));
        assertEquals(expectedSec, finalRow.getValue("totalTime"));
    }

    @Test
    public void testAverageAggregation() throws Exception {
        when(arguments.value("aggregationType")).thenReturn(new Text("average"));
        directive.initialize(arguments);

        List<Row> inputRows = Arrays.asList(
                new Row("size", "1MB").add("time", "1s"),
                new Row("size", "2MB").add("time", "3s")
        );

        when(store.get("agg.total.size")).thenReturn(null);
        when(store.get("agg.total.time")).thenReturn(null);
        when(store.get("agg.row.count")).thenReturn(null);

        directive.execute(inputRows, context); // intermediate execution

        when(store.get("agg.total.size")).thenReturn(1_000_000L + 2_000_000L);
        when(store.get("agg.total.time")).thenReturn(1_000L + 3_000L);
        when(store.get("agg.row.count")).thenReturn(2L);

        List<Row> result = directive.execute(Collections.emptyList(), context);
        assertEquals(1, result.size());

        Row avgRow = result.get(0);

        double expectedAvgMB = (3_000_000 / 2.0) / 1_000_000.0;
        double expectedAvgSec = (4_000 / 2.0) / 1000.0;

        assertEquals(expectedAvgMB, avgRow.getValue("totalSize"));
        assertEquals(expectedAvgSec, avgRow.getValue("totalTime"));
    }
}
