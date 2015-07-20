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

import org.project.openbaton.catalogue.nfvo.VimInstance;
import org.project.openbaton.nfvo.core.interfaces.VimManagement;
import org.project.openbaton.nfvo.exceptions.VimException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/api/v1/datacenters")
public class RestVimInstances {

//	TODO add log prints
//	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private VimManagement vimManagement;

	/**
	 * Adds a new VNF software Image to the datacenter repository
	 * 
	 * @param vimInstance
	 *            : Image to add
	 * @return datacenter: The datacenter filled with values from the core
	 */
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VimInstance create(@RequestBody @Valid VimInstance vimInstance) throws VimException {
		return vimManagement.add(vimInstance);
	}

	/**
	 * Removes the Datacenter from the Datacenter repository
	 * 
	 * @param id: The Datacenter's id to be deleted
	 */
	@RequestMapping(value = "{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("id") String id) {
		vimManagement.delete(id);
	}

	/**
	 * Returns the list of the Datacenters available
	 * @return List<Datacenter>: The List of Datacenters available
	 */
	@RequestMapping(method = RequestMethod.GET)
	public List<VimInstance> findAll() {
		return vimManagement.query();
	}

	/**
	 * Returns the Datacenter selected by id
	 * @param id: The Datacenter's id selected
	 * @return Datacenter: The Datacenter selected
	 */
	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	public VimInstance findById(@PathVariable("id") String id) {
		VimInstance vimInstance = vimManagement.query(id);

		return vimInstance;
	}

	/**

	 * This operation updates the Network Service Descriptor (NSD)
	 * 
	 * @param new_vimInstance
	 *            : the new datacenter to be updated to
	 * @param id
	 *            : the id of the old datacenter
	 * @return VimInstance: the VimInstance updated

	 */

	@RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public VimInstance update(@RequestBody @Valid VimInstance new_vimInstance,
			@PathVariable("id") String id) throws VimException {
		return vimManagement.update(new_vimInstance, id);
	}
}
