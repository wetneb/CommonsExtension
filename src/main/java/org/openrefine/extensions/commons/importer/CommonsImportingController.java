package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineModel;
import org.openrefine.RefineServlet;
import org.openrefine.commands.HttpUtilities;
import org.openrefine.importing.ImportingController;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.GridCache;
import org.openrefine.model.changes.LazyChangeDataStore;
import org.openrefine.model.changes.LazyGridCache;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommonsImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("CommonsImportingController");
    protected RefineServlet servlet;
    public static int DEFAULT_PREVIEW_LIMIT = 50;
    public static int DEFAULT_PROJECT_LIMIT = 0;

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }

    /* Handling of http requests between frontend and OpenRefine servlet */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if(logger.isDebugEnabled()){
            logger.debug("doPost Query String::{}", request.getQueryString());
        }
        response.setCharacterEncoding("UTF-8");
        Properties parameters = ParsingUtilities.parseUrlParameters(request);

        String subCommand = parameters.getProperty("subCommand");

        if(logger.isDebugEnabled()){
            logger.info("doPost::subCommand::{}", subCommand);
        }

        if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("parse-preview".equals(subCommand)) {
            try {

                doParsePreview(request, response, parameters);

            } catch (Exception e) {
                logger.error("doPost::CommonsServiceException::{}", e);
                HttpUtilities.respond(response, "error", "Unable to parse preview");
            }
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }

    }

    /**
     *
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     */
    private void doInitializeParserUI(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        if(logger.isDebugEnabled()) {
            logger.debug("::doInitializeParserUI::");
        }

        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, "options", options);

        JSONUtilities.safePut(options, "skipDataLines", 0);
        if(logger.isDebugEnabled()) {
            logger.debug("doInitializeParserUI:::{}", result.toString());
        }

        HttpUtilities.respond(response, result.toString());

    }

    /**
     * doParsePreview
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     */
    private void doParsePreview(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
                throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        try {
            ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                    request.getParameter("options"));

            List<Exception> exceptions = new LinkedList<Exception>();

            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            
            try {
                Grid grid = CommonsImporter.parsePreview(
                        RefineModel.getRunner(),
                        job.metadata,
                        job,
                        DEFAULT_PREVIEW_LIMIT,
                        optionObj,
                        exceptions
                );
                job.setProject(new Project(grid, new LazyChangeDataStore(RefineModel.getRunner()), new LazyGridCache()));
                
                writer.writeStartObject();
                writer.writeStringField("status", "ok");
                writer.writeEndObject();
            } catch(Exception e) {
                writer.writeStartObject();
                writer.writeStringField("status", "error");
                writer.writeStringField("message", e.toString());
                writer.writeEndObject();
            } finally {
                writer.flush();
                writer.close();
                w.flush();
                w.close();
            }

        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            job.touch();
            job.updating = false;
        }
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }
        
        job.updating = true;
        try {
            final ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                    request.getParameter("options"));

            final List<Exception> exceptions = new LinkedList<Exception>();

            job.setState("creating-project");

            new Thread() {

                @Override
                public void run() {
                    ProjectMetadata pm = new ProjectMetadata();
                    pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
                    pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));

                    try {
                        Grid grid = CommonsImporter.parse(
                                RefineModel.getRunner(),
                                pm,
                                job,
                                DEFAULT_PROJECT_LIMIT ,
                                optionObj,
                                exceptions
                        );
                        long projectId = Project.generateID();
                        ChangeDataStore dataStore = ProjectManager.singleton.getChangeDataStore(projectId);
                        GridCache gridStore = ProjectManager.singleton.getGridCache(projectId);
                        job.setProject(new Project(projectId, grid, dataStore, gridStore));
                        
                        ProjectManager.singleton.registerProject(job.getProject(), pm);
                        job.setState("created-project");
                        job.setProjectID(job.getProject().getId());
                    } catch(Exception e) {
                        job.setError(Collections.singletonList(e));
                    }

                    if (!job.canceled) {
                        job.touch();
                        job.updating = false;
                    }
                }
            }.start();

            HttpUtilities.respond(response, "ok", "done");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
