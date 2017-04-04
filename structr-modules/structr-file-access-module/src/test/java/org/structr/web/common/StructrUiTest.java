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
package org.structr.web.common;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------
/**
 * Base class for all structr UI tests
 *
 * All tests are executed in superuser context
 *
 *
 */
public abstract class StructrUiTest extends TestCase {

	protected Properties config                   = new Properties();
	protected GraphDatabaseCommand graphDbCommand = null;
	protected SecurityContext securityContext     = null;

	protected App app = null;

	// the jetty server
	private boolean running = false;
	protected String basePath;

	protected static final String prot = "http://";
	protected static final String restUrl = "/structr/rest";
	protected static final String htmlUrl = "/structr/html";
	protected static final String wsUrl = "/structr/ws";
	protected static final String host = "localhost";
	protected static final int httpPort = (System.getProperty("httpPort") != null ? Integer.parseInt(System.getProperty("httpPort")) : 8875);
	protected static final int ftpPort = (System.getProperty("ftpPort") != null ? Integer.parseInt(System.getProperty("ftpPort")) : 8876);

	protected static String baseUri;

	static {

		// check character set
		checkCharset();

		baseUri = prot + host + ":" + httpPort + htmlUrl + "/";
		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = prot + host + ":" + httpPort;
		RestAssured.port = httpPort;

	}

	//~--- methods --------------------------------------------------------
	@Override
	protected void setUp() throws Exception {
		setUp(null);
	}

	protected void setUp(final Map<String, Object> additionalConfig) {

		System.out.println("\n######################################################################################");
		System.out.println("# Starting " + getClass().getSimpleName() + "#" + getName());
		System.out.println("######################################################################################");

		final long timestamp = System.currentTimeMillis();

		basePath = "/tmp/structr-test-" + timestamp + "-" + System.nanoTime();

		// enable "just testing" flag to avoid JAR resource scanning
		Settings.Testing.setValue(true);
		Settings.Services.setValue("NodeService HttpService SchemaService");
		Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");
		Settings.LogDatabasePath.setValue(basePath + "/logDb.dat");

		Settings.RelationshipCacheSize.setValue(1000);
		Settings.NodeCacheSize.setValue(1000);

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet WebSocketServlet HtmlServlet");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		do {
			try {
				Thread.sleep(100);
			} catch (Throwable t) {
			}

		} while (!services.isInitialized());

		securityContext = SecurityContext.getSuperUserInstance();

		app = StructrApp.getInstance(securityContext);

