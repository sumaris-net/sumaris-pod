/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.core.dao.technical.model;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeNodeEntities {

    public static <T extends ITreeNodeEntityBean<? extends Serializable, T>> List<T> treeAsList(T rootNode) {
        return streamAll(rootNode).collect(Collectors.toList());
    }

    public static <T extends ITreeNodeEntityBean<? extends Serializable, T>> Stream<T> streamAll(T rootNode) {
        Stream.Builder<T> result = Stream.<T>builder();
        appendToStream(rootNode, result);
        return result.build();
    }

    public static <T extends ITreeNodeEntityBean<? extends Serializable, T>, R extends ITreeNodeEntityBean<? extends Serializable, R>>
        Stream<R> streamAllAndMap(T rootNode, BiFunction<T, R, R> mapFunction) {
        Stream.Builder<R> result = Stream.<R>builder();
        mapAndAppendToStream(rootNode, result, mapFunction, null);
        return result.build();
    }

    public static <ID extends Serializable, T extends ITreeNodeEntityBean<ID, T>> T listAsTree(Collection<T> nodes, Function<T, ID> getParentId) {

        nodes.forEach(node -> {
            if (node.getParent() == null && getParentId != null) {
                ID parentId = getParentId.apply(node);
                if (parentId != null) {
                    nodes.stream().filter(n -> parentId.equals(n.getId()))
                            .findFirst()
                            .ifPresent(node::setParent);
                }
            }
        });

        return  nodes.stream().filter(n -> n.getParent() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid input list: no root node found. Expected to have a node without parent."));
    }

    /* -- protected functions -- */

    protected static <T extends ITreeNodeEntityBean<? extends Serializable, T>> void appendToStream(final T node, final Stream.Builder<T> result) {
        if (node == null) return;

        // Add current node
        result.add(node);

        // Process children
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
            // Recursive call
            node.getChildren().forEach(child -> appendToStream(child, result));
        }
    }


    protected static <T extends ITreeNodeEntityBean<? extends Serializable, T>, R extends ITreeNodeEntityBean<? extends Serializable, R>>
        void mapAndAppendToStream(final T node, final Stream.Builder<R> result, final BiFunction<T, R, R> mapFunction, R parentNode) {
        if (node == null) return;

        // Visit current
        R mappedNode = mapFunction.apply(node, parentNode);

        // Add visited node
        result.add(mappedNode);

        // Process children
        if (CollectionUtils.isNotEmpty(node.getChildren())) {
            // Recursive call
            node.getChildren().forEach(child -> mapAndAppendToStream(child, result, mapFunction, mappedNode));
        }
    }
}
