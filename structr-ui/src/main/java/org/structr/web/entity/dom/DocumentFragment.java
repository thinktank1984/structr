/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.entity.dom;

import java.net.URI;
import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.w3c.dom.DOMException;

/**
 *
 *
 */

public interface DocumentFragment extends DOMNode, org.w3c.dom.DocumentFragment, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DocumentFragment");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DocumentFragment"));
		type.setExtends(URI.create("#/definitions/DOMNode"));

		// ----- interface org.w3c.dom.Node -----
		type.addMethod("getNodeName").setSource("return \"#document-fragment\";");
		type.addMethod("getLocalName").setSource("return null");
		type.addMethod("getNodeValue").setSource("return null").addException(DOMException.class.getName());
		type.addMethod("hasAttributes").setSource("return false").setReturnType("boolean");
		type.addMethod("getAttributes").setSource("return null").setReturnType("NamedNodeMap");
		type.addMethod("getNodeType").setSource("return DOCUMENT_FRAGMENT_NODE;");
		type.addMethod("setNodeValue").addException(DOMException.class.getName()).addParameter("value", "String");

		// ----- interface DOMNode -----
		type.addMethod("getContextName").setSource("return \"DocumentFragment\";").setReturnType("String");
		type.addMethod("isSynced").setSource("return false").setReturnType("boolean");
		type.addMethod("contentEquals").setSource("return false;").addParameter("otherNode", DOMNode.class.getName());
		type.addMethod("updateFromNode").addException(FrameworkException.class.getName()).addParameter("otherNode", DOMNode.class.getName());

		// ----- interface Renderable -----

		/*
		public void render(RenderContext renderContext, int depth) throws FrameworkException;
		public void renderContent(RenderContext renderContext, int depth) throws FrameworkException;
		*/
	}}

	/*
	@Override
	public String getContextName() {
		return "DocumentFragment";
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return "#document-fragment";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
	}

	@Override
	public short getNodeType() {
		return DOCUMENT_FRAGMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public boolean contentEquals(final DOMNode otherNode) {
		return false;
	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {
		// do nothing
	}

	@Override
	public boolean isSynced() {
		return false;
	}

	// ----- interface Renderable -----
	@Override
	public void render(RenderContext renderContext, int depth) throws FrameworkException {

		NodeList _children = getChildNodes();
		int len            = _children.getLength();

		for (int i=0; i<len; i++) {

			Node child = _children.item(i);

			if (child != null && child instanceof Renderable) {

				((Renderable)child).render(renderContext, depth);
			}
		}

	}

	@Override
	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {
	}

	@Override
	public Node doAdopt(Page newPage) throws DOMException {

		// do nothing, only children of DocumentFragments are
		// adopted
		return null;
	}

	@Override
	public Node doImport(Page newPage) throws DOMException {
		// simply return an empty DocumentFragment, as the importing
		// will be done by the Page method if deep importing is enabled.
		return newPage.createDocumentFragment();
	}
i	*/
}
