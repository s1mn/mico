package io.github.ust.mico.core;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

// TODO: Setup proper integration testing with neo4j
@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
public class GitHubCrawlerTests {
    private static final String REPO_URI = "https://api.github.com/repos/octokit/octokit.rb";
    private static final String RELEASE = "v4.12.0";

    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private DependsOnRepository dependsOnRepository;
    @Autowired
    private ServiceInterfaceRepository serviceInterfaceRepository;

    @Test
    public void testGitHubCrawlerLatestRelease() {
        serviceRepository.deleteAll();
        dependsOnRepository.deleteAll();
        serviceInterfaceRepository.deleteAll();

        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        GitHubCrawler crawler = new GitHubCrawler(restTemplate);
        Service service = crawler.crawlGitHubRepoLatestRelease(REPO_URI);
        serviceRepository.save(service);

        Service readService = serviceRepository.findByShortNameAndVersion(service.getShortName(), service.getVersion());
        assertEquals(service.getShortName(), readService.getShortName());
        assertEquals(service.getDescription(), readService.getDescription());
        assertEquals(service.getId(), readService.getId());
        assertEquals(service.getVersion(), readService.getVersion());
        assertEquals(service.getVcsRoot(), readService.getVcsRoot());
        assertEquals(service.getName(), readService.getName());
    }

    @Test
    public void testGitHubCrawlerSpecificRelease() {
        serviceRepository.deleteAll();
        dependsOnRepository.deleteAll();
        serviceInterfaceRepository.deleteAll();

        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        GitHubCrawler crawler = new GitHubCrawler(restTemplate);
        Service service = crawler.crawlGitHubRepoSpecificRelease(REPO_URI, RELEASE);
        serviceRepository.save(service);

        Service readService = serviceRepository.findByShortNameAndVersion(service.getShortName(), service.getVersion());
        assertEquals(service.getShortName(), readService.getShortName());
        assertEquals(service.getDescription(), readService.getDescription());
        assertEquals(service.getId(), readService.getId());
        assertEquals(service.getVersion(), readService.getVersion());
        assertEquals(service.getVcsRoot(), readService.getVcsRoot());
        assertEquals(service.getName(), readService.getName());
    }

    @Test
    public void testGitHubCrawlerAllReleases() {
        serviceRepository.deleteAll();
        dependsOnRepository.deleteAll();
        serviceInterfaceRepository.deleteAll();

        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        GitHubCrawler crawler = new GitHubCrawler(restTemplate);
        List<Service> serviceList = crawler.crawlGitHubRepoAllReleases(REPO_URI);
        serviceRepository.saveAll(serviceList);

        Service readService = serviceRepository.findByShortNameAndVersion(serviceList.get(0).getShortName(), serviceList.get(0).getVersion());
        assertEquals(serviceList.get(0).getShortName(), readService.getShortName());
        assertEquals(serviceList.get(0).getDescription(), readService.getDescription());
        assertEquals(serviceList.get(0).getId(), readService.getId());
        assertEquals(serviceList.get(0).getVersion(), readService.getVersion());
        assertEquals(serviceList.get(0).getVcsRoot(), readService.getVcsRoot());
        assertEquals(serviceList.get(0).getName(), readService.getName());
    }

    @Test
    public void testMakeExternalVersionInternal() {
        final String VERSION1 = "1.0.0";
        final String VERSION2 = "1.0.0-foo";
        final String VERSION3 = "1.0.0-0.1.1";
        final String VERSION4 = "1.0.0-rc.1";
        final String VERSION5 = "v1.0.0";
        final String VERSION6 = "v1.0.0-foo";
        final String VERSION7 = "v1.0.0-0.1.1";
        final String VERSION8 = "v1.0.0-rc.1";
        final String VERSION_EXPECTED = "1.0.0";

        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        GitHubCrawler crawler = new GitHubCrawler(restTemplate);

        try {
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION1));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION2));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION3));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION4));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION5));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION6));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION7));
            assertEquals(VERSION_EXPECTED, crawler.makeExternalVersionInternal(VERSION8));
        } catch (VersionNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = VersionNotSupportedException.class)
    public void testVersionNotSupportedException() throws VersionNotSupportedException {
        final String VERSION = "some-strange-version-1.0.0";
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        GitHubCrawler crawler = new GitHubCrawler(restTemplate);
        String version = crawler.makeExternalVersionInternal(VERSION);
    }

    @Test
    public void cleanupDatabase() {
        serviceRepository.deleteAll();
        dependsOnRepository.deleteAll();
        serviceInterfaceRepository.deleteAll();
    }
}