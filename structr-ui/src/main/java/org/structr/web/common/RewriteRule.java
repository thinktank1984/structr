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
package org.structr.web.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.api.config.Settings;

/**
 * Custom rewrite rule to transport the "force https" config setting
 * into the rewrite engine to evaluate.
 */
public class RewriteRule extends org.tuckey.web.filters.urlrewrite.extend.RewriteRule {

	public void checkConfig(final HttpServletRequest request, final HttpServletResponse response) {

		// Set a request attribute that the URL rewrite engine can evaluate to find
		// out whether forced redirect to HTTPS is desired. The setting can be found
		// in urlrewrite.xml:17.
		request.setAttribute("structr.force.https", Settings.ForceHttps.getValue());
	}
}
