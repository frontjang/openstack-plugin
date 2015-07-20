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

package org.project.openbaton.nfvo.api;

import org.project.openbaton.catalogue.mano.common.Security;
import org.project.openbaton.catalogue.mano.common.VNFDependency;
import org.project.openbaton.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.project.openbaton.catalogue.mano.descriptor.PhysicalNetworkFunctionDescriptor;
import org.project.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.project.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.nfvo.api.exceptions.*;
import org.project.openbaton.nfvo.core.interfaces.NetworkServiceDescriptorManagement;
import org.project.openbaton.nfvo.core.interfaces.NetworkServiceRecordManagement;
import org.project.openbaton.nfvo.exceptions.BadFormatException;
import org.project.openbaton.nfvo.exceptions.NotFoundException;
import org.project.openbaton.nfvo.exceptions.QuotaExceededException;
import org.project.openbaton.nfvo.exceptions.VimException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.naming.NamingException;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1//ns-descriptors")
public class RestNetworkServiceDescriptor {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private NetworkServiceDescriptorManagement networkServiceDescriptorManagement;

	@Autowired
	private NetworkServiceRecordManagement networkServiceRecordManagement;

	/**
	 * This operation allows submitting and validating a Network Service
	 * Descriptor (NSD), including any related VNFFGD and VLD.
	 * 
	 * @param networkServiceDescriptor
	 *            : the Network Service Descriptor to be created
	 * @return networkServiceDescriptor: the Network Service Descriptor filled
	 *         with id and values from core
	 */
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public NetworkServiceDescriptor create(@RequestBody @Valid NetworkServiceDescriptor networkServiceDescriptor) {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.onboard(networkServiceDescriptor);
		} catch (NotFoundException e) {
			log.error(e.getMessage(), e);
			throw new EntityNotFoundException(e.getMessage());
		} catch (BadFormatException e) {
			log.error(e.getMessage(), e);
			throw new BadRequestException(e.getMessage());
		}
		return nsd;
	}

	/**
	 * This operation is used to remove a disabled Network Service Descriptor
	 * 
	 * @param id
	 *            : the id of Network Service Descriptor
	 * @ 
	 */
	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("id") String id)  {
		try {
			networkServiceDescriptorManagement.delete(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);

		}
	}

	/**
	 * This operation returns the list of Network Service Descriptor (NSD)
	 * 
	 * @return List<NetworkServiceDescriptor>: the list of Network Service
	 *         Descriptor stored
	 */

	@RequestMapping(method = RequestMethod.GET)
	public List<NetworkServiceDescriptor> findAll() {

		return networkServiceDescriptorManagement.query();
	}

	/**
	 * This operation returns the Network Service Descriptor (NSD) selected by
	 * id
	 * 
	 * @param id
	 *            : the id of Network Service Descriptor
	 * @return NetworkServiceDescriptor: the Network Service Descriptor selected
	 * @ 
	 */

	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	public NetworkServiceDescriptor findById(@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return nsd;
	}

	/**
	 * This operation updates the Network Service Descriptor (NSD)
	 * 
	 * @param networkServiceDescriptor
	 *            : the Network Service Descriptor to be updated
	 * @param id
	 *            : the id of Network Service Descriptor
	 * @return networkServiceDescriptor: the Network Service Descriptor updated
	 */

	@RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public NetworkServiceDescriptor update(
			@RequestBody @Valid NetworkServiceDescriptor networkServiceDescriptor,
			@PathVariable("id") String id) {
		return networkServiceDescriptorManagement.update(
				networkServiceDescriptor, id);
	}

	/**
	 * Returns the list of VirtualNetworkFunctionDescriptor into a NSD with id
	 * 
	 * @param id
	 *            : The id of NSD
	 * @return List<VirtualNetworkFunctionDescriptor>: The List of
	 *         VirtualNetworkFunctionDescriptor into NSD
	 * @ 
	 */
	@RequestMapping(value = "{id}/vnfdescriptors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Set<VirtualNetworkFunctionDescriptor> getVirtualNetworkFunctionDescriptors(
			@PathVariable("id") String id) {

		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return nsd.getVnfd();
	}

	@RequestMapping(value = "{id}/vnfdescriptors/{id_vfn}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public VirtualNetworkFunctionDescriptor getVirtualNetworkFunctionDescriptor(
			@PathVariable("id") String id, @PathVariable("id_vfn") String id_vfn)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return findVNF(nsd.getVnfd(), id_vfn);
	}

	@RequestMapping(value = "{id}/vnfdescriptors/{id_vfn}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteVirtualNetworkFunctionDescriptor(
			@PathVariable("id") String id, @PathVariable("id_vfn") String id_vfn)  {

		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		VirtualNetworkFunctionDescriptor nDescriptor = findVNF(nsd.getVnfd(),
				id_vfn);
		nsd.getVnfd().remove(nDescriptor);
	}

	@RequestMapping(value = "{id}/vnfdescriptors/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VirtualNetworkFunctionDescriptor postVNFD(
			@RequestBody @Valid VirtualNetworkFunctionDescriptor vnfDescriptor,
			@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		nsd.getVnfd().add(vnfDescriptor);
		networkServiceDescriptorManagement.update(nsd, id);
		return vnfDescriptor;
	}

	@RequestMapping(value = "{id}/vnfdescriptors/{id_vfn}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public VirtualNetworkFunctionDescriptor updateVNF(
			@RequestBody @Valid VirtualNetworkFunctionDescriptor vnfDescriptor,
			@PathVariable("id") String id, @PathVariable("id_vfn") String id_vfn)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}

		VirtualNetworkFunctionDescriptor nDescriptor = findVNF(nsd.getVnfd(),
				id_vfn);
		nDescriptor = vnfDescriptor;
		// TODO: replace all PUT like this
		// nsd.getVnfd().remove(nDescriptor);
		nsd.getVnfd().add(nDescriptor);
		networkServiceDescriptorManagement.update(nsd, id);
		return nDescriptor;
	}

	/**
	 * Returns the list of VNFDependency into a NSD with id
	 * 
	 * @param id
	 *            : The id of NSD
	 * @return List<VNFDependency>: The List of VNFDependency into NSD
	 * @ 
	 */

	@RequestMapping(value = "{id}/vnfdependencies", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Set<VNFDependency> getVNFDependencies(@PathVariable("id") String id) {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return nsd.getVnf_dependency();
	}

	@RequestMapping(value = "{id}/vnfdependencies/{id_vnfd}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public VNFDependency getVNFDependency(@PathVariable("id") String id,
			@PathVariable("id_vnfd") String id_vnfd)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}

		return findVNFD(nsd.getVnf_dependency(), id_vnfd);
	}

	@RequestMapping(value = "{id}/vnfdependencies/{id_vnfd}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteVNFDependency(@PathVariable("id") String id,
			@PathVariable("id_vnfd") String id_vnfd)  {

		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}

		VNFDependency vnfDependency = findVNFD(nsd.getVnf_dependency(), id_vnfd);
		nsd.getVnf_dependency().remove(vnfDependency);
	}

	@RequestMapping(value = "{id}/vnfdependencies/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VNFDependency postVNFDependency(
			@RequestBody @Valid VNFDependency vnfDependency,
			@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		nsd.getVnf_dependency().add(vnfDependency);
		networkServiceDescriptorManagement.update(nsd, id);
		return vnfDependency;
	}

	@RequestMapping(value = "{id}/vnfdependencies/{id_pnf}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public VNFDependency updateVNFD(
			@RequestBody @Valid VNFDependency vnfDependency,
			@PathVariable("id") String id,
			@PathVariable("id_pnf") String id_vnfd)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		VNFDependency vDependency = findVNFD(nsd.getVnf_dependency(), id_vnfd);
		vDependency = vnfDependency;
		nsd.getVnf_dependency().add(vDependency);
		networkServiceDescriptorManagement.update(nsd, id);
		return vnfDependency;
	}

	/**
	 * Returns the list of PhysicalNetworkFunctionDescriptor into a NSD with id
	 * 
	 * @param id
	 *            : The id of NSD
	 * @return List<PhysicalNetworkFunctionDescriptor>: The List of
	 *         PhysicalNetworkFunctionDescriptor into NSD
	 * @ 
	 */
	@RequestMapping(value = "{id}/pnfdescriptors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Set<PhysicalNetworkFunctionDescriptor> getPhysicalNetworkFunctionDescriptors(
			@PathVariable("id") String id) {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {

			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return nsd.getPnfd();
	}

	/**
	 * Returns the PhysicalNetworkFunctionDescriptor
	 * 
	 * @param id
	 *            : The NSD id
	 * @param id_pnf
	 *            : The PhysicalNetworkFunctionDescriptor id
	 * @return PhysicalNetworkFunctionDescriptor: The
	 *         PhysicalNetworkFunctionDescriptor selected
	 * @ 
	 */

	@RequestMapping(value = "{id}/pnfdescriptors/{id_pnf}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public PhysicalNetworkFunctionDescriptor getPhysicalNetworkFunctionDescriptor(
			@PathVariable("id") String id, @PathVariable("id_pnf") String id_pnf)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {

			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return findPNFD(nsd.getPnfd(), id_pnf);
	}

	/**
	 * Deletes the PhysicalNetworkFunctionDescriptor with the id_pnf
	 * 
	 * @param id
	 *            : The NSD id
	 * @param id_pnf
	 *            : The PhysicalNetworkFunctionDescriptor id
	 * @ 
	 */
	@RequestMapping(value = "{id}/pnfdescriptors/{id_pnf}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePhysicalNetworkFunctionDescriptor(
			@PathVariable("id") String id, @PathVariable("id_pnf") String id_pnf)  {

		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		PhysicalNetworkFunctionDescriptor pDescriptor = findPNFD(nsd.getPnfd(),
				id_pnf);
		nsd.getVnfd().remove(pDescriptor);
	}

	/**
	 * Stores the PhysicalNetworkFunctionDescriptor
	 * 
	 * @param pDescriptor
	 *            : The PhysicalNetworkFunctionDescriptor to be stored
	 * @param id
	 *            : The NSD id
	 * @return PhysicalNetworkFunctionDescriptor: The
	 *         PhysicalNetworkFunctionDescriptor stored
	 * @ 
	 */
	@RequestMapping(value = "{id}/pnfdescriptors/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PhysicalNetworkFunctionDescriptor postPhysicalNetworkFunctionDescriptor(
			@RequestBody @Valid PhysicalNetworkFunctionDescriptor pDescriptor,
			@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		nsd.getPnfd().add(pDescriptor);
		networkServiceDescriptorManagement.update(nsd, id);
		return pDescriptor;
	}

	/**
	 * Edits the PhysicalNetworkFunctionDescriptor
	 * 
	 * @param pDescriptor
	 *            : The PhysicalNetworkFunctionDescriptor to be edited
	 * @param id
	 *            : The NSD id
	 * @return PhysicalNetworkFunctionDescriptor: The
	 *         PhysicalNetworkFunctionDescriptor edited
	 * @ 
	 */
	@RequestMapping(value = "{id}/pnfdescriptors/{id_pnf}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public PhysicalNetworkFunctionDescriptor updatePNFD(
			@RequestBody @Valid PhysicalNetworkFunctionDescriptor pDescriptor,
			@PathVariable("id") String id, @PathVariable("id_pnf") String id_pnf)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		PhysicalNetworkFunctionDescriptor pnfDescriptor = findPNFD(
				nsd.getPnfd(), id_pnf);
		pnfDescriptor = pDescriptor;
		nsd.getPnfd().add(pnfDescriptor);
		networkServiceDescriptorManagement.update(nsd, id);
		return pnfDescriptor;
	}

	/**
	 * Returns the Security into a NSD with id
	 * 
	 * @param id
	 *            : The id of NSD
	 * @return Security: The Security of PhysicalNetworkFunctionDescriptor into
	 *         NSD
	 * @ 
	 */

	@RequestMapping(value = "{id}/security", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Security getSecurity(@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		return nsd.getNsd_security();
	}

	/**
	 * Returns the Security with the id_s
	 * 
	 * @param id
	 *            : The NSD id
	 * @param id_s
	 *            : The Security id
	 * @return Security: The Security selected by id_s
	 * @ 
	 */
	@RequestMapping(value = "{id}/security/{id_s}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Security getSecurity(@PathVariable("id") String id,
			@PathVariable("id_s") String id_s)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {

			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		if (!nsd.getNsd_security().getId().equals(id_s)) {
			log.error("Security with id: " + id_s + " not found.");
			throw new NSDNotFoundException(id_s);
		}
		return nsd.getNsd_security();
	}

	/**
	 * Deletes the Security with the id_s
	 * 
	 * @param id
	 *            : The NSD id
	 * @param id_s
	 *            : The Security id
	 * @ 
	 */
	@RequestMapping(value = "{id}/security/{id_s}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSecurity(@PathVariable("id") String id,
			@PathVariable("id_s") String id_s)  {

		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		if (!nsd.getNsd_security().getId().equals(id_s)) {
			log.error("Security with id: " + id_s + " not found.");
			throw new NSDNotFoundException(id_s);
		}
		nsd.setNsd_security(null);
		networkServiceDescriptorManagement.update(nsd, id);
	}

	/**
	 * Stores the Security into NSD
	 * 
	 * @param security
	 *            : The Security to be stored
	 * @param id
	 *            : The id of NSD
	 * @return Security: The Security stored
	 * @ 
	 */
	@RequestMapping(value = "{id}/security/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public Security postSecurity(@RequestBody @Valid Security security,
			@PathVariable("id") String id)  {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}
		nsd.setNsd_security(security);
		networkServiceDescriptorManagement.update(nsd, id);
		return security;
	}

	@RequestMapping(value = "{id}/vnfdescriptors/{id_pnf}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Security updateSecurity(@RequestBody @Valid Security security,
			@PathVariable("id") String id, @PathVariable("id_s") String id_s) {
		NetworkServiceDescriptor nsd = null;
		try {
			nsd = networkServiceDescriptorManagement.query(id);
		} catch (NoResultException e) {
			log.error(e.getMessage(), e);
			throw new NSDNotFoundException(id);
		}

		nsd.setNsd_security(security);
		networkServiceDescriptorManagement.update(nsd, id);
		return security;
	}

	@RequestMapping(value = "/records", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public NetworkServiceRecord createRecord(
			@RequestBody @Valid NetworkServiceDescriptor networkServiceDescriptor) throws BadFormatException, InterruptedException, ExecutionException, VimException, NotFoundException, VimDriverException, QuotaExceededException {
			return networkServiceRecordManagement.onboard(networkServiceDescriptor);
	}

	private PhysicalNetworkFunctionDescriptor findPNFD(
			Collection<PhysicalNetworkFunctionDescriptor> listPNFD, String id_pnf) {
		PhysicalNetworkFunctionDescriptor pNetworkFunctionDescriptor = null;
		for (PhysicalNetworkFunctionDescriptor pDescriptor : listPNFD) {
			if (pDescriptor.getId().equals(id_pnf)) {
				pNetworkFunctionDescriptor = pDescriptor;
			}
		}
		if (pNetworkFunctionDescriptor == null) {
			throw new PNFDNotFoundException(id_pnf);
		}
		return pNetworkFunctionDescriptor;
	}

	private VNFDependency findVNFD(Collection<VNFDependency> vnf_dependency,
			String id_vnfd) {
		VNFDependency vDependency = null;
		for (VNFDependency vnfDependency : vnf_dependency) {
			if (vnfDependency.getId().equals(id_vnfd)) {
				vDependency = vnfDependency;
			}
		}
		if (vDependency == null) {

			throw new VNFDependencyNotFoundException(id_vnfd);
		}
		return vDependency;
	}

	private VirtualNetworkFunctionDescriptor findVNF(Collection<VirtualNetworkFunctionDescriptor> listVNF, String id_vfn) {

		VirtualNetworkFunctionDescriptor nDescriptor = null;
		for (VirtualNetworkFunctionDescriptor vnfDescriptor : listVNF) {
			if (vnfDescriptor.getId().equals(id_vfn)) {
				nDescriptor = vnfDescriptor;
			}
		}
		if (nDescriptor == null) {
			throw new VNFDNotFoundException(id_vfn);
		}
		return nDescriptor;
	}

}
