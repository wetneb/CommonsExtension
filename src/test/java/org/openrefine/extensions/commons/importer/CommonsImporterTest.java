package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineServlet;
import org.openrefine.RefineTest;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.util.ParsingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CommonsImporterTest extends RefineTest {

    protected RefineServlet servlet;

    // dependencies
    private ProjectMetadata metadata;
    private ImportingJob job;

    /**
     * Test column names upon project creation as well as reconciled cells
     */
    @Test
    public void testParse() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":127722,\"ns\":6,\"title\":\"File:3 Puppies.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));

            metadata = new ProjectMetadata();
            metadata.setName("Commons Import Test Project");
            
            job = Mockito.mock(ImportingJob.class);
            ObjectNode options = ParsingUtilities.evaluateJsonStringToObjectNode(
                    "{\"categoryJsonValue\":[{\"category\":\"Category:Costa Rica\",\"depth\":\"0\"}],\"skipDataLines\":0,"
                    + "\"limit\":-1,\"disableAutoPreview\":false,\"categoriesColumn\":true,\"mIdsColumn\":true}");
            List<Exception> exceptions = new ArrayList<Exception>();
            CommonsImporter importer = new CommonsImporter();

            importer.setApiUrl(url.toString());
            
            Grid grid = CommonsImporter.parse(runner(), metadata, job, 0, options, exceptions);


            Cell cell = grid.getRow(0).cells.get(0);
            ColumnModel columnModel = grid.getColumnModel();
            Assert.assertEquals(columnModel.getColumnByIndex(0).getName(), "File");
            Assert.assertEquals(columnModel.getColumnByIndex(1).getName(), "M-ids");
            Assert.assertEquals(columnModel.getColumnByIndex(2).getName(), "Categories");
            Assert.assertEquals(cell.recon.match.id, "M127722");
            Assert.assertEquals(cell.recon.match.name, "File:3 Puppies.jpg");

            server.close();

        }
    }
}
