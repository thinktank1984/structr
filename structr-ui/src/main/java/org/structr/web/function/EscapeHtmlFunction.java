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
package org.structr.web.function;

import org.apache.commons.lang.StringEscapeUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class EscapeHtmlFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_ESCAPE_HTML    = "Usage: ${escape_html(text)}. Example: ${escape_html(\"test & test\")}";
	public static final String ERROR_MESSAGE_ESCAPE_HTML_JS = "Usage: ${{Structr.escape_html(text)}}. Example: ${{Structr.escape_html(\"test & test\")}}";

	@Override
	public String getName() {
		return "escape_html";
	}

	@Override
	public String getSignature() {
		return "text";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return StringEscapeUtils.escapeHtml(sources[0].toString());

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ESCAPE_HTML_JS : ERROR_MESSAGE_ESCAPE_HTML);
	}

	@Override
	public String shortDescription() {
		return "Replaces HTML characters with their corresponding HTML entities";
	}
}
