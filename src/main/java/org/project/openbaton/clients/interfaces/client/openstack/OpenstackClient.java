package org.project.openbaton.clients.interfaces.client.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.jclouds.io.payloads.InputStreamPayload;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.glance.v1_0.GlanceApi;
import org.jclouds.openstack.glance.v1_0.domain.ContainerFormat;
import org.jclouds.openstack.glance.v1_0.domain.DiskFormat;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;
import org.jclouds.openstack.glance.v1_0.features.ImageApi;
import org.jclouds.openstack.glance.v1_0.options.CreateImageOptions;
import org.jclouds.openstack.glance.v1_0.options.UpdateImageOptions;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network.CreateNetwork;
import org.jclouds.openstack.neutron.v2.domain.Network.UpdateNetwork;
import org.jclouds.openstack.neutron.v2.domain.Subnet.CreateSubnet;
import org.jclouds.openstack.neutron.v2.domain.Subnet.UpdateSubnet;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.RebootType;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.QuotaApi;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.project.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.project.openbaton.catalogue.nfvo.*;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.plugin.vimdrivers.SpringClientInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import org.jclouds.scriptbuilder.ScriptBuilder;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.domain.OsFamily;

import java.io.*;
import java.util.*;

/**
 * Created by mpa on 06.05.15.
 */
@Service
@Scope("prototype")
public class OpenstackClient implements ClientInterfaces {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private VimInstance vimInstance;

    private NovaApi novaApi;
    private NeutronApi neutronApi;
    private GlanceApi glanceApi;

    private Set<String> zones;
    private String defaultZone = null;

    public void setNovaApi(NovaApi novaApi) {
        this.novaApi = novaApi;
    }
    public void setVimInstance(VimInstance vimInstance) { this.vimInstance = vimInstance; }
    public void setNeutronApi(NeutronApi neutronApi) {
        this.neutronApi = neutronApi;
    }
    public void setGlanceApi(GlanceApi glanceApi) {
        this.glanceApi = glanceApi;
    }

    public OpenstackClient() {
        //TODO get properties from configurations
        vimInstance = null;
        neutronApi = null;
        zones = null;
        novaApi = null;
        glanceApi = null;
    }

    private void init(VimInstance vimInstance) {
 Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
        Properties overrides = new Properties();
        overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
        overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
        overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        this.vimInstance = vimInstance;
        this.novaApi = ContextBuilder.newBuilder("openstack-nova").endpoint(vimInstance.getAuthUrl()).credentials(vimInstance.getTenant() + ":" + vimInstance.getUsername(), vimInstance.getPassword()).modules(modules).overrides(overrides).buildApi(NovaApi.class);
        this.neutronApi = ContextBuilder.newBuilder("openstack-neutron").endpoint(vimInstance.getAuthUrl()).credentials(vimInstance.getTenant() + ":" + vimInstance.getUsername(), vimInstance.getPassword()).modules(modules).overrides(overrides).buildApi(NeutronApi.class);
        this.glanceApi = ContextBuilder.newBuilder("openstack-glance").endpoint(vimInstance.getAuthUrl()).credentials(vimInstance.getTenant() + ":" + vimInstance.getUsername(), vimInstance.getPassword()).modules(modules).overrides(overrides).buildApi(GlanceApi.class);
        this.zones = novaApi.getConfiguredRegions();
        if (null == defaultZone) {
            this.defaultZone = zones.iterator().next();
        }
    }

    public void setZone(String zone) {
        if (null != zone && "" == zone) {
            defaultZone = zone;
        }
    }

