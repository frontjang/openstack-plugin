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

package org.project.openbaton.nfvo.core.interfaces;

import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.project.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.project.openbaton.nfvo.exceptions.BadFormatException;
import org.project.openbaton.nfvo.exceptions.NotFoundException;
import org.project.openbaton.nfvo.exceptions.QuotaExceededException;
import org.project.openbaton.nfvo.exceptions.VimException;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by mpa on 30/04/15.
 */

public interface NetworkServiceRecordManagement {

	/**
	 * This operation allows submitting and
	 * validating a Network Service	Descriptor (NSD),
	 * including any related VNFFGD and VLD.
	 */
	NetworkServiceRecord onboard(String nsd_id) throws InterruptedException, ExecutionException, VimException, NotFoundException, BadFormatException, VimDriverException, QuotaExceededException;

	/**
	 * This operation allows submitting and
	 * validating a Network Service	Descriptor (NSD),
	 * including any related VNFFGD and VLD.
	 */
	NetworkServiceRecord onboard(NetworkServiceDescriptor networkServiceDescriptor) throws ExecutionException, InterruptedException, VimException, NotFoundException, NotFoundException, BadFormatException, VimDriverException, QuotaExceededException;

	/**
	 * This operation allows updating a Network 
	 * Service Descriptor (NSD), including any 
	 * related VNFFGD and VLD.This update might 
	 * include creating/deleting new VNFFGDs
	 * and/or new VLDs.
	 * @param new_nsd
	 * @param old_id
	 */
	NetworkServiceRecord update(NetworkServiceRecord new_nsd, String old_id);

	/**
	 * This operation is used to query the
	 * information of the Network Service
	 * Descriptor (NSD), including any
	 * related VNFFGD and VLD.
	 */
	List<NetworkServiceRecord> query();

	NetworkServiceRecord query(String id);

	/**
	 * This operation is used to remove a
	 * disabled Network Service Descriptor.
	 * @param id
	 */
	void delete(String id) throws VimException, NotFoundException, ExecutionException, InterruptedException;

}
