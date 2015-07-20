/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.project.openbaton.nfvo.core.tests.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.project.openbaton.nfvo.api.RestVimInstances;
import org.project.openbaton.catalogue.nfvo.VimInstance;

import org.project.openbaton.nfvo.core.interfaces.VimManagement;
import org.project.openbaton.nfvo.exceptions.VimException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;



public class ApiRestVimInstancesTest {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@InjectMocks
	RestVimInstances restVimInstances;

	@Mock
	private VimManagement mock;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void findAllVimInstances() {
		log.info("" + mock.query());
		List<VimInstance> list = mock.query();
		when(mock.query()).thenReturn(list);
		assertEquals(list, restVimInstances.findAll());
	}

	@Test
	public void createVimInstance() throws VimException {
		VimInstance datacenter = new VimInstance();
		datacenter.setId("123");
		datacenter.setName("DC-1");
		datacenter.setType("OpenStack");
		datacenter.setName("datacenter_test");
		when(mock.add(datacenter)).thenReturn(datacenter);
		log.info("" + restVimInstances.create(datacenter));
		VimInstance datacenter2 = restVimInstances.create(datacenter);
		assertEquals(datacenter, datacenter2);
		
	}

	@Test
	public void findByIdVimInstance() {
		VimInstance datacenter = new VimInstance();
		datacenter.setId("123");
		datacenter.setName("DC-1");
		datacenter.setType("OpenStack");
		datacenter.setName("datacenter_test");
		when(mock.query(datacenter.getId())).thenReturn(datacenter);
		assertEquals(datacenter, restVimInstances.findById(datacenter.getId()));
	}

	@Test
	public void updateVimInstance() throws VimException {
		VimInstance datacenter = new VimInstance();
		datacenter.setId("123");
		datacenter.setName("DC-1");
		datacenter.setType("OpenStack");
		datacenter.setName("datacenter_test");
		when(mock.update(datacenter, datacenter.getId())).thenReturn(datacenter);
		assertEquals(datacenter, restVimInstances.update(datacenter, datacenter.getId()));
	}

	@Test
	public void deleteVimInstance() {
		mock.delete("123");
		restVimInstances.delete("123");
	}
}
