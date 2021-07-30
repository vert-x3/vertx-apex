package io.vertx.ext.web.validation.builder;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.validation.impl.parser.ValueParser;

/**
 * This interface is used to create {@link ValueParser} able to parse serialized array structures. <br/>
 *
 * Look at {@link Parsers} for all available factories
 */
@VertxGen
@FunctionalInterface
public interface ArrayParserFactory {

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ValueParser<String> newArrayParser(ValueParser<String> itemsParser);

}
