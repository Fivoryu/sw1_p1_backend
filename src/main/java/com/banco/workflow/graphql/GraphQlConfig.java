package com.banco.workflow.graphql;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;
import graphql.schema.idl.RuntimeWiring;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuración de GraphQL con custom scalars
 */
@Component
public class GraphQlConfig implements RuntimeWiringConfigurer {

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder.scalar(dateTimeScalar())
               .scalar(jsonScalar());
    }

    /**
     * Scalar personalizado para LocalDateTime
     */
    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("LocalDateTime scalar")
                .coercing(new Coercing<Object, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime dateTime) {
                            return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Cannot serialize " + dataFetcherResult);
                    }

                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof String stringInput) {
                            return LocalDateTime.parse(stringInput, 
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingParseValueException("Cannot parse " + input);
                    }

                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseValueException {
                        if (input instanceof String) {
                            return parseValue(input);
                        }
                        throw new CoercingParseValueException("Cannot parse literal " + input);
                    }
                })
                .build();
    }

    /**
     * Scalar personalizado para JSON
     */
    private GraphQLScalarType jsonScalar() {
        return GraphQLScalarType.newScalar()
                .name("JSON")
                .description("JSON scalar")
                .coercing(new Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        return dataFetcherResult;
                    }

                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        return input;
                    }

                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseValueException {
                        return input;
                    }
                })
                .build();
    }
}
