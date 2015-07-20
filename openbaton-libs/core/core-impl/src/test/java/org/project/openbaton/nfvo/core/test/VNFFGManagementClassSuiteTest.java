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
import org.project.openbaton.catalogue.mano.descriptor.*;
import org.project.openbaton.catalogue.nfvo.NFVImage;
import org.project.openbaton.catalogue.nfvo.Network;
import org.project.openbaton.catalogue.nfvo.VimInstance;
import org.project.openbaton.nfvo.core.interfaces.VNFFGManagement;
import org.project.openbaton.nfvo.repositories_interfaces.GenericRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by lto on 20/04/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration(classes = { ApplicationTest.class })
@TestPropertySource(properties = { "timezone = GMT", "port: 4242" })
public class VNFFGManagementClassSuiteTest {

	private Logger log = LoggerFactory.getLogger(ApplicationTest.class);

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Autowired
	private VNFFGManagement vnffgManagement;

	@Autowired
	@Qualifier("VNFFGDescriptorRepository")
	private GenericRepository<VNFForwardingGraphDescriptor> vnffgDescriptorRepository;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(ApplicationTest.class);
		log.info("Starting test");
	}

	@Test
	public void vnffgManagementNotNull(){
		Assert.assertNotNull(vnffgManagement);
	}

	@Test
	public void vnffgManagementUpdateTest(){
		exception.expect(UnsupportedOperationException.class);
		VNFForwardingGraphDescriptor vnffgDescriptor_exp = createVNFFGDescriptor();
		when(vnffgDescriptorRepository.find(vnffgDescriptor_exp.getId())).thenReturn(vnffgDescriptor_exp);

		VNFForwardingGraphDescriptor vnffgDescriptor_new = createVNFFGDescriptor();
		vnffgDescriptor_new.setVendor("UpdatedVendor");
		vnffgDescriptor_exp = vnffgManagement.update(vnffgDescriptor_new, vnffgDescriptor_exp.getId());

		assertEqualsVNFFG(vnffgDescriptor_exp, vnffgDescriptor_new);
	}

	private void assertEqualsVNFFG(VNFForwardingGraphDescriptor vnffgDescriptor_exp, VNFForwardingGraphDescriptor vnffgDescriptor_new) {
		Assert.assertEquals(vnffgDescriptor_exp.getVendor(), vnffgDescriptor_new.getVendor());
		Assert.assertEquals(vnffgDescriptor_exp.getId(), vnffgDescriptor_new.getId());
		Assert.assertEquals(vnffgDescriptor_exp.getDescriptor_version(), vnffgDescriptor_new.getDescriptor_version());
	}

	private VNFForwardingGraphDescriptor createVNFFGDescriptor() {
		VNFForwardingGraphDescriptor vnffgDescriptor = new VNFForwardingGraphDescriptor();
		vnffgDescriptor.setVendor("vendor");
		vnffgDescriptor.setConnection_point(new HashSet<ConnectionPoint>());
		ConnectionPoint connectionPoint = new ConnectionPoint();
		connectionPoint.setType("type");
		vnffgDescriptor.getConnection_point().add(connectionPoint);
		HashSet<CostituentVNF> constituent_vnfs = new HashSet<>();
		CostituentVNF costituentVNF = new CostituentVNF();
		costituentVNF.setAffinity("affinity");
		costituentVNF.setCapability("capability");
		costituentVNF.setNumber_of_instances(3);
		costituentVNF.setRedundancy_model(RedundancyModel.ACTIVE);
		costituentVNF.setVnf_flavour_id_reference("flavor_id");
		costituentVNF.setVnf_reference("vnf_id");
		constituent_vnfs.add(costituentVNF);
		vnffgDescriptor.setConstituent_vnfs(constituent_vnfs);
		vnffgDescriptor.setNumber_of_endpoints(2);
		vnffgDescriptor.setVersion("version");
		vnffgDescriptor.setNumber_of_virtual_links(2);
		HashSet<VirtualLinkDescriptor> dependent_virtual_link = new HashSet<>();
		VirtualLinkDescriptor virtualLinkDescriptor = new VirtualLinkDescriptor();
		virtualLinkDescriptor.setVld_security(new Security());
		virtualLinkDescriptor.setVendor("vendor");
		virtualLinkDescriptor.setTest_access(new HashSet<String>() {{
			add("test_access");
		}});
		virtualLinkDescriptor.setLeaf_requirement("leaf_requirement");
		virtualLinkDescriptor.setNumber_of_endpoints(1);
		virtualLinkDescriptor.setDescriptor_version("version");
		virtualLinkDescriptor.setConnectivity_type("tyxpe");
		virtualLinkDescriptor.setQos(new HashSet<String>() {{
			add("qos");
		}});
		virtualLinkDescriptor.setConnection(new HashSet<String>() {{
			add("connection");
		}});
		virtualLinkDescriptor.setRoot_requirement("root_requirement");
		dependent_virtual_link.add(virtualLinkDescriptor);
		vnffgDescriptor.setDependent_virtual_link(dependent_virtual_link);
		vnffgDescriptor.setVnffgd_security(new Security());
		return vnffgDescriptor;
	}

	@Test
	public void vnffgManagementAddTest(){
		VNFForwardingGraphDescriptor vnffgDescriptor_exp = createVNFFGDescriptor();
		when(vnffgDescriptorRepository.create(any(VNFForwardingGraphDescriptor.class))).thenReturn(vnffgDescriptor_exp);
		VNFForwardingGraphDescriptor vnffgDescriptor_new = vnffgManagement.add(vnffgDescriptor_exp);

		assertEqualsVNFFG(vnffgDescriptor_exp, vnffgDescriptor_new);
	}

	@Test
	public void vnffgManagementQueryTest(){
		when(vnffgDescriptorRepository.findAll()).thenReturn(new ArrayList<VNFForwardingGraphDescriptor>());

		Assert.assertEquals(0, vnffgManagement.query().size());

		VNFForwardingGraphDescriptor vnffgDescriptor_exp = createVNFFGDescriptor();
		when(vnffgDescriptorRepository.find(vnffgDescriptor_exp.getId())).thenReturn(vnffgDescriptor_exp);
		VNFForwardingGraphDescriptor vnffgDescriptor_new = vnffgManagement.query(vnffgDescriptor_exp.getId());
		assertEqualsVNFFG(vnffgDescriptor_exp,vnffgDescriptor_new);
	}

	@Test
	public void vnffgManagementDeleteTest(){
		VNFForwardingGraphDescriptor vnffgDescriptor_exp = createVNFFGDescriptor();
		when(vnffgDescriptorRepository.find(vnffgDescriptor_exp.getId())).thenReturn(vnffgDescriptor_exp);
		vnffgManagement.delete(vnffgDescriptor_exp.getId());
		when(vnffgDescriptorRepository.find(vnffgDescriptor_exp.getId())).thenReturn(null);
		VNFForwardingGraphDescriptor vnffgDescriptor_new = vnffgManagement.query(vnffgDescriptor_exp.getId());
		Assert.assertNull(vnffgDescriptor_new);
	}

	@AfterClass
	public static void shutdown() {
		// TODO Teardown to avoid exceptions during test shutdown
	}


	private NFVImage createNfvImage() {
		NFVImage nfvImage = new NFVImage();
		nfvImage.setName("image_name");
		nfvImage.setExtId("ext_id");
		nfvImage.setMinCPU("1");
		nfvImage.setMinRam(1024);
		return nfvImage;
	}

	private NetworkServiceDescriptor createNetworkServiceDescriptor() {
		final NetworkServiceDescriptor nsd = new NetworkServiceDescriptor();
		nsd.setVendor("FOKUS");
		Set<VirtualNetworkFunctionDescriptor> virtualNetworkFunctionDescriptors = new HashSet<VirtualNetworkFunctionDescriptor>();
		VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor = new VirtualNetworkFunctionDescriptor();
		virtualNetworkFunctionDescriptor
				.setMonitoring_parameter(new HashSet<String>() {
					{
						add("monitor1");
						add("monitor2");
						add("monitor3");
					}
				});
		virtualNetworkFunctionDescriptor.setDeployment_flavour(new HashSet<VNFDeploymentFlavour>() {{
			VNFDeploymentFlavour vdf = new VNFDeploymentFlavour();
			vdf.setExtId("ext_id");
			vdf.setFlavour_key("flavor_name");
			add(vdf);
		}});
		virtualNetworkFunctionDescriptor
				.setVdu(new HashSet<VirtualDeploymentUnit>() {
					{
						VirtualDeploymentUnit vdu = new VirtualDeploymentUnit();
						vdu.setHigh_availability(HighAvailability.ACTIVE_ACTIVE);
						vdu.setComputation_requirement("high_requirements");
						VimInstance vimInstance = new VimInstance();
						vimInstance.setName("vim_instance");
						vimInstance.setType("test");
						vdu.setVimInstance(vimInstance);
						add(vdu);
					}
				});
		virtualNetworkFunctionDescriptors.add(virtualNetworkFunctionDescriptor);
		nsd.setVnfd(virtualNetworkFunctionDescriptors);
		return nsd;
	}

	private VimInstance createVimInstance() {
		VimInstance vimInstance = new VimInstance();
		vimInstance.setName("vim_instance");
		vimInstance.setType("test");
		vimInstance.setNetworks(new HashSet<Network>() {{
			Network network = new Network();
			network.setExtId("ext_id");
			network.setName("network_name");
			add(network);
		}});
		vimInstance.setFlavours(new HashSet<DeploymentFlavour>() {{
			DeploymentFlavour deploymentFlavour = new DeploymentFlavour();
			deploymentFlavour.setExtId("ext_id_1");
			deploymentFlavour.setFlavour_key("flavor_name");
			add(deploymentFlavour);

			deploymentFlavour = new DeploymentFlavour();
			deploymentFlavour.setExtId("ext_id_2");
			deploymentFlavour.setFlavour_key("m1.tiny");
			add(deploymentFlavour);
		}});
		vimInstance.setImages(new HashSet<NFVImage>() {{
			NFVImage image = new NFVImage();
			image.setExtId("ext_id_1");
			image.setName("ubuntu-14.04-server-cloudimg-amd64-disk1");
			add(image);

			image = new NFVImage();
			image.setExtId("ext_id_2");
			image.setName("image_name_1");
			add(image);
		}});
		return vimInstance;
	}

}
