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

import org.project.openbaton.catalogue.nfvo.Network;
import org.project.openbaton.catalogue.nfvo.VimInstance;
import org.project.openbaton.nfvo.exceptions.VimException;

import java.util.List;


/**
 * Created by mpa on 30/04/15.
 */

public interface NetworkManagement {
    
	/**
     * This operation allows adding new VNF software 
     * images to the image repository.
     * @param vimInstance
     * @param network
     */
    Network add(VimInstance vimInstance, Network network) throws VimException;

    /**
	 * This operation allows deleting in the VNF software 
	 * images from the image repository.
     * @param vimInstance
     * @param network
     */
    void delete(VimInstance vimInstance, Network network) throws VimException;
    
    /**
	 * This operation allows updating the VNF software 
	 * images in the image repository.
     * @param vimInstance
     * @param new_network
     */
    Network update(VimInstance vimInstance, Network new_network) throws VimException;
    
    /**
	 * This operation allows querying the information of 
	 * the VNF software images in the image repository.
	 */
    List<Network> query();
    
    /**
     * This operation allows querying the information of 
     * the VNF software image in the image repository.
     * @param id
     */
    Network query(String id);
}
