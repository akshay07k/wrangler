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

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.parser.DirectivesBaseVisitor;
import io.cdap.wrangler.parser.DirectivesParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomDirectivesVisitor extends the generated DirectivesBaseVisitor and
 * converts parse tree nodes into corresponding Token objects.
 *
 * <p>
 * Hint: Use ctx.getText() to retrieve the text of each node and determine if it
 * matches the pattern for a BYTE_SIZE or TIME_DURATION token.
 * </p>
 *
 * <p>
 * This visitor collects tokens in a TokenGroup (implemented here as a List of
 * Tokens)
 * so that they can be used for directive execution.
 * </p>
 */
public class CustomDirectivesVisitor extends DirectivesBaseVisitor<List<Token>> {

  /**
   * Visits a directive context and constructs a token group from its values.
   *
   * @param ctx the directive context
   * @return a list of Token objects representing the directive's arguments.
   */
  @Override
  public List<Token> visitDirective(DirectivesParser.DirectiveContext ctx) {
    List<Token> tokenGroup = new ArrayList<>();

    // Iterate over each value node in the directive context.
    for (ParseTree child : ctx.children) {
      if (child instanceof DirectivesParser.ValueContext) {
        tokenGroup.addAll(visitValue((DirectivesParser.ValueContext) child));
      }
    }
    return tokenGroup;
  }

  /**
   * Visits a generic value context. This method checks if the node's text matches
   * the
   * patterns for BYTE_SIZE or TIME_DURATION. The hint "chk ctx.getText()" reminds
   * us to
   * use the textual content from the parse tree.
   *
   * @param ctx the value context
   * @return the corresponding Token (ByteSize, TimeDuration, or a default token
   *         via super.visitValue(ctx))
   */
  @Override
  public List<Token> visitValue(DirectivesParser.ValueContext ctx) {
    String text = ctx.String().getText().toUpperCase();
    List<Token> tokens = new ArrayList<>();

    // Regex pattern for byte sizes: e.g., "10MB", "1.5GB" (case-insensitive).
    if (text.matches("(?i)^\\d+(\\.\\d+)?(KB|MB|GB|TB|PB|B)$")) {
      tokens.add(new ByteSize(text));
      return tokens;
    }

    text = text.toLowerCase();
    // Regex pattern for time durations: e.g., "150ms", "2.5h", "30sec"
    // (case-insensitive).
    if (text.matches("(?i)^\\d+(\\.\\d+)?(ms|s|sec|m|min|h|hr|d|day)$")) {
      tokens.add(new TimeDuration(text));
      return tokens;
    }
    // Otherwise, use the default implementation.
    return super.visitValue(ctx);
  }

  /**
   * Visits a dedicated byteSizeArg context, and returns a ByteSize token.
   *
   * @param ctx the byteSizeArg context
   * @return a ByteSize token based on the context text.
   */
  @Override
  public List<Token> visitByteSizeArg(DirectivesParser.ByteSizeArgContext ctx) {
    List<Token> tokens = new ArrayList<>();
    tokens.add(new ByteSize(ctx.getText()));
    return tokens;
  }

  /**
   * Visits a dedicated timeDurationArg context, and returns a TimeDuration token.
   *
   * @param ctx the timeDurationArg context
   * @return a TimeDuration token based on the context text.
   */
  @Override
  public List<Token> visitTimeDurationArg(DirectivesParser.TimeDurationArgContext ctx) {
    List<Token> tokens = new ArrayList<>();
    tokens.add(new TimeDuration(ctx.getText()));
    return tokens;
  }
}
