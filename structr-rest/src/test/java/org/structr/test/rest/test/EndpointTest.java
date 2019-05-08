/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.test.rest.test;

import com.jayway.restassured.RestAssured;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class EndpointTest extends StructrRestTestBase {

	private static final Logger logger = LoggerFactory.getLogger(EndpointTest.class.getName());

	@Test
	public void testOnSaveConcurrency() {

		String projectUuid = null;
		String taskUuid    = null;

		// create schema type with onSave method
		try {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.addMethod("onSave", "error('nope', 'nope', 'nope')", "");

			project.relate(task, "HAS", Relation.Cardinality.ManyToMany, "projects", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// create schema type with onSave method
		try (final Tx tx = app.tx()) {

			final NodeInterface project = app.create(StructrApp.getConfiguration().getNodeEntityClass("Project"), "test");
			final NodeInterface task    = app.create(StructrApp.getConfiguration().getNodeEntityClass("Task"), "task");

			projectUuid = project.getUuid();
			taskUuid    = task.getUuid();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		final ExecutorService service  = Executors.newCachedThreadPool();
		final AtomicInteger numSuccess = new AtomicInteger();
		final AtomicInteger numFailure = new AtomicInteger();
		final List<Future> futures     = new LinkedList<>();
		final String finalProjectUuid  = projectUuid;
		final String finalTaskUuid     = taskUuid;
		final int num                  = 100;
		final long t0                  = System.currentTimeMillis();

		for (int i=0; i<num; i++) {

			final int index = i;

			futures.add(service.submit(() -> {

				for (int j=0; j<10; j++) {

					final int statusCode = RestAssured

						.given()
							.accept("application/json")
							//.body("{ tasks: [ " + finalTaskUuid + "], name: 'blah' }") // => KEIN Fehler
							//.body("{ name: blah" + index + " }") // => KEIN Fehler
							.body("{ name: blah }") // => Fehler

						.when()
							.put("/Project/" + finalProjectUuid)

						.andReturn()
						.statusCode();

					if (statusCode == 200) {
						numSuccess.incrementAndGet();
					} else {
						numFailure.incrementAndGet();
					}
				}
			}));
		}

		// wait for all futures to resolve
		for (final Future f : futures) {

			try {f.get(); } catch (Throwable t) {
			}
		}

		final long t1 = System.currentTimeMillis();

		service.shutdown();

		System.out.println("#############################################################################################");
		System.out.println("200:  " + numSuccess.get());
		System.out.println("422:  " + numFailure.get());
		System.out.println("Time: " + (t1-t0) + " ms");

		if (numSuccess.get() > 0) {

			fail("Concurrent property write access is broken.");
		}
	}

	/*
	@Test
	public void testRestInputValidation() {

		try {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");

			project.addStringProperty("name");

			StructrSchema.extendDatabaseSchema(app, schema);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		RestAssured.given().accept("application/json").body("{ name: blah }").expect().statusCode(422).when().post("/Project");

	}
	*/
}
