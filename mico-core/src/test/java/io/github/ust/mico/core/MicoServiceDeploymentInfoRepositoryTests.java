/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.ust.mico.core;

import io.github.ust.mico.core.model.*;
import io.github.ust.mico.core.persistence.*;
import io.github.ust.mico.core.util.CollectionUtils;
import io.github.ust.mico.core.util.EmbeddedRedisServer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import io.github.ust.mico.core.dto.request.MicoServiceDeploymentInfoRequestDTO;
import io.github.ust.mico.core.model.MicoApplication;
import io.github.ust.mico.core.model.MicoService;
import io.github.ust.mico.core.model.MicoServiceDeploymentInfo;
import io.github.ust.mico.core.persistence.MicoApplicationRepository;
import io.github.ust.mico.core.persistence.MicoServiceDeploymentInfoRepository;
import io.github.ust.mico.core.persistence.MicoServiceInterfaceRepository;
import io.github.ust.mico.core.persistence.MicoServiceRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MicoServiceDeploymentInfoRepositoryTests {
    public static @ClassRule
    RuleChain rules = RuleChain.outerRule(EmbeddedRedisServer.runningAt(6379).suppressExceptions());

    @Autowired
    private MicoApplicationRepository applicationRepository;

    @Autowired
    private MicoServiceRepository serviceRepository;

    @Autowired
    private MicoServiceInterfaceRepository serviceInterfaceRepository;

    @Autowired
    private MicoServiceDeploymentInfoRepository serviceDeploymentInfoRepository;

    @Autowired
    private MicoServiceDependencyRepository serviceDependencyRepository;

    @Autowired
    private MicoServicePortRepository servicePortRepository;

    @Autowired
    private MicoLabelRepository labelRepository;

    @Autowired
    private MicoEnvironmentVariableRepository environmentVariableRepository;

    @Autowired
    private MicoBackgroundTaskRepository backgroundTaskRepository;

    @Before
    public void setUp() {
        applicationRepository.deleteAll();
        serviceRepository.deleteAll();
        serviceInterfaceRepository.deleteAll();
        serviceDeploymentInfoRepository.deleteAll();
        serviceDependencyRepository.deleteAll();
        servicePortRepository.deleteAll();
        labelRepository.deleteAll();
        environmentVariableRepository.deleteAll();
        backgroundTaskRepository.deleteAll();
    }

    @After
    public void cleanUp() {

    }

    private MicoApplication a1;
    private MicoApplication a2;

    private MicoService s1;
    private MicoService s2;
    private MicoService s3;
    private MicoService s4;

    private MicoLabel l1;
    private MicoLabel l2;
    private MicoLabel l3;
    private MicoLabel l4;

    private MicoEnvironmentVariable v1;
    private MicoEnvironmentVariable v2;
    private MicoEnvironmentVariable v3;
    private MicoEnvironmentVariable v4;

    @Commit
    @Test
    public void findServiceDeploymentInfoByApplication() {
        createTestData();

        List<MicoServiceDeploymentInfo> serviceDeploymentInfoList = serviceDeploymentInfoRepository.findAllByApplication("a1", "v1.0.0");
        assertEquals(3, serviceDeploymentInfoList.size());

        int numOfSDIs = 0;

        // Iterate over every serviceDeploymentInfo and check if it contains the correct service, labels and environment variables
        for (int i = 0; i < serviceDeploymentInfoList.size(); i++) {
            MicoServiceDeploymentInfo sdi = serviceDeploymentInfoList.get(i);

            // If the sdi is linked to s1, validate all labels and environment variables of the corresponding service
            if (sdi.getService().getShortName().equals("s1")) {
                assertEquals(1, sdi.getLabels().size());
                assertEquals(2, sdi.getEnvironmentVariables().size());
                assertEquals("key1", sdi.getLabels().get(0).getKey());

                if (sdi.getEnvironmentVariables().get(0).getName().equals("env1")) {
                    assertEquals("val1", sdi.getEnvironmentVariables().get(0).getValue());
                } else {
                    assertEquals("val2", sdi.getEnvironmentVariables().get(0).getValue());
                }
                numOfSDIs++;

            } else if (sdi.getService().getShortName().equals("s2")) {
                assertEquals(1, sdi.getEnvironmentVariables().size());
                assertEquals(0, sdi.getLabels().size());
                assertEquals("env3", sdi.getEnvironmentVariables().get(0).getName());
                numOfSDIs++;

            } else if (sdi.getService().getShortName().equals("s3")) {
                assertEquals(0, sdi.getEnvironmentVariables().size());
                assertEquals(2, sdi.getLabels().size());

                if (sdi.getLabels().get(0).getKey().equals("key2")) {
                    assertEquals("value2", sdi.getLabels().get(0).getValue());
                } else {
                    assertEquals("value3", sdi.getLabels().get(0).getValue());
                }
                numOfSDIs++;

            } else {
                // This case should not occur
                numOfSDIs = 4;
            }
        }

        assertEquals(3, numOfSDIs);
    }

    @Commit
    @Test
    public void findServiceDeploymentInfoByApplicationAndService() {
        createTestData();

        Optional<MicoServiceDeploymentInfo> serviceDeploymentInfoOptional = serviceDeploymentInfoRepository.findByApplicationAndService("a1", "v1.0.0", "s1");
        assertTrue(serviceDeploymentInfoOptional.isPresent());

        assertEquals("v1.0.2", serviceDeploymentInfoOptional.get().getService().getVersion());
        assertEquals("key1", serviceDeploymentInfoOptional.get().getLabels().get(0).getKey());
        assertEquals("val1", serviceDeploymentInfoOptional.get().getEnvironmentVariables().get(0).getValue());

        serviceDeploymentInfoOptional = serviceDeploymentInfoRepository.findByApplicationAndService("a1", "v1.0.0", "s2", "v1.0.3");
        assertTrue(serviceDeploymentInfoOptional.isPresent());

        assertEquals("v1.0.3", serviceDeploymentInfoOptional.get().getService().getVersion());
        assertEquals(0, serviceDeploymentInfoOptional.get().getLabels().size());
        assertEquals("val3", serviceDeploymentInfoOptional.get().getEnvironmentVariables().get(0).getValue());
    }

    @Commit
    @Test
    public void deleteServiceDeploymentInfoByApplication() {
        createTestData();

        serviceDeploymentInfoRepository.deleteAllByApplication("a1");

        // Services should be still deployed and validate other repositories
        assertEquals(3, serviceRepository.findAllByApplication("a1", "v1.0.0").size());
        assertEquals(1, labelRepository.count());
        assertEquals(1, environmentVariableRepository.count());
        assertEquals(1, serviceDeploymentInfoRepository.count());
    }

    @Commit
    @Test
    public void deleteServiceDeploymentInfoByApplicationWithVersion() {
        createTestData();

        serviceDeploymentInfoRepository.deleteAllByApplication("a1", "v1.0.0");

        // Services should be still deployed and validate other repositories
        assertEquals(3, serviceRepository.findAllByApplication("a1", "v1.0.0").size());
        assertEquals(1, labelRepository.count());
        assertEquals(1, environmentVariableRepository.count());
        assertEquals(1, serviceDeploymentInfoRepository.count());
    }

    @Commit
    @Test
    public void deleteServiceDeploymentInfoByApplicationAndService() {
        createTestData();

        serviceDeploymentInfoRepository.deleteByApplicationAndService("a1", "v1.0.0", "s1");

        // Services should be still deployed and validate other repositories
        assertEquals(3, serviceRepository.findAllByApplication("a1", "v1.0.0").size());
        assertEquals(3, labelRepository.count());
        assertEquals(2, environmentVariableRepository.count());
        assertEquals(3, serviceDeploymentInfoRepository.count());
    }

    @Commit
    @Test
    public void deleteServiceDeploymentInfoByApplicationAndServiceWithVersion() {
        createTestData();

        serviceDeploymentInfoRepository.deleteByApplicationAndService("a1", "v1.0.0", "s2", "v1.0.3");

        // Services should be still deployed and validate other repositories
        assertEquals(3, serviceRepository.findAllByApplication("a1", "v1.0.0").size());
        assertEquals(4, labelRepository.count());
        assertEquals(3, environmentVariableRepository.count());
        assertEquals(3, serviceDeploymentInfoRepository.count());
    }

    /**
     * Creates several applications, services, labels, environment variables and deployment information
     */
    private void createTestData() {
        MicoApplication a1 = new MicoApplication().setShortName("a1").setVersion("v1.0.0");
        MicoApplication a2 = new MicoApplication().setShortName("a2").setVersion("v1.0.1");

        MicoService s1 = new MicoService().setShortName("s1").setVersion("v1.0.2");
        MicoService s2 = new MicoService().setShortName("s2").setVersion("v1.0.3");
        MicoService s3 = new MicoService().setShortName("s3").setVersion("v1.0.4");
        MicoService s4 = new MicoService().setShortName("s4").setVersion("v1.0.5");

        MicoLabel l1 = new MicoLabel().setKey("key1").setValue("value1");
        MicoLabel l2 = new MicoLabel().setKey("key2").setValue("value2");
        MicoLabel l3 = new MicoLabel().setKey("key3").setValue("value3");
        MicoLabel l4 = new MicoLabel().setKey("key4").setValue("value4");

        MicoEnvironmentVariable v1 = new MicoEnvironmentVariable().setName("env1").setValue("val1");
        MicoEnvironmentVariable v2 = new MicoEnvironmentVariable().setName("env2").setValue("val2");
        MicoEnvironmentVariable v3 = new MicoEnvironmentVariable().setName("env3").setValue("val3");
        MicoEnvironmentVariable v4 = new MicoEnvironmentVariable().setName("env4").setValue("val4");

        // Add some services and deployment informations to the applications
        a1.getServices().addAll(CollectionUtils.listOf(s1, s2, s3));
        a1.getServiceDeploymentInfos().addAll(CollectionUtils.listOf(
            new MicoServiceDeploymentInfo().setService(s1).setReplicas(3).setLabels(CollectionUtils.listOf(l1)).setEnvironmentVariables(CollectionUtils.listOf(v1, v2)),
            new MicoServiceDeploymentInfo().setService(s2).setReplicas(4).setEnvironmentVariables(CollectionUtils.listOf(v3)),
            new MicoServiceDeploymentInfo().setService(s3).setReplicas(5).setLabels(CollectionUtils.listOf(l2, l3))
        ));
        a2.getServices().add(s4);
        a2.getServiceDeploymentInfos().add(
            new MicoServiceDeploymentInfo().setService(s4).setReplicas(4).setLabels(CollectionUtils.listOf(l4)).setEnvironmentVariables(CollectionUtils.listOf(v4)
            ));

        // Save all created objects in their corresponding repositories
        applicationRepository.save(a1);
        applicationRepository.save(a2);
        serviceRepository.save(s1);
        serviceRepository.save(s2);
        serviceRepository.save(s3);
        serviceRepository.save(s4);
        labelRepository.save(l1);
        labelRepository.save(l2);
        labelRepository.save(l3);
        labelRepository.save(l4);
        environmentVariableRepository.save(v1);
        environmentVariableRepository.save(v2);
        environmentVariableRepository.save(v3);
        environmentVariableRepository.save(v4);
    }

    @Test
    public void findAllByApplication() {
        MicoApplication a1 = new MicoApplication().setShortName("a1").setVersion("v1.0.0");
        MicoService s1 = new MicoService().setShortName("s1").setVersion("v1.0.0");
        MicoService s2 = new MicoService().setShortName("s2").setVersion("v1.0.0");

        a1.getServices().add(s1);
        a1.getServiceDeploymentInfos().add(new MicoServiceDeploymentInfo().setService(s1).setReplicas(5));
        a1.getServiceDeploymentInfos().add(new MicoServiceDeploymentInfo().setService(s2).setReplicas(10));

        applicationRepository.save(a1);

        List<MicoServiceDeploymentInfo> serviceDeploymentInfos = serviceDeploymentInfoRepository.findAllByApplication("a1", "v1.0.0");

        assertEquals(2, serviceDeploymentInfos.size());
        assertNotNull(serviceDeploymentInfos.get(0).getService());
    }

    @Test
    public void findByApplicationAndService() {
        MicoApplication a1 = new MicoApplication().setShortName("a1").setVersion("v1.0.0");
        MicoService s1 = new MicoService().setShortName("s1").setVersion("v1.0.0");

        a1.getServices().add(s1);
        a1.getServiceDeploymentInfos().add(new MicoServiceDeploymentInfo().setService(s1).setReplicas(5));

        applicationRepository.save(a1);

        Optional<MicoServiceDeploymentInfo> serviceDeploymentInfo = serviceDeploymentInfoRepository.findByApplicationAndService(
            "a1", "v1.0.0", "s1", "v1.0.0");

        assertTrue(serviceDeploymentInfo.isPresent());
        System.out.println(serviceDeploymentInfo.get().toString());
        assertEquals(5, serviceDeploymentInfo.get().getReplicas());
        assertNotNull(serviceDeploymentInfo.get().getService());
        assertEquals("s1", serviceDeploymentInfo.get().getService().getShortName());
    }

    @Test
    public void findByApplicationAndServiceAfterUpdate() {
        MicoApplication a1 = new MicoApplication().setShortName("a1").setVersion("v1.0.0");
        MicoService s1 = new MicoService().setShortName("s1").setVersion("v1.0.0");

        a1.getServices().add(s1);
        applicationRepository.save(a1);

        MicoServiceDeploymentInfo sdi = new MicoServiceDeploymentInfo().setService(s1);
        MicoServiceDeploymentInfoRequestDTO serviceDeploymentInfoDTO = new MicoServiceDeploymentInfoRequestDTO().setReplicas(5);
        a1.getServiceDeploymentInfos().add(sdi.applyValuesFrom(serviceDeploymentInfoDTO));
        MicoApplication updatedApplication = applicationRepository.save(a1);

        System.out.println(updatedApplication.toString());
        assertEquals(1, updatedApplication.getServiceDeploymentInfos().size());
        assertEquals(5, updatedApplication.getServiceDeploymentInfos().get(0).getReplicas());
        assertNotNull(updatedApplication.getServiceDeploymentInfos().get(0).getService());

        Optional<MicoServiceDeploymentInfo> serviceDeploymentInfo = serviceDeploymentInfoRepository.findByApplicationAndService(
            "a1", "v1.0.0", "s1", "v1.0.0");

        assertTrue(serviceDeploymentInfo.isPresent());
        System.out.println(serviceDeploymentInfo.get().toString());
        assertEquals(5, serviceDeploymentInfo.get().getReplicas());
        assertNotNull(serviceDeploymentInfo.get().getService());
        assertEquals("s1", serviceDeploymentInfo.get().getService().getShortName());
    }

}
