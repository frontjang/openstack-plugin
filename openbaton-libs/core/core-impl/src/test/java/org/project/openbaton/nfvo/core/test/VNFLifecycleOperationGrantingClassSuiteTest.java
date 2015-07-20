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

package org.project.openbaton.nfvo.core.test;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.project.openbaton.catalogue.mano.common.*;
import org.project.openbaton.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.project.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.NFVImage;
import org.project.openbaton.catalogue.nfvo.Network;
import org.project.openbaton.catalogue.nfvo.Quota;
import org.project.openbaton.catalogue.nfvo.VimInstance;
import org.project.openbaton.nfvo.core.interfaces.NetworkServiceDescriptorManagement;
import org.project.openbaton.nfvo.core.interfaces.VNFLifecycleOperationGranting;
import org.project.openbaton.nfvo.exceptions.BadFormatException;
import org.project.openbaton.nfvo.exceptions.NotFoundException;
import org.project.openbaton.nfvo.exceptions.VimException;
import org.project.openbaton.nfvo.repositories_interfaces.GenericRepository;
import org.project.openbaton.nfvo.vim_interfaces.vim.VimBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by lto on 20/04/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration(classes = { ApplicationTest.class })
@TestPropertySource(properties = { "timezone = GMT", "port: 4242" })
public class VNFLifecycleOperationGrantingClassSuiteTest {

	private Logger log = LoggerFactory.getLogger(ApplicationTest.class);

	@Autowired
	private VNFLifecycleOperationGranting vnfLifecycleOperationGranting;

	@Autowired
	private VimBroker vimBroker;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(ApplicationTest.class);
		log.info("Starting test");
	}

	@Test
	public void vnfLifecycleOperationGrantingTest() throws VimException {
		VirtualNetworkFunctionRecord vnfr = createVirtualNetworkFunctionRecord();
		boolean granted;

		when(vimBroker.getLeftQuota(any(VimInstance.class))).thenReturn(createMaxQuota());
		granted = vnfLifecycleOperationGranting.grantLifecycleOperation(vnfr);
		Assert.assertTrue(granted);

		when(vimBroker.getLeftQuota(any(VimInstance.class))).thenReturn(createMinQuota());
		granted = vnfLifecycleOperationGranting.grantLifecycleOperation(vnfr);
		Assert.assertFalse(granted);
	}

	private VirtualNetworkFunctionRecord createVirtualNetworkFunctionRecord() {
		VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = new VirtualNetworkFunctionRecord();
		virtualNetworkFunctionRecord
				.setMonitoring_parameter(new HashSet<String>() {
					{
						add("monitor1");
						add("monitor2");
						add("monitor3");
					}
				});
		VNFDeploymentFlavour vdf = new VNFDeploymentFlavour();
		vdf.setExtId("mocked_vdu_ext_id");
		vdf.setFlavour_key("mocked_flavor_name_1");
		virtualNetworkFunctionRecord.setName("mocked_vnfr");
		virtualNetworkFunctionRecord.setDeployment_flavour_key(vdf.getFlavour_key());
		virtualNetworkFunctionRecord.setVdu(new HashSet<VirtualDeploymentUnit>());
		VimInstance vimInstance = createVimInstance();
		for (int i = 1; i <= 3; i++) {
			virtualNetworkFunctionRecord.getVdu().add(createVDU(i, vimInstance));
		}
		return virtualNetworkFunctionRecord;
	}

	private VirtualDeploymentUnit createVDU(int suffix, VimInstance vimInstance) {
		VirtualDeploymentUnit vdu = new VirtualDeploymentUnit();
		vdu.setHostname("mocked_vdu_hostname_" + suffix);
		vdu.setHigh_availability(HighAvailability.ACTIVE_ACTIVE);
		vdu.setVm_image(new HashSet<String>() {{
			add("mocked_image");
		}});
		vdu.setComputation_requirement("high_requirements");
		vdu.setVnfc(new HashSet<VNFComponent>());
		vdu.setLifecycle_event(new HashSet<LifecycleEvent>());
		vdu.setMonitoring_parameter(new HashSet<String>());
		vdu.setVimInstance(vimInstance);
		return vdu;
	}

	private VimInstance createVimInstance() {
		VimInstance vimInstance = new VimInstance();
		vimInstance.setName("mocked_vim_instance");
		vimInstance.setType("mocked_test_type");
		vimInstance.setNetworks(new HashSet<Network>() {{
			Network network = new Network();
			network.setExtId("mocked_network_ext_id");
			network.setName("mocked_network_name");
			add(network);
		}});
		vimInstance.setFlavours(new HashSet<DeploymentFlavour>() {{
			DeploymentFlavour deploymentFlavour = new DeploymentFlavour();
			deploymentFlavour.setExtId("mocked_flavor_ext_id_1");
			deploymentFlavour.setFlavour_key("mocked_flavor_name_1");
			deploymentFlavour.setRam(1024);
			deploymentFlavour.setVcpus(2);
			add(deploymentFlavour);

			deploymentFlavour = new DeploymentFlavour();
			deploymentFlavour.setExtId("mocked_flavor_ext_id_2");
			deploymentFlavour.setFlavour_key("mocked_flavor_name_2");
			deploymentFlavour.setRam(1024);
			deploymentFlavour.setVcpus(2);
			add(deploymentFlavour);
		}});
		vimInstance.setImages(new HashSet<NFVImage>() {{
			NFVImage image = new NFVImage();
			image.setExtId("mocked_image_ext_id_1");
			image.setName("mocked_image_name_1");
			add(image);

			image = new NFVImage();
			image.setExtId("mocked_image_ext_id_2");
			image.setName("mocked_image_name_2");
			add(image);
		}});
		return vimInstance;
	}

	private Quota createMaxQuota() {
		Quota quota = new Quota();
		quota.setInstances(100);
		quota.setRam(50000);
		quota.setCores(400);
		return quota;
	}

	private Quota createMinQuota() {
		Quota quota = new Quota();
		quota.setInstances(1);
		quota.setRam(256);
		quota.setCores(1);
		return quota;
	}

}
