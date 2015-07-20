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

package org.project.openbaton.nfvo.cli.command;


import com.google.gson.Gson;
import org.project.openbaton.catalogue.mano.descriptor.VNFForwardingGraphDescriptor;
import org.project.openbaton.nfvo.api.RestVNFFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * OpenBaton VNFFG-related commands implementation using the spring-shell library.
 */
@Component
public class VNFFG implements CommandMarker {

	private Logger log = LoggerFactory.getLogger(this.getClass());


	@Autowired
	private RestVNFFG vNFFGRequest;

	private Gson mapper = new Gson();

	/**
	 * Adds a new VNF software VNFFG to the vnfForwardingGraphDescriptor repository
	 *
	 * @param vnfForwardingGraphDescriptor
	 *            : VNFFG to add
	 * @return vnfForwardingGraphDescriptor: The vnfForwardingGraphDescriptor filled with values from the core
	 */
	@CliCommand(value = "vnfForwardingGraphDescriptor create", help = "Adds a new vnfForwardingGraphDescriptor to the vnfForwardingGraphDescriptor repository")
	public String create(@CliOption(key = { "vnfForwardingGraphDescriptorFile" }, mandatory = true, help = "The vnfForwardingGraphDescriptor json file") final File vnfForwardingGraphDescriptor) {
		try {
			return "VNFFG CREATED: " + vNFFGRequest.create(mapper.<VNFForwardingGraphDescriptor>fromJson(new InputStreamReader(new FileInputStream(vnfForwardingGraphDescriptor)), VNFForwardingGraphDescriptor.class));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage());
			return e.getMessage();
		}
	}

	/**
	 * Removes the VNF software VNFFG from the VNFFG repository
	 *
	 * @param id
	 *            : The VNFFG's id to be deleted
	 */
	@CliCommand(value = "vnfForwardingGraphDescriptor delete", help = "Removes the vnfForwardingGraphDescriptor from the vnfForwardingGraphDescriptor repository")
	public String delete(
			@CliOption(key = { "id" }, mandatory = true, help = "The vnfForwardingGraphDescriptor id") final String id) {
		vNFFGRequest.delete(id);
		return "VNFFG DELETED";
	}

	/**
	 * Returns the VNFFG selected by id
	 *
	 * @param id
	 *            : The id of the VNFFG
	 * @return vnfForwardingGraphDescriptor: The VNFFG selected
	 */
	@CliCommand(value = "vnfForwardingGraphDescriptor find", help = "Returns the vnfForwardingGraphDescriptor selected by id, or all if no id is given")
	public String findById(
			@CliOption(key = { "id" }, mandatory = true, help = "The vnfForwardingGraphDescriptor id") final String id) {
		if (id != null) {
			return "FOUND VNFFG: " + vNFFGRequest.findById(id);
		} else {
			return "FOUND VNFFGs: " + vNFFGRequest.findAll();
		}
	}
	/**
	 * Updates the VNF software vnfForwardingGraphDescriptor
	 *
	 * @param vnfForwardingGraphDescriptor
	 *            : the VNF software vnfForwardingGraphDescriptor to be updated
	 * @param id
	 *            : the id of VNF software vnfForwardingGraphDescriptor
	 * @return networkServiceDescriptor: the VNF software vnfForwardingGraphDescriptor updated
	 */
	@CliCommand(value = "vnfForwardingGraphDescriptor update", help = "Updates the vnfForwardingGraphDescriptor")
	public String update(
			@CliOption(key = { "vnfForwardingGraphDescriptorFile" }, mandatory = true, help = "The vnfForwardingGraphDescriptor json file") final File vnfForwardingGraphDescriptor,
			@CliOption(key = { "id" }, mandatory = true, help = "The vnfForwardingGraphDescriptor id") final String id) {
		try {
			return "VNFFG UPDATED: " + vNFFGRequest.update(mapper.<VNFForwardingGraphDescriptor>fromJson(new InputStreamReader(new FileInputStream(vnfForwardingGraphDescriptor)), VNFForwardingGraphDescriptor.class), id);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage());
			return e.getMessage();
		}
	}

}