		graphDbCommand = app.command(GraphDatabaseCommand.class);

	}

	@Override
	protected void tearDown() throws Exception {

		Services.getInstance().shutdown();

		File testDir = new File(basePath);
		int count = 0;

		// try up to 10 times to delete the directory
		while (testDir.exists() && count++ < 10) {

			try {

				if (testDir.isDirectory()) {

					FileUtils.deleteDirectory(testDir);

				} else {

					testDir.delete();
				}

			} catch (Throwable t) {
			}

			try {
				Thread.sleep(500);
			} catch (Throwable t) {
			}
		}

		super.tearDown();

		System.out.println("######################################################################################");
		System.out.println("# " + getClass().getSimpleName() + "#" + getName() + " finished.");
		System.out.println("######################################################################################\n");

	}

	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirs.
	 *
	 * @param directory The base directory
	 * @param packageName The package name for classes found inside the base
	 * directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {

		List<Class> classes = new ArrayList<>();

		if (!directory.exists()) {

			return classes;
		}

		File[] files = directory.listFiles();

		for (File file : files) {

			if (file.isDirectory()) {

				assert !file.getName().contains(".");

				classes.addAll(findClasses(file, packageName + "." + file.getName()));

			} else if (file.getName().endsWith(".class")) {

				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}

		}

		return classes;

	}

	protected <T extends NodeInterface> List<T> createTestNodes(final Class<T> type, final int number) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		props.put(AbstractNode.type, type.getSimpleName());

		List<T> nodes = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			props.put(AbstractNode.name, type.getSimpleName() + i);
			nodes.add(app.create(type, props));
		}

		return nodes;
	}

	protected <T extends NodeInterface> List<T> createTestNodes(final Class<T> type, final int number, final PropertyMap props) throws FrameworkException {

		List<T> nodes = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			nodes.add(app.create(type, props));
		}

		return nodes;
	}

	protected List<RelationshipInterface> createTestRelationships(final Class relType, final int number) throws FrameworkException {

		List<GenericNode> nodes = createTestNodes(GenericNode.class, 2);
		final GenericNode startNode = nodes.get(0);
		final GenericNode endNode   = nodes.get(1);

		List<RelationshipInterface> rels = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			rels.add(app.create(startNode, endNode, relType));
		}

		return rels;
	}

	//~--- get methods ----------------------------------------------------
	/**
	 * Get classes in given package and subpackages, accessible from the
	 * context class loader
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected static List<Class> getClasses(String packageName) throws ClassNotFoundException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assert classLoader != null;

		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<>();

		while (resources.hasMoreElements()) {

			URL resource = resources.nextElement();

			dirs.add(new File(resource.getFile()));

		}

		List<Class> classList = new ArrayList<>();

		for (File directory : dirs) {

			classList.addAll(findClasses(directory, packageName));
		}

		return classList;

	}

	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected void grant(final String signature, final long flags, final boolean reset) {

		if (reset) {

			// delete existing grants
			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.header("X-User", "superadmin")
					.header("X-Password", "sehrgeheim")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))

				.expect()
					.statusCode(200)

				.when()
					.delete("/resource_access");
		}

		// list existing grants
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", "superadmin")
				.header("X-Password", "sehrgeheim")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.when()
				.get("/resource_access");

		// create new grant
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", "superadmin")
				.header("X-Password", "sehrgeheim")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.body(" { 'signature' : '" + signature + "', 'flags': " + flags + ", 'visibleToPublicUsers': true } ")

			.expect()
				.statusCode(201)

			.when()
				.post("/resource_access");
	}

	protected void testGet(final String resource, final String username, final String password, final int expectedStatusCode) {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)

			.expect()
				.statusCode(expectedStatusCode)

			.when()
				.get(resource);

	}

	protected void testPost(final String resource, final String username, final String password, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.body(body)

			.expect()
				.statusCode(expectedStatusCode)

			.when()
				.post(resource);
	}

	protected void testPut(final String resource, final String username, final String password, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.body(body)

			.expect()
				.statusCode(expectedStatusCode)

			.when()
				.put(resource);
	}

	protected void testDelete(final String resource, final String username, final String password, final int expectedStatusCode) {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)

			.expect()
				.statusCode(expectedStatusCode)

			.when()
				.delete(resource);
	}

	private static void checkCharset() {

		System.out.println("######### Charset settings ##############");
		System.out.println("Default Charset=" + Charset.defaultCharset());
		System.out.println("file.encoding=" + System.getProperty("file.encoding"));
		System.out.println("Default Charset=" + Charset.defaultCharset());
		System.out.println("Default Charset in Use=" + getEncodingInUse());
		System.out.println("This should look like the umlauts of 'a', 'o', 'u' and 'ss': äöüß");
		System.out.println("#########################################");

	}

	private static String getEncodingInUse() {
		OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
		return writer.getEncoding();
	}

	// disabled to be able to test on windows systems
//	public void testCharset() {
//		assertTrue(StringUtils.remove(getEncodingInUse().toLowerCase(), "-").equals("utf8"));
//	}
	protected void makePublic(final Object... objects) throws FrameworkException {

		for (Object obj : objects) {
			((GraphObject) obj).setProperty(GraphObject.visibleToPublicUsers, true);
		}

	}

	protected <T> List<T> toList(final T... elements) {
		return Arrays.asList(elements);
	}

	protected Map<String, byte[]> toMap(final Pair... pairs) {

		final Map<String, byte[]> map = new LinkedHashMap<>();

		for (final Pair pair : pairs) {
			map.put(pair.key, pair.value);
		}

		return map;
	}

	public static class Pair {

		public String key   = null;
		public byte[] value = null;

		public Pair(final String key, final byte[] value) {

			this.key = key;
			this.value = value;
		}
	}
}
