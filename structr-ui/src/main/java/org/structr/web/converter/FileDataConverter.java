/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.converter;

import java.io.IOException;
import java.util.logging.Level;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.lang3.StringUtils;

import org.structr.web.common.ImageHelper;
import org.structr.common.KeyAndClass;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.FileHelper;
import org.structr.web.common.FileHelper.Base64URIData;
import org.structr.web.entity.FileBase;

//~--- classes ----------------------------------------------------------------

/**
 * Converts image data into an image node.
 *
 * If a {@link KeyAndClass} object is given, the image will be created with
 * the corresponding type and with setProperty to the given property key.
 *
 * If no {@link KeyAndClass} object is given, the image data will be set on
 * the image node itself.
 *
 *
 */
public class FileDataConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(FileDataConverter.class.getName());

	public FileDataConverter(final SecurityContext securityContext, final GraphObject entity) {
		super(securityContext, entity);
	}

	@Override
	public Object convert(final Object source) throws FrameworkException {

		if (source == null) {
			return false;
		}

		final FileBase currentFile = (FileBase)currentObject;

		if (source instanceof byte[]) {

			try {
				byte[] data      = (byte[]) source;
				MagicMatch match = Magic.getMagicMatch(data);
				String mimeType  = match.getMimeType();

				try {
					FileHelper.setFileData(currentFile, data, mimeType);

				} catch (IOException ioex) {

					logger.log(Level.WARNING, "Unable to store file", ioex);
				}

			} catch (MagicException | MagicParseException | MagicMatchNotFoundException mex) {

				logger.log(Level.WARNING, "Unable to parse file data", mex);
			}

		} else if (source instanceof String) {

			String sourceString = (String) source;

			if (StringUtils.isNotBlank(sourceString)) {

				final Base64URIData uriData = new Base64URIData(sourceString);
				if (uriData != null) {

					try {

						FileHelper.setFileData(currentFile, uriData.getBinaryData(), uriData.getContentType());

					} catch (IOException ioex) {

						logger.log(Level.WARNING, "Unable to store file", ioex);
					}
				}
			}
		}

		return null;
	}

	@Override
	public Object revert(Object source) {

		if (currentObject instanceof FileBase) {

			final FileBase currentFile = (FileBase)currentObject;
			return ImageHelper.getBase64String(currentFile);

		} else {
			return source;
		}
	}

}