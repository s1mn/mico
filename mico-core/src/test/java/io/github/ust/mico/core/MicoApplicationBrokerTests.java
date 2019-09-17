package io.github.ust.mico.core;

import io.github.ust.mico.core.broker.MicoApplicationBroker;
import io.github.ust.mico.core.broker.MicoServiceBroker;
import io.github.ust.mico.core.broker.MicoServiceDeploymentInfoBroker;
import io.github.ust.mico.core.exception.*;
import io.github.ust.mico.core.model.MicoApplication;
import io.github.ust.mico.core.model.MicoService;
import io.github.ust.mico.core.model.MicoServiceDeploymentInfo;
import io.github.ust.mico.core.persistence.MicoApplicationRepository;
import io.github.ust.mico.core.persistence.MicoServiceDeploymentInfoRepository;
import io.github.ust.mico.core.persistence.MicoServiceRepository;
import io.github.ust.mico.core.util.CollectionUtils;
import lombok.var;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.github.ust.mico.core.TestConstants.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("unit-testing")
public class MicoApplicationBrokerTests {

    @MockBean
    private MicoApplicationRepository micoApplicationRepository;

    @MockBean
    private MicoServiceBroker micoServiceBroker;

    @MockBean
    private MicoServiceDeploymentInfoBroker micoServiceDeploymentInfoBroker;

    @Autowired
    private MicoServiceRepository micoServiceRepository;

    @Autowired
    private MicoApplicationBroker micoApplicationBroker;


    @Test
    public void getAllApplications() {
        given(micoApplicationRepository.findAll(ArgumentMatchers.anyInt())).willReturn(
            CollectionUtils.listOf(
                new MicoApplication().setName(NAME).setShortName(SHORT_NAME_1).setVersion(VERSION_1_0_1).setDescription(DESCRIPTION_1)));

        List<MicoApplication> listOfMicoApplications = micoApplicationBroker.getMicoApplications();
        assertThat(listOfMicoApplications.get(0).getShortName()).isEqualTo(SHORT_NAME_1);
        /*

         */
    }

    @Test
    public void getApplicationsFromDatabase() throws MicoApplicationNotFoundException {
        MicoApplication micoApplication = new MicoApplication()
            .setShortName(SHORT_NAME_1)
            .setVersion(VERSION_1_0_1)
            .setDescription(DESCRIPTION_1)
            .setName(NAME_1);

        given(micoApplicationRepository.findByShortNameAndVersion(micoApplication.getShortName(), micoApplication.getVersion())).willReturn(Optional.of(micoApplication));

        MicoApplication application = micoApplicationBroker.getMicoApplicationByShortNameAndVersion(SHORT_NAME_1, VERSION_1_0_1);
        assertThat(application.getShortName()).isEqualTo(SHORT_NAME_1);
        assertThat(application.getVersion()).isEqualTo(VERSION_1_0_1);
    }

    @Test
    public void updateApplicationVersion() throws MicoApplicationNotFoundException, MicoApplicationAlreadyExistsException {
        MicoApplication micoApplication = new MicoApplication()
            .setName(NAME_2)
            .setShortName(SHORT_NAME_2)
            .setVersion(VERSION_1_0_2)
            .setDescription(DESCRIPTION_2);

        given(micoApplicationRepository.findByShortNameAndVersion(micoApplication.getShortName(), micoApplication.getVersion())
        ).willReturn(Optional.of(micoApplication));
        given(micoApplicationRepository.save(any(MicoApplication.class))).willReturn(micoApplication);

        MicoApplication updatedMicoApplication = micoApplicationBroker.copyAndUpgradeMicoApplicationByShortNameAndVersion(
            micoApplication.getShortName(), micoApplication.getVersion(), VERSION_1_0_3);

        assertThat(updatedMicoApplication.getShortName()).isEqualTo(SHORT_NAME_2);
        assertThat(updatedMicoApplication.getVersion()).isEqualTo(VERSION_1_0_3);
        assertThat(updatedMicoApplication.getName()).isEqualTo(NAME_2);
        assertThat(updatedMicoApplication.getDescription()).isEqualTo(DESCRIPTION_2);
    }

    @Test
    public void deleteApplication() throws MicoApplicationIsNotUndeployedException, MicoApplicationNotFoundException {
        MicoApplication micoApplication = new MicoApplication()
            .setShortName(SHORT_NAME_1)
            .setVersion(VERSION_1_0_1)
            .setName(NAME_1)
            .setDescription(DESCRIPTION_1);

        given(micoApplicationRepository.findByShortNameAndVersion(micoApplication.getShortName(), micoApplication.getVersion()))
            .willReturn(Optional.of(micoApplication));

        micoApplicationBroker.deleteMicoApplicationByShortNameAndVersion(micoApplication.getShortName(), micoApplication.getVersion());
    }

    @Test
    public void addKFConnectorToMicoApplication() {
        //TODO: Wait for final implementation
    }

    @Test
    public void addMicoServiceToMicoApplication()
        throws KafkaFaasConnectorNotAllowedHereException, MicoApplicationNotFoundException,
        MicoServiceInstanceNotFoundException, MicoApplicationIsNotUndeployedException, MicoTopicRoleUsedMultipleTimesException,
        KubernetesResourceException, MicoServiceAddedMoreThanOnceToMicoApplicationException, MicoServiceNotFoundException,
        MicoServiceDeploymentInformationNotFoundException, MicoApplicationDoesNotIncludeMicoServiceException {

        MicoApplication micoApplication = new MicoApplication()
            .setShortName(SHORT_NAME_1)
            .setVersion(VERSION_1_0_1)
            .setName(NAME_1)
            .setDescription(DESCRIPTION_1);

        MicoService micoService = new MicoService()
            .setShortName(SHORT_NAME_2)
            .setVersion(VERSION_1_0_2)
            .setDescription(DESCRIPTION_2);

        MicoServiceDeploymentInfo micoServiceDeploymentInfo = new MicoServiceDeploymentInfo()
            .setService(micoService)
            .setId(ID)
            .setInstanceId(INSTANCE_ID);

        given(micoApplicationRepository.findByShortNameAndVersion(micoApplication.getShortName(), micoApplication.getVersion()))
            .willReturn(Optional.of(micoApplication));
        given(micoServiceBroker.getServiceFromDatabase(micoService.getShortName(), micoService.getVersion()))
            .willReturn(micoService);
        given(micoServiceDeploymentInfoBroker.getMicoServiceDeploymentInformation(micoApplication.getShortName(), micoApplication.getVersion(), micoService.getShortName()))
            .willReturn(micoServiceDeploymentInfo);


        micoApplicationBroker.addMicoServiceToMicoApplicationByShortNameAndVersion(SHORT_NAME_1, VERSION_1_0_1, micoService.getShortName(), micoService.getVersion(), Optional.empty());
        assertThat(micoApplication.getServices().get(0).equals(micoService));
    }

}