    private void createKeyPair(VimInstance vimInstance, String name, String path) throws IOException {
        KeyPairApi keypairApi = this.novaApi.getKeyPairApi(
                this.defaultZone).get();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            line = sb.toString();
            keypairApi.createWithPublicKey(name, line);
        } catch (IOException e) {
            System.out.println("ERROR::Given file path is not valid.");
        } finally {
            br.close();
        }
    }

    @Override
    public Server launchInstance(VimInstance vimInstance, String name, String imageId, String flavorId,
                                  String keypair, Set<String> network, Set<String> secGroup,
                                  String userData) {
        String script=new ScriptBuilder().addStatement(exec(userData)).render(OsFamily.UNIX);
        init(vimInstance);
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        CreateServerOptions options = CreateServerOptions.Builder.keyPairName(keypair).networks(network).securityGroupNames(secGroup).userData(script.getBytes());
        String extId  = serverApi.create(name, imageId, flavorId, options).getId();
        Server server = getServerById(vimInstance, extId);
        return server;
    }

    @Override
    public Server launchInstanceAndWait(VimInstance vimInstance, String name, String imageId, String flavorId,
                                  String keypair, Set<String> network, Set<String> secGroup,
                                  String userData) throws VimDriverException {
        boolean bootCompleted = false;
        Server server = launchInstance(vimInstance, name, imageId, flavorId, keypair, network, secGroup, userData);
        while (bootCompleted==false) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            server = getServerById(vimInstance, server.getExtId());
            if (server.getStatus().equals("ACTIVE")) {
                bootCompleted = true;
            }
            if (server.getStatus().equals("ERROR")){
                throw new VimDriverException(server.getExtendedStatus());
            }
        }
        return server;
    }

    public void rebootServer(String extId, RebootType type) {
        init(vimInstance);
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        serverApi.reboot(extId, type);
    }

    public void deleteServerById(VimInstance vimInstance, String extId) {
        init(vimInstance);
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        serverApi.delete(extId);
    }

    @Override
    public void deleteServerByIdAndWait(VimInstance vimInstance, String extId) {
        init(vimInstance);
        boolean deleteCompleted = false;
        deleteServerById(vimInstance, extId);
        while (deleteCompleted==false) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                getServerById(vimInstance, extId);
            } catch (NullPointerException e) {
                deleteCompleted = true;
            }
        }
    }

    @Override
    public List<NFVImage> listImages(VimInstance vimInstance) {
        init(vimInstance);
        ImageApi imageApi = this.glanceApi.getImageApi(defaultZone);
        List<NFVImage> images = new ArrayList<NFVImage>();
        for (IterableWithMarker<ImageDetails> jcloudsImage : imageApi.listInDetail().toList()){
            for(int i = 0; i < jcloudsImage.size() ; i++){
                NFVImage image = new NFVImage();
                image.setName(jcloudsImage.get(i).getName());
                image.setExtId(jcloudsImage.get(i).getId());
                image.setMinRam(jcloudsImage.get(i).getMinRam());
                image.setMinDiskSpace(jcloudsImage.get(i).getMinDisk());
                image.setCreated(jcloudsImage.get(i).getCreatedAt());
                image.setUpdated(jcloudsImage.get(i).getUpdatedAt());
                image.setIsPublic(jcloudsImage.get(i).isPublic());
                image.setDiskFormat(jcloudsImage.get(i).getDiskFormat().toString().toUpperCase());
                image.setContainerFormat(jcloudsImage.get(i).getContainerFormat().toString().toUpperCase());
                images.add(image);
            }
        }
        return images;
    }

    @Override
    public List<Server> listServer(VimInstance vimInstance){
        init(vimInstance);
        List<Server> servers = new ArrayList<Server>();
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        for (org.jclouds.openstack.nova.v2_0.domain.Server jcloudsServer : serverApi.listInDetail().concat()){
            Server server = new Server();
            server.setExtId(jcloudsServer.getId());
            server.setName(jcloudsServer.getName());
            server.setStatus(jcloudsServer.getStatus().value());
            server.setExtendedStatus(jcloudsServer.getExtendedStatus().toString());
            HashMap<String, List<String>> ipMap = new HashMap<String, List<String>>();
            for (String key : jcloudsServer.getAddresses().keys()) {
                List<String> ips = new ArrayList<String>();
                for (Address address : jcloudsServer.getAddresses().get(key)) {
                    ips.add(address.getAddr());
                }
                ipMap.put(key, ips);
            }
            server.setIps(ipMap);
            server.setCreated(jcloudsServer.getCreated());
            server.setUpdated(jcloudsServer.getUpdated());
            server.setImage(getImageById(jcloudsServer.getImage().getId()));
            server.setFlavor(getFlavorById(jcloudsServer.getFlavor().getId()));
            servers.add(server);
        }
        return servers;
    }

    private Server getServerById(VimInstance vimInstance, String extId) {
        init(vimInstance);
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        try {
            org.jclouds.openstack.nova.v2_0.domain.Server jcloudsServer = serverApi.get(extId);
            log.trace("" + jcloudsServer);
            Server server = new Server();
            server.setExtId(jcloudsServer.getId());
            server.setName(jcloudsServer.getName());
            server.setStatus(jcloudsServer.getStatus().value());
            server.setExtendedStatus(jcloudsServer.getExtendedStatus().toString());
            HashMap<String, List<String>> ipMap = new HashMap<String, List<String>>();
            for (String key : jcloudsServer.getAddresses().keys()) {
                List<String> ips = new ArrayList<String>();
                for (Address address : jcloudsServer.getAddresses().get(key)) {
                    ips.add(address.getAddr());
                }
                ipMap.put(key, ips);
            }
            server.setIps(ipMap);
            server.setCreated(jcloudsServer.getCreated());
            server.setUpdated(jcloudsServer.getUpdated());
            server.setImage(getImageById(jcloudsServer.getImage().getId()));
            server.setFlavor(getFlavorById(jcloudsServer.getFlavor().getId()));
            return server;
        } catch (NullPointerException e) {
            throw new NullPointerException("Server with extId: " + extId + " not found.");
        }
    }

    private String getServerIdByName(String name) {
        init(vimInstance);
        ServerApi serverApi = this.novaApi.getServerApi(defaultZone);
        for (Resource s : serverApi.list().concat()) {
            if (s.getName().equalsIgnoreCase(name))
                return s.getId();
        }
        throw new NullPointerException("Server with name: " + name + " not found.");
    }

    @Override
    public NFVImage addImage(VimInstance vimInstance, NFVImage image, InputStream payload) {
        init(vimInstance);
        NFVImage addedImage = addImage(image.getName(), payload, image.getDiskFormat(), image.getContainerFormat(), image.getMinDiskSpace(), image.getMinRam(), image.isPublic());
        image.setName(addedImage.getName());
        image.setExtId(addedImage.getExtId());
        image.setCreated(addedImage.getCreated());
        image.setUpdated(addedImage.getUpdated());
        image.setMinDiskSpace(addedImage.getMinDiskSpace());
        image.setMinRam(addedImage.getMinRam());
        image.setIsPublic(addedImage.isPublic());
        image.setDiskFormat(addedImage.getDiskFormat());
        image.setContainerFormat(addedImage.getContainerFormat());
        return image;
    }

    private NFVImage addImage(String name, InputStream payload, String diskFormat, String containerFromat, long minDisk, long minRam, boolean isPublic) {
        ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        CreateImageOptions createImageOptions = new CreateImageOptions();
        createImageOptions.minDisk(minDisk);
        createImageOptions.minRam(minRam);
        createImageOptions.isPublic(isPublic);
        createImageOptions.diskFormat(DiskFormat.valueOf(diskFormat));
        createImageOptions.containerFormat(ContainerFormat.valueOf(containerFromat));

        Payload jcloudsPayload = new InputStreamPayload(payload);
        try {
            ByteArrayOutputStream bufferedPayload = new ByteArrayOutputStream();
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = payload.read(bytes)) != -1) {
                bufferedPayload.write(bytes, 0, read);
            }
            bufferedPayload.flush();
            jcloudsPayload = new ByteArrayPayload(bufferedPayload.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageDetails imageDetails = imageApi.create(name, jcloudsPayload, new CreateImageOptions[]{createImageOptions});
        NFVImage image = new NFVImage();
        image.setName(imageDetails.getName());
        image.setExtId(imageDetails.getId());
        image.setCreated(imageDetails.getCreatedAt());
        image.setUpdated(imageDetails.getUpdatedAt());
        image.setMinDiskSpace(imageDetails.getMinDisk());
        image.setMinRam(imageDetails.getMinRam());
        image.setIsPublic(imageDetails.isPublic());
        image.setDiskFormat(imageDetails.getDiskFormat().toString().toUpperCase());
        image.setContainerFormat(imageDetails.getContainerFormat().toString().toUpperCase());
        return image;
    }

    @Override
    public boolean deleteImage(VimInstance vimInstance, NFVImage image) {
        init(vimInstance);
        ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        boolean isDeleted = imageApi.delete(image.getExtId());
        return isDeleted;
    }

    @Override
    public NFVImage updateImage(VimInstance vimInstance, NFVImage image){
        init(vimInstance);
        NFVImage updatedImage = updateImage(image.getExtId(), image.getName(), image.getDiskFormat(), image.getContainerFormat(), image.getMinDiskSpace(), image.getMinRam(), image.isPublic());
        image.setName(updatedImage.getName());
        image.setExtId(updatedImage.getExtId());
        image.setCreated(updatedImage.getCreated());
        image.setUpdated(updatedImage.getUpdated());
        image.setMinDiskSpace(updatedImage.getMinDiskSpace());
        image.setMinRam(updatedImage.getMinRam());
        image.setIsPublic(updatedImage.isPublic());
        image.setDiskFormat(updatedImage.getDiskFormat());
        image.setContainerFormat(updatedImage.getContainerFormat());
        return image;
    }

    private NFVImage updateImage(String extId, String name, String diskFormat, String containerFormat, long minDisk, long minRam, boolean isPublic){
        ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        UpdateImageOptions updateImageOptions = new UpdateImageOptions();
        updateImageOptions.name(name);
        updateImageOptions.minRam(minRam);
        updateImageOptions.minDisk(minDisk);
        updateImageOptions.isPublic(isPublic);
        updateImageOptions.diskFormat(DiskFormat.valueOf(diskFormat));
        updateImageOptions.containerFormat(ContainerFormat.valueOf(containerFormat));
        ImageDetails imageDetails = imageApi.update(extId, updateImageOptions);
        NFVImage image = new NFVImage();
        image.setName(imageDetails.getName());
        image.setExtId(imageDetails.getId());
        image.setCreated(imageDetails.getCreatedAt());
        image.setUpdated(imageDetails.getUpdatedAt());
        image.setMinDiskSpace(imageDetails.getMinDisk());
        image.setMinRam(imageDetails.getMinRam());
        image.setIsPublic(imageDetails.isPublic());
        image.setDiskFormat(imageDetails.getDiskFormat().toString().toUpperCase());
        image.setContainerFormat(imageDetails.getContainerFormat().toString().toUpperCase());
        return image;
    }

    @Override
    public NFVImage copyImage(VimInstance vimInstance, NFVImage image, InputStream inputStream) {
        init(vimInstance);
        NFVImage copiedImage = copyImage(image.getName(), inputStream, image.getDiskFormat(), image.getContainerFormat(), image.getMinDiskSpace(), image.getMinRam(), image.isPublic());
        image.setName(copiedImage.getName());
        image.setExtId(copiedImage.getExtId());
        image.setCreated(copiedImage.getCreated());
        image.setUpdated(copiedImage.getUpdated());
        image.setMinDiskSpace(copiedImage.getMinDiskSpace());
        image.setMinRam(copiedImage.getMinRam());
        image.setIsPublic(copiedImage.isPublic());
        image.setDiskFormat(copiedImage.getDiskFormat());
        image.setContainerFormat(copiedImage.getContainerFormat());
        return image;
    }

    private NFVImage copyImage(String name, InputStream inputStream, String diskFormat, String containerFormat, long minDisk, long minRam, boolean isPublic) {
        ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        NFVImage image = addImage(name, inputStream, diskFormat, containerFormat, minDisk, minRam, isPublic);
        return image;
    }

    private NFVImage getImageById(String extId) {
        //ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        org.jclouds.openstack.nova.v2_0.features.ImageApi imageApi = this.novaApi.getImageApi(this.defaultZone);
        try {
            //ImageDetails jcloudsImage = imageApi.get(extId);
            org.jclouds.openstack.nova.v2_0.domain.Image jcloudsImage = imageApi.get(extId);
            NFVImage image = new NFVImage();
            image.setExtId(jcloudsImage.getId());
            image.setName(jcloudsImage.getName());
            image.setCreated(jcloudsImage.getCreated());
            image.setUpdated(jcloudsImage.getUpdated());
            image.setMinDiskSpace(jcloudsImage.getMinDisk());
            image.setMinRam(jcloudsImage.getMinRam());
            image.setIsPublic(false);
            image.setContainerFormat("not provided");
            image.setDiskFormat("not provided");
//            image.setIsPublic(jcloudsImage.isPublic());
//            image.setDiskFormat(jcloudsImage.getDiskFormat().toString().toUpperCase());
//            image.setContainerFormat(jcloudsImage.getContainerFormat().toString().toUpperCase());
            return image;
        } catch (NullPointerException e) {
            throw new NullPointerException("Image with extId: " + extId + " not found.");
        }
    }

    private String getImageIdByName(String name) {
        ImageApi imageApi = this.glanceApi.getImageApi(this.defaultZone);
        for (Resource i : imageApi.list().concat()) {
            if (i.getName().equalsIgnoreCase(name))
                return i.getId();
        }
        throw new NullPointerException("Image with name: " + name + " not found");
    }

    @Override
    public DeploymentFlavour addFlavor(VimInstance vimInstance, DeploymentFlavour flavor) {
        init(vimInstance);
        DeploymentFlavour addedFlavor = addFlavor(flavor.getFlavour_key(), flavor.getVcpus(), flavor.getRam(), flavor.getDisk());
        flavor.setExtId(addedFlavor.getExtId());
        flavor.setFlavour_key(addedFlavor.getFlavour_key());
        flavor.setVcpus(addedFlavor.getVcpus());
        flavor.setRam(addedFlavor.getRam());
        flavor.setDisk(addedFlavor.getVcpus());
        return flavor;
    }

    private DeploymentFlavour addFlavor(String name, int vcpus, int ram, int disk) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        UUID id = java.util.UUID.randomUUID();
        org.jclouds.openstack.nova.v2_0.domain.Flavor newFlavor = org.jclouds.openstack.nova.v2_0.domain.Flavor.builder().id(id.toString()).name(name).disk(disk).ram(ram).vcpus(vcpus).build();
        org.jclouds.openstack.nova.v2_0.domain.Flavor jcloudsFlavor = flavorApi.create(newFlavor);
        DeploymentFlavour flavor = new DeploymentFlavour();
        flavor.setExtId(jcloudsFlavor.getId());
        flavor.setFlavour_key(jcloudsFlavor.getName());
        flavor.setVcpus(jcloudsFlavor.getVcpus());
        flavor.setRam(jcloudsFlavor.getRam());
        flavor.setDisk(jcloudsFlavor.getVcpus());
        return flavor;
    }

    @Override
    public DeploymentFlavour updateFlavor(VimInstance vimInstance, DeploymentFlavour flavor) throws VimDriverException {
        init(vimInstance);
        try {
            DeploymentFlavour updatedFlavor = updateFlavor(vimInstance, flavor.getExtId(), flavor.getFlavour_key(), flavor.getVcpus(), flavor.getRam(), flavor.getDisk());
            flavor.setFlavour_key(updatedFlavor.getFlavour_key());
            flavor.setExtId(updatedFlavor.getExtId());
            flavor.setRam(updatedFlavor.getRam());
            flavor.setDisk(updatedFlavor.getDisk());
            flavor.setVcpus(updatedFlavor.getVcpus());
            return flavor;
        } catch (VimDriverException e) {
            throw new VimDriverException("Image with id: " + flavor.getId() + " not updated successfully");
            }
    }

    private DeploymentFlavour updateFlavor(VimInstance vimInstance, String extId, String name, int vcpus, int ram, int disk) throws VimDriverException {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        boolean isDeleted = deleteFlavor(vimInstance, extId);
        if (isDeleted) {
            org.jclouds.openstack.nova.v2_0.domain.Flavor newFlavor = org.jclouds.openstack.nova.v2_0.domain.Flavor.builder().id(extId).name(name).disk(disk).ram(ram).vcpus(vcpus).build();
            org.jclouds.openstack.nova.v2_0.domain.Flavor jcloudsFlavor = flavorApi.create(newFlavor);
            DeploymentFlavour updatedFlavor = new DeploymentFlavour();
            updatedFlavor.setExtId(jcloudsFlavor.getId());
            updatedFlavor.setFlavour_key(jcloudsFlavor.getName());
            updatedFlavor.setVcpus(jcloudsFlavor.getVcpus());
            updatedFlavor.setRam(jcloudsFlavor.getRam());
            updatedFlavor.setDisk(jcloudsFlavor.getVcpus());
            return updatedFlavor;
        } else {
            throw new VimDriverException("Image with extId: " + extId + " not updated successfully");
        }
    }

    @Override
    public boolean deleteFlavor(VimInstance vimInstance, String extId) {
        init(vimInstance);
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        flavorApi.delete(extId);
        boolean isDeleted;
        try {
            getFlavorById(extId);
            isDeleted = true;
        } catch (NullPointerException e) {
            isDeleted = false;
        }
        return isDeleted;
    }


    private DeploymentFlavour getFlavorById(String extId) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        try {
            org.jclouds.openstack.nova.v2_0.domain.Flavor jcloudsFlavor = flavorApi.get(extId);
            DeploymentFlavour flavor = new DeploymentFlavour();
            flavor.setFlavour_key(jcloudsFlavor.getName());
            flavor.setExtId(jcloudsFlavor.getId());
            flavor.setRam(jcloudsFlavor.getRam());
            flavor.setDisk(jcloudsFlavor.getDisk());
            flavor.setVcpus(jcloudsFlavor.getVcpus());
            return flavor;
        } catch (NullPointerException e) {
            throw new NullPointerException("Flavor with extId: " + extId + " not found.");
        }
    }

    private String getFlavorIdByName(String name) {
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        for (Resource f : flavorApi.list().concat()) {
            if (f.getName().equalsIgnoreCase(name))
                return f.getId();
        }
        throw new NullPointerException("Flavor with name: " + name + " not found.");
    }

    @Override
    public List<DeploymentFlavour> listFlavors(VimInstance vimInstance) {
        init(vimInstance);
        List<DeploymentFlavour> flavors = new ArrayList<DeploymentFlavour>();
        FlavorApi flavorApi = this.novaApi.getFlavorApi(this.defaultZone);
        for (org.jclouds.openstack.nova.v2_0.domain.Flavor jcloudsFlavor : flavorApi.listInDetail().concat()) {
            DeploymentFlavour flavor = new DeploymentFlavour();
            flavor.setExtId(jcloudsFlavor.getId());
            flavor.setFlavour_key(jcloudsFlavor.getName());
            flavor.setRam(jcloudsFlavor.getRam());
            flavor.setDisk(jcloudsFlavor.getDisk());
            flavor.setVcpus(jcloudsFlavor.getVcpus());
            flavors.add(flavor);
        }
        return flavors;
    }

    private String getSecurityGroupById(String extId) {
        SecurityGroupApi securityGroupApi = novaApi.getSecurityGroupApi(defaultZone).get();
        try {
            SecurityGroup securityGroup = securityGroupApi.get(extId);
            return securityGroup.getId();
        } catch (Exception e) {
            throw new NullPointerException("Security Group with extId: " + extId + " not found.");
        }
    }

    private String getSecurityGroupIdByName(String name) {
        SecurityGroupApi securityGroupApi = novaApi.getSecurityGroupApi(defaultZone).get();
        Iterator<? extends SecurityGroup> sgList = securityGroupApi.list().iterator();
        while (sgList.hasNext()) {
            SecurityGroup group = sgList.next();
            if (group.getName().equalsIgnoreCase(name))
                return group.getId();
        }
        throw new NullPointerException("Security Group with name: " + name + " not found.");
    }

    @Override
    public Network createNetwork(VimInstance vimInstance, Network network) {
        init(vimInstance);
        Network createdNetwork = createNetwork(network.getName(), network.isExternal(), network.isShared());
        network.setName(createdNetwork.getName());
        network.setExtId(createdNetwork.getExtId());
        network.setExternal(createdNetwork.isExternal());
        network.setShared(createdNetwork.isShared());
        return network;
    }

    private Network createNetwork(String name, boolean external, boolean shared) {
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        //CreateNetwork createNetwork = CreateNetwork.createBuilder(name).networkType(NetworkType.fromValue(networkType)).external(external).shared(shared).segmentationId(segmentationId).physicalNetworkName(physicalNetworkName).build();
        CreateNetwork createNetwork = CreateNetwork.createBuilder(name).external(external).shared(shared).build();
        org.jclouds.openstack.neutron.v2.domain.Network jcloudsNetwork = networkApi.create(createNetwork);
        Network network = new Network();
        network.setName(jcloudsNetwork.getName());
        network.setExtId(jcloudsNetwork.getId());
        network.setExternal(jcloudsNetwork.getExternal());
        network.setShared(jcloudsNetwork.getShared());
        return network;
    }

    @Override
    public Network updateNetwork(VimInstance vimInstance, Network network) {
        init(vimInstance);
        Network updatedNetwork = updateNetwork(network.getExtId(), network.getName(), network.isExternal(), network.isShared());
        network.setName(updatedNetwork.getName());
        network.setExtId(updatedNetwork.getExtId());
        network.setExternal(updatedNetwork.isExternal());
        network.setShared(updatedNetwork.isShared());
        return network;
    }

    private Network updateNetwork(String extId, String name, boolean external, boolean shared) {
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        //Plugin does not support updating provider attributes. -> NetworkType, SegmentationId, physicalNetworkName
        //UpdateNetwork updateNetwork = UpdateNetwork.updateBuilder().name(name).networkType(NetworkType.fromValue(networkType)).external(external).shared(shared).segmentationId(segmentationId).physicalNetworkName(physicalNetworkName).build();
        //UpdateNetwork updateNetwork = UpdateNetwork.updateBuilder().name(name).external(external).shared(shared).build();
        UpdateNetwork updateNetwork = UpdateNetwork.updateBuilder().name(name).build();
        org.jclouds.openstack.neutron.v2.domain.Network jcloudsNetwork = networkApi.update(extId, updateNetwork);
        Network network = new Network();
        network.setName(jcloudsNetwork.getName());
        network.setExtId(jcloudsNetwork.getId());
        network.setExternal(jcloudsNetwork.getExternal());
        network.setShared(jcloudsNetwork.getShared());
        return network;
    }

    private boolean deleteNetwork(Network network) {
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        boolean isDeleted = networkApi.delete(network.getExtId());
        return isDeleted;
    }

    @Override
    public boolean deleteNetwork(VimInstance vimInstance, String extId) {
        init(vimInstance);
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        boolean isDeleted = networkApi.delete(extId);
        return isDeleted;
    }


    @Override
    public Network getNetworkById(VimInstance vimInstance, String extId) {
        init(vimInstance);
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        try {
            org.jclouds.openstack.neutron.v2.domain.Network jcloudsNetwork = networkApi.get(extId);
            Network network = new Network();
            network.setName(jcloudsNetwork.getName());
            network.setExtId(jcloudsNetwork.getId());
            network.setExternal(jcloudsNetwork.getExternal());
            network.setShared(jcloudsNetwork.getShared());
            return network;
        } catch (Exception e) {
            throw new NullPointerException("Network not found");
        }
    }

    private String getNetworkIdByName(String name) {
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        for (org.jclouds.openstack.neutron.v2.domain.Network net : networkApi.list().concat()) {
            if (net.getName().equalsIgnoreCase(name))
                return net.getId();
        }
        throw new NullPointerException("Network not found");
    }

    @Override
    public List<String> getSubnetsExtIds(VimInstance vimInstance, String extId) {
        init(vimInstance);
        NetworkApi networkApi = neutronApi.getNetworkApi(defaultZone);
        List<String> subnets = new ArrayList<String>();
        try {
            org.jclouds.openstack.neutron.v2.domain.Network jcloudsNetwork = networkApi.get(extId);
            subnets = jcloudsNetwork.getSubnets().asList();
            return subnets;
        } catch (Exception e) {
            throw new NullPointerException("Network not found");
        }
    }

    @Override
    public List<Network> listNetworks(VimInstance vimInstance) {
        init(vimInstance);
        List<Network> networks = new ArrayList<Network>();
        for (org.jclouds.openstack.neutron.v2.domain.Network jcloudsNetwork : this.neutronApi.getNetworkApi(defaultZone).list().concat()){
            log.trace("OpenstackNetwork: " + jcloudsNetwork.toString());
            Network network = new Network();
            network.setName(jcloudsNetwork.getName());
            network.setExtId(jcloudsNetwork.getId());
            network.setExternal(jcloudsNetwork.getExternal());
            network.setShared(jcloudsNetwork.getShared());
            networks.add(network);
        }
        return networks;
    }

    @Override
    public Subnet createSubnet(VimInstance vimInstance, Network network, Subnet subnet) {
        init(vimInstance);
        Subnet createdSubnet = createSubnet(network, subnet.getName(), subnet.getCidr());
        subnet.setExtId(createdSubnet.getExtId());
        subnet.setName(createdSubnet.getName());
        subnet.setCidr(createdSubnet.getCidr());
        return subnet;
    }

    private Subnet createSubnet(Network network, String name, String cidr) {
        SubnetApi subnetApi = neutronApi.getSubnetApi(defaultZone);
        CreateSubnet createSubnet = CreateSubnet.createBuilder(network.getExtId(), cidr).name(name).ipVersion(4).build();
        org.jclouds.openstack.neutron.v2.domain.Subnet jcloudsSubnet = subnetApi.create(createSubnet);
        Subnet subnet = new Subnet();
        subnet.setExtId(jcloudsSubnet.getId());
        subnet.setName(jcloudsSubnet.getName());
        subnet.setCidr(jcloudsSubnet.getCidr());
        return subnet;
    }

    @Override
    public Subnet updateSubnet(VimInstance vimInstance, Network network, Subnet subnet) {
        init(vimInstance);
        Subnet updatedSubnet = updateSubnet(network, subnet.getExtId(), subnet.getName());
        subnet.setExtId(updatedSubnet.getExtId());
        subnet.setName(updatedSubnet.getName());
        subnet.setCidr(updatedSubnet.getCidr());
        return subnet;
    }

    private Subnet updateSubnet(Network network, String subnetExtId, String name) {
        SubnetApi subnetApi = neutronApi.getSubnetApi(defaultZone);
        //Cannot update read-only attribute cidr
        //Cannot update read-only attribute network_id
        //Cannot update read-only attribute ip_version
        //UpdateSubnet updateSubnet = UpdateSubnet.updateBuilder().networkId(network.getExtId()).cidr(cidr).name(name).ipVersion(4).build();
        UpdateSubnet updateSubnet = UpdateSubnet.updateBuilder().name(name).build();
        org.jclouds.openstack.neutron.v2.domain.Subnet jcloudsSubnet = subnetApi.update(subnetExtId, updateSubnet);
        Subnet subnet = new Subnet();
        subnet.setExtId(jcloudsSubnet.getId());
        subnet.setName(jcloudsSubnet.getName());
        subnet.setCidr(jcloudsSubnet.getCidr());
        return subnet;
    }

    private boolean deleteSubnet(Subnet subnet) {
        SubnetApi subnetApi = neutronApi.getSubnetApi(defaultZone);
        boolean isDeleted = subnetApi.delete(subnet.getExtId());
        return isDeleted;
    }

    @Override
    public boolean deleteSubnet(VimInstance vimInstance, String extId) {
        init(vimInstance);
        SubnetApi subnetApi = neutronApi.getSubnetApi(defaultZone);
        boolean isDeleted = subnetApi.delete(extId);
        return isDeleted;
    }


    private Subnet getSubnetById(String extId) {
        SubnetApi subnetApi = neutronApi.getSubnetApi(defaultZone);
        try {
            org.jclouds.openstack.neutron.v2.domain.Subnet jcloudsSubnet = subnetApi.get(extId);
            Subnet subnet = new Subnet();
            subnet.setName(jcloudsSubnet.getName());
            subnet.setExtId(jcloudsSubnet.getId());
            subnet.setNetworkId(jcloudsSubnet.getNetworkId());
            subnet.setCidr(jcloudsSubnet.getCidr());
            return subnet;
        } catch (Exception e) {
            throw new NullPointerException("Subnet with extId: " + extId + " not found.");
        }
    }

    private List<Subnet> listSubnets(VimInstance vimInstance) {
        init(vimInstance);
        List<Subnet> subnets = new ArrayList<Subnet>();
        for (org.jclouds.openstack.neutron.v2.domain.Subnet net : this.neutronApi.getSubnetApi(defaultZone).list().concat()){
            Subnet subnet = new Subnet();
            subnet.setName(net.getName());
            subnet.setExtId(net.getId());
            subnet.setNetworkId(net.getNetworkId());
            subnet.setCidr(net.getCidr());
            subnets.add(subnet);
        }
        return subnets;
    }

    private List<String> listAllFloatingIps() {
        List<String> floatingIPs = new LinkedList<String>();
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        Iterator<FloatingIP> floatingIpIterator = floatingIPApi.list().iterator();
        while (floatingIpIterator.hasNext()) {
            FloatingIP floatingIP = floatingIpIterator.next();
            floatingIPs.add(floatingIP.getIp());
        }
        return floatingIPs;
    }

    private List<String> listAssociatedFloatingIps() {
        List<String> floatingIPs = new LinkedList<String>();
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        Iterator<FloatingIP> floatingIpIterator = floatingIPApi.list().iterator();
        while (floatingIpIterator.hasNext()) {
            FloatingIP floatingIP = floatingIpIterator.next();
            if (floatingIP.getInstanceId() != null) {
                floatingIPs.add(floatingIP.getIp());
            }
        }
        return floatingIPs;
    }

    private List<String> listFreeFloatingIps() {
        List<String> floatingIPs = new LinkedList<String>();
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        Iterator<FloatingIP> floatingIpIterator = floatingIPApi.list().iterator();
        while (floatingIpIterator.hasNext()) {
            FloatingIP floatingIP = floatingIpIterator.next();
            if (floatingIP.getInstanceId() == null) {
                floatingIPs.add(floatingIP.getIp());
            }
        }
        return floatingIPs;
    }

    private void associateFloatingIpFromPool(VimInstance vimInstance, Server server, String pool) {
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        FloatingIP floatingIp = floatingIPApi.allocateFromPool(pool);
        associateFloatingIp(vimInstance, server, floatingIp.getIp());
    }

    private void associateFloatingIp(VimInstance vimInstance, Server server, String floatingIp) {
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        floatingIPApi.addToServer(floatingIp, server.getExtId());
    }

    private void disassociateFloatingIp(Server server, String floatingIp) {
        org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(defaultZone).get();
        floatingIPApi.removeFromServer(floatingIp, server.getExtId());
    }

    @Override
    public Quota getQuota(VimInstance vimInstance) {
        init(vimInstance);
        QuotaApi quotaApi = novaApi.getQuotaApi(defaultZone).get();
        org.jclouds.openstack.nova.v2_0.domain.Quota jcloudsQuota = quotaApi.getByTenant("admin");
        Quota quota = new Quota();
        quota.setTenant(jcloudsQuota.getId());
        quota.setCores(jcloudsQuota.getCores());
        quota.setFloatingIps(jcloudsQuota.getFloatingIps());
        quota.setInstances(jcloudsQuota.getInstances());
        quota.setKeyPairs(jcloudsQuota.getKeyPairs());
        quota.setRam(jcloudsQuota.getRam());
        return quota;
    }

    @Override
    public String getType(VimInstance vimInstance) {
        return "openstack";
    }

}
