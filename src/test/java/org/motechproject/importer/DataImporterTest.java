package org.motechproject.importer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.importer.domain.CSVImportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:applicationBulkImportContext.xml")
public class DataImporterTest {

    @Autowired
    private CSVDataImporter dataImporter;

    @Autowired
    SampleCSVImporter sampleCSVImporter;

    @Before
    @After
    public void setUpAndTearDown() {
        sampleCSVImporter.isPostCalled = false;
        sampleCSVImporter.isValidateCalled = false;
        sampleCSVImporter.setValid(false);
    }

    @Test
    public void shouldImportSampleBean() {
        sampleCSVImporter.setValid(true);
        String fileName = "sample.csv";

        CSVImportResponse csvImportResponse = dataImporter.importData("sampleEntity", fileName);

        assertTrue(sampleCSVImporter.isPostCalled);
        assertTrue(sampleCSVImporter.isValidateCalled);
        assertThat(sampleCSVImporter.sampleBeans.get(0).getSampleX(), is("sampleX1"));
        assertThat(sampleCSVImporter.sampleBeans.get(0).getSampleY(), is("sampleY1"));
        assertEquals(fileName, csvImportResponse.getLastProcessedFileName());
        assertTrue(csvImportResponse.isImportSuccessful());
    }

    @Test
    public void shouldIgnoreFieldsThatAreNotPresentInFile() {
        sampleCSVImporter.setValid(true);

        dataImporter.importData("sampleEntity", "sample.csv");

        assertNull(sampleCSVImporter.sampleBeans.get(0).getNotPresentInFile());
    }

    @Test
    public void shouldUseSetterInjectionWhenSetterIsPresent() {
        sampleCSVImporter.setValid(true);

        dataImporter.importData("sampleEntity", "sample.csv");

        assertEquals("differentValueThanTheOnePresentInCSVFile", sampleCSVImporter.sampleBeans.get(0).getShouldBeSetUsingSetter());
    }

    @Test
    public void shouldProcessValidationErrorsIfAny() throws IOException, URISyntaxException {
        sampleCSVImporter.setValid(false);

        CSVImportResponse csvImportResponse = dataImporter.importData("sampleEntity", "sample.csv");

        assertFalse(csvImportResponse.isImportSuccessful());
        assertTrue(sampleCSVImporter.isValidateCalled);
        assertFalse(sampleCSVImporter.isPostCalled);
        URL errorsFilePath = this.getClass().getResource("/errors.csv");

        FileInputStream fileInputStream = new FileInputStream(errorsFilePath.toURI().getPath());
        String fileContent = IOUtils.toString(fileInputStream);

        assertEquals("this is a sample error for first record", fileContent.trim());
        FileUtils.deleteQuietly(new File(errorsFilePath.toString()));
    }

    @Test
    public void shouldUpdateValidRecordsAndProcessValidationErrorsForInvalidRecords() throws URISyntaxException, IOException {
        sampleCSVImporter.setValid(false);
        sampleCSVImporter.setInvalidRecords();

        CSVImportResponse csvImportResponse = dataImporter.importData("sampleEntity", true, "sample.csv");

        assertFalse(csvImportResponse.isImportSuccessful());

        assertTrue(sampleCSVImporter.isValidateCalled);
        URL errorsFilePath = this.getClass().getResource("/errors.csv");
        FileInputStream fileInputStream = new FileInputStream(errorsFilePath.toURI().getPath());
        String fileContent = IOUtils.toString(fileInputStream);
        assertEquals("this is a sample error for first record", fileContent.trim());
        FileUtils.deleteQuietly(new File(errorsFilePath.toString()));

        assertTrue(sampleCSVImporter.isPostCalled);
        assertEquals(1, sampleCSVImporter.sampleBeans.size());
        assertEquals("sampleX2", sampleCSVImporter.sampleBeans.get(0).getSampleX());
        assertEquals("sampleY2", sampleCSVImporter.sampleBeans.get(0).getSampleY());
    }
}
