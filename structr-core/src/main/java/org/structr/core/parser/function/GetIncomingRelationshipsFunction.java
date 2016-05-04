/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.parser.function;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetIncomingRelationshipsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS    = "Usage: ${get_incoming_relationships(from, to [, relType])}. Example: ${get_incoming_relationships(me, user, 'FOLLOWS')}";
	public static final String ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS_JS = "Usage: ${{Structr.get_incoming_relationships(from, to [, relType])}}. Example: ${{Structr.get_incoming_relationships(Structr.get('me'), user, 'FOLLOWS')}}";

	@Override
	public String getName() {
		return "get_incoming_relationships()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final List<AbstractRelationship> list = new ArrayList<>();

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3)) {

			final Object source = sources[0];
			final Object target = sources[1];

			AbstractNode sourceNode = null;
			AbstractNode targetNode = null;

			if (source instanceof AbstractNode && target instanceof AbstractNode) {

				sourceNode = (AbstractNode)source;
				targetNode = (AbstractNode)target;

			} else {

				logger.log(Level.WARNING, "Error: entities are not nodes. Parameters: {0}", getParametersAsString(sources));
				return "Error: entities are not nodes.";

			}

			if (sources.length == 2) {

				for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

					final NodeInterface s = rel.getSourceNode();
					final NodeInterface t = rel.getTargetNode();

					// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
					if (s != null && t != null
						&& s.equals(targetNode) && t.equals(sourceNode)) {
						list.add(rel);
					}
				}

			} else if (sources.length == 3) {

				// dont try to create the relClass because we would need to do that both ways!!! otherwise it just fails if the nodes are in the "wrong" order (see tests:890f)
				final String relType = (String)sources[2];

				for (final AbstractRelationship rel : sourceNode.getIncomingRelationships()) {

					final NodeInterface s = rel.getSourceNode();
					final NodeInterface t = rel.getTargetNode();

					// We need to check if current user can see source and target node which is often not the case for OWNS or SECURITY rels
					if (s != null && t != null
						&& rel.getRelType().name().equals(relType)
						&& s.equals(targetNode) && t.equals(sourceNode)) {
						list.add(rel);
					}
				}

			}

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

		}

		return list;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS_JS : ERROR_MESSAGE_GET_INCOMING_RELATIONSHIPS);
	}

	@Override
	public String shortDescription() {
		return "Returns the incoming relationships of the given entity with an optional relationship type";
	}

}
