/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.text.model;

import java.net.URI;
import org.structr.core.entity.Relation;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonReferenceType;

/**
 *
 * @author Christian Morgner
 */
public interface StructuredDocument extends StructuredTextNode {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("StructuredDocument");
		final JsonObjectType meta = schema.addType("MetadataNode");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/StructuredDocument"));
		type.setExtends(URI.create("#/definitions/StructuredTextNode"));

		type.addStringProperty("title");
		type.addStringProperty("author");
		type.addStringProperty("status");

		type.addPropertyGetter("metadata", Iterable.class);

		final JsonReferenceType rel = type.relate(meta, "METADATA", Relation.Cardinality.OneToMany, "document", "metadata");

		// enable cascading delete for metadata nodes
		rel.setCascadingDelete(JsonSchema.Cascade.sourceToTarget);
	}}

	Iterable<MetadataNode> getMetadata();
}
