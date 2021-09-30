/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ao.domingosfernando.advanced.query.search;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author domingos.fernando
 * @param <T>
 */
public class CustomRsqlVisitor<T> implements RSQLVisitor<Specification<T>, Void>
{

    private final GenericRsqlSpecBuilder<T> builder;

    public CustomRsqlVisitor()
    {
        builder = new GenericRsqlSpecBuilder<>();
    }

    /**
     *
     * @param node
     * @param param
     * @return
     */
    @Override
    public Specification<T> visit(AndNode node, Void param)
    {
        return builder.createSpecification(node);
    }

    @Override
    public Specification<T> visit(OrNode node, Void param)
    {
        return builder.createSpecification(node);
    }

    @Override
    public Specification<T> visit(ComparisonNode node, Void params)
    {
        return builder.createSpecification(node);
    }

}
