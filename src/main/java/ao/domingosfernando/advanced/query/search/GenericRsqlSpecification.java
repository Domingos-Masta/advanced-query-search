/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ao.domingosfernando.advanced.query.search;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import lombok.AllArgsConstructor;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author domingos.fernando
 * @param <T>
 */
@AllArgsConstructor
public class GenericRsqlSpecification<T> implements Specification<T>
{

    private final String property;
    private final ComparisonOperator operator;
    private final List<String> arguments;

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                 CriteriaBuilder builder)
    {
        Path<String> propertyExpression = parseProperty(root);
        List<Object> args = castArguments(propertyExpression);
        Object argument = args.get(0);
        query.distinct(true);

        switch (RsqlSearchOperation.getSimpleOperator(operator)) {
            case EQUAL:
                if (propertyExpression instanceof PluralAttributePath && argument.equals("empty")) {
                    return builder.isEmpty(root.get(property));
                }
                else if (argument == null || argument.equals("null")) {
                    return builder.isNull(propertyExpression);
                }
                else if (argument instanceof String) {
                    return builder.like(builder.lower(propertyExpression),
                            argument.toString().replace('*', '%').toLowerCase());
                }
                else {
                    return builder.equal(propertyExpression, argument);
                }

            case NOT_EQUAL:
                if (propertyExpression instanceof PluralAttributePath && argument.equals("empty")) {
                    return builder.isNotEmpty(root.get(property));
                }
                else if (argument == null || argument.equals("null")) {
                    return builder.isNotNull(propertyExpression);
                }
                else if (argument instanceof String) {
                    return builder.notLike(builder.lower(propertyExpression),
                            argument.toString().replace('*', '%').toLowerCase());
                }
                else {
                    return builder.notEqual(propertyExpression, argument);
                }

            case GREATER_THAN:
                if (argument instanceof Date) {
                    return builder.greaterThan(root.get(property), (Date) argument);
                }
                return builder.greaterThan(propertyExpression,
                        argument.toString());

            case GREATER_THAN_OR_EQUAL:
                if (argument instanceof Date) {
                    return builder.greaterThanOrEqualTo(root.get(property), (Date) argument);
                }
                return builder.greaterThanOrEqualTo(propertyExpression,
                        argument.toString());

            case LESS_THAN:
                if (argument instanceof Date) {
                    return builder.lessThan(root.get(property), (Date) argument);
                }
                return builder.lessThan(propertyExpression,
                        argument.toString());

            case LESS_THAN_OR_EQUAL:
                if (argument instanceof Date) {
                    return builder.lessThanOrEqualTo(root.get(property), (Date) argument);
                }
                return builder.lessThanOrEqualTo(propertyExpression,
                        argument.toString());
            case IN:
                return propertyExpression.in(args);
            case NOT_IN:
                return builder.not(propertyExpression.in(args));
        }

        return null;
    }

    // This method will help us diving deep into nested property using the dot convention
    // The originial tutorial did not have this, so it can only parse the shallow properties.
    private Path<String> parseProperty(Root<T> root)
    {
        Path<String> path;
        if (property.contains(".")) {
            // Nested properties
            String[] pathSteps = property.split("\\.");
            String step = pathSteps[0];
            path = root.get(step);
            From lastFrom = root;

            for (int i = 1; i <= pathSteps.length - 1; i++) {
                if (path instanceof PluralAttributePath) {
                    PluralAttribute attr = ((PluralAttributePath) path).getAttribute();
                    Join join = getJoin(attr, lastFrom);
                    path = join.get(pathSteps[i]);
                    lastFrom = join;
                }
                else if (path instanceof SingularAttributePath) {
                    SingularAttribute attr = ((SingularAttributePath) path).getAttribute();
                    if (attr.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC) {
                        Join join = lastFrom.join(attr, JoinType.LEFT);
                        path = join.get(pathSteps[i]);
                        lastFrom = join;
                    }
                    else {
                        path = path.get(pathSteps[i]);
                    }
                }
                else {
                    path = path.get(pathSteps[i]);
                }
            }
        }
        else {
            path = root.get(property);
        }
        return path;
    }

    private Join getJoin(PluralAttribute attr, From from)
    {
        switch (attr.getCollectionType()) {
            case COLLECTION:
                return from.join((CollectionAttribute) attr);
            case SET:
                return from.join((SetAttribute) attr);
            case LIST:
                return from.join((ListAttribute) attr);
            case MAP:
                return from.join((MapAttribute) attr);
            default:
                return null;
        }
    }

    private List<Object> castArguments(Path<?> propertyExpression)
    {
        Class<?> type = propertyExpression.getJavaType();

        return arguments.stream().map(arg -> {
            if (type.equals(Integer.class)) {
                return Integer.parseInt(arg);
            }
            else if (type.equals(Long.class)) {
                return Long.parseLong(arg);
            }
            else if (type.equals(Date.class)) {
                return strToDate(arg);
            }
            else if (type.equals(Byte.class)) {
                return Byte.parseByte(arg);
            }
            else if (type.equals(Boolean.class)) {
                return Boolean.parseBoolean(arg);
            }
            else {
                return arg;
            }
        }).collect(Collectors.toList());
    }

    private static Date strToDate(String data)
    {
        if (data == null) {
            return null;
        }

        Date dataF = null;
        try {
            DateFormat dateFormat;
            if (data.indexOf('-') >= 0) {
                int value = Integer.parseInt(data.split("-")[0]);
                if (value < 1000) {
                    dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                }
                else {
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                }
            }
            else {
                int value = Integer.parseInt(data.split("/")[0]);
                if (value < 1000) {
                    dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                }
                else {
                    dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                }
            }
            long timestamp = dateFormat.parse(data).getTime();
            dataF = new Date(timestamp);
        }
        catch (ParseException pe) {
            System.err.println("Erro ao converter String em data: " + pe.getLocalizedMessage());
        }
        return dataF;
    }

}
