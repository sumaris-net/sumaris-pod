package net.sumaris.core.dao.technical.jpa;

import com.google.common.collect.Maps;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Map;

public abstract class SpecificationWithParameters<T> implements Specification<T> {
        
        private Map<String, ParameterExpression<?>> parameters = Maps.newHashMap();


        public <R> ParameterExpression<R> get(String name) {
            return (ParameterExpression<R>) parameters.get(name);
        }

        protected <R> ParameterExpression<R> add(ParameterExpression<R> param) {
                parameters.put(param.getName(), param);
                return param;
        }


}