/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ao.domingosfernando.advanced.query.search;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author domingos.fernando
 */
public final class AdvancedQuerySearch
{

    public static Specification<Object> getCustomSpecification(String search, Object object)
    {
        Node rootNode = new RSQLParser().parse(search);
        Specification<Object> specs = rootNode.accept(new CustomRsqlVisitor<>());
        return specs;
    }
}
