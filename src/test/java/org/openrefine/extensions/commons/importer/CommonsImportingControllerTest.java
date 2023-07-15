package org.openrefine.extensions.commons.importer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openrefine.RefineServlet;
import org.openrefine.importing.ImportingManager;
import org.openrefine.io.FileProjectManager;
import org.openrefine.model.ModelException;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.column.ColumnAdditionByFetchingURLsOperation;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.mit.simile.butterfly.ButterflyModule;

public class CommonsImportingControllerTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    protected Logger logger;

    protected RefineServlet servlet;

    // System under test
    private CommonsImportingController SUT = null;

    public static File createTempDirectory(String name)
            throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }

    protected ButterflyModule getCoreModule() {
        ButterflyModule coreModule = mock(ButterflyModule.class);
        when(coreModule.getName()).thenReturn("core");
        return coreModule;
    }

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation("core", "column-addition-by-fetching-urls",
                ColumnAdditionByFetchingURLsOperation.class);
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {

        MockitoAnnotations.initMocks(this);

        File dir = createTempDirectory("OR_CommonsExtension_Test_WorkspaceDir");
        FileProjectManager.initialize(dir);

        servlet = new RefineServlet();
        ImportingManager.initialize(servlet);

    }

    @AfterMethod
    public void tearDown() {
        SUT = null;
        request = null;
        response = null;
    }

    @Test
    public void testDoGet() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            when(response.getWriter()).thenReturn(pw);

            SUT.doGet(request, response);

            String result = sw.getBuffer().toString().trim();
            ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);
            String code = json.get("status").asText();
            String message = json.get("message").asText();
            Assert.assertNotNull(code);
            Assert.assertNotNull(message);
            Assert.assertEquals(code, "error");
            Assert.assertEquals(message, "GET not implemented");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
