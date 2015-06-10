/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AnyTypeClassITCase extends AbstractITCase {

    @Test
    public void read() {
        AnyTypeClassTO minimalGroup = anyTypeClassService.read("minimal group");
        assertNotNull(minimalGroup);

        assertFalse(minimalGroup.getPlainSchemas().isEmpty());
        assertFalse(minimalGroup.getDerSchemas().isEmpty());
        assertFalse(minimalGroup.getVirSchemas().isEmpty());
    }

    @Test
    public void list() {
        List<AnyTypeClassTO> list = anyTypeClassService.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getPlainSchemas().add("firstname");

        Response response = anyTypeClassService.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertTrue(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());

        newClass.getDerSchemas().add("cn");
        anyTypeClassService.update(newClass);

        newClass = anyTypeClassService.read(newClass.getKey());
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertFalse(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());

        anyTypeClassService.delete(newClass.getKey());

        try {
            anyTypeClassService.read(newClass.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void deleteSchema() {
        PlainSchemaTO newSchema = new PlainSchemaTO();
        newSchema.setKey("newSchema" + getUUIDString());
        newSchema.setType(AttrSchemaType.Date);
        createSchema(SchemaType.PLAIN, newSchema);

        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getPlainSchemas().add(newSchema.getKey());

        Response response = anyTypeClassService.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);
        assertTrue(newClass.getPlainSchemas().contains(newSchema.getKey()));

        schemaService.delete(SchemaType.PLAIN, newSchema.getKey());

        newClass = anyTypeClassService.read(newClass.getKey());
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().contains(newSchema.getKey()));
    }
}
