/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2019 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2019 LSC Project
 *         Raphael Ouazana <rouazana@linagora.com>
 ****************************************************************************
 */
package org.lsc.plugins.connectors.james;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscDatasets;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.PluginConnectionType;
import org.lsc.configuration.PluginDestinationServiceType;
import org.lsc.configuration.ServiceType.Connection;
import org.lsc.configuration.TaskType;
import org.lsc.plugins.connectors.james.generated.JamesAliasService;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

public class JamesAliasDstServiceTest {
	private static final int JAMES_WEBADMIN_PORT = 8000;
	private static int MAPPED_JAMES_WEBADMIN_PORT;
	private static final boolean FROM_SAME_SERVICE = true;

	private static TaskType task;
	private static GenericContainer<?> james;
    
	private JamesAliasDstService testee;
	
    @BeforeAll
    static void setup() {
    	james = new GenericContainer<>("linagora/james-memory:openpaas-1.5.2");
    	james.withExposedPorts(JAMES_WEBADMIN_PORT)
			.start();
		
    	MAPPED_JAMES_WEBADMIN_PORT = james.getMappedPort(JAMES_WEBADMIN_PORT);
		JamesAliasService jamesAliasService = mock(JamesAliasService.class);
		PluginDestinationServiceType pluginDestinationService = mock(PluginDestinationServiceType.class);
		PluginConnectionType jamesConnection = mock(PluginConnectionType.class);
		Connection connection = mock(Connection.class);
		task = mock(TaskType.class);
		
		when(jamesConnection.getUrl()).thenReturn("http://localhost:"  + MAPPED_JAMES_WEBADMIN_PORT);
		when(jamesConnection.getUsername()).thenReturn("admin@open-paas.org");
		when(jamesConnection.getPassword()).thenReturn("secret");
		when(connection.getReference()).thenReturn(jamesConnection);
		when(jamesAliasService.getConnection()).thenReturn(connection);
		when(task.getBean()).thenReturn("org.lsc.beans.SimpleBean");
		when(task.getPluginDestinationService()).thenReturn(pluginDestinationService);
		when(pluginDestinationService.getAny()).thenReturn(ImmutableList.of(jamesAliasService));

//		PreemptiveBasicAuthScheme basicAuthScheme = new PreemptiveBasicAuthScheme();
//		basicAuthScheme.setUserName("admin@open-paas.org");
//		basicAuthScheme.setPassword("secret");
        RestAssured.requestSpecification = new RequestSpecBuilder()
        		.setPort(MAPPED_JAMES_WEBADMIN_PORT)
//        		.setAuth(basicAuthScheme)
        		.setContentType(ContentType.JSON)
        		.setAccept(ContentType.JSON)
        		.setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .setBasePath("/address/aliases")
                .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    @AfterEach
    void cleanAllGroups() {
    	List<String> usersWithAliases = with()
    		.get("")
    		.jsonPath()
			.getList("");
    	usersWithAliases.forEach(this::deleteUserWithAliases);
    }
    
    private void deleteUserWithAliases(String id) {
    	with()
    		.delete("/{id}", id)
		.then()
			.statusCode(HttpStatus.SC_NO_CONTENT);
    }
    
    @AfterAll
    static void close() {
    	james.close();
    }
    
    @Test
    void jamesAliasesApiShouldReturnEmptyByDefault() throws Exception {
    	given()
		.when()
			.get("")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body("", hasSize(0));
    }
    
}
