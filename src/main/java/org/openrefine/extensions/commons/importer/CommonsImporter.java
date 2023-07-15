package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import org.openrefine.ProjectMetadata;
import org.openrefine.importers.TabularParserHelper;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.Runner;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.model.recon.StandardReconConfig.ColumnDetail;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.JSONUtilities;

public class CommonsImporter {

    static String apiUrl = "https://commons.wikimedia.org/w/api.php";


    /**
     * Utility method for testing with mock api calls
     *
     * @param limit
     */
    public void setApiUrl(String apiUrlTest) {
        apiUrl = apiUrlTest;
    }

    static public Grid parsePreview(
            Runner runner,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws Exception {

        return parse(
                runner,
                metadata,
                job,
                limit,
                options,
                exceptions
        );

    }

    static public Grid parse(
            Runner runner,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws Exception {

        List<CategoryWithDepth> categoriesWithDepth = new ArrayList<>();
        Iterator<FileRecord> rcf;
        Iterator<FileRecord> fetchedFiles = Collections.emptyIterator();
        JSONUtilities.safePut(options, "headerLines", 0);
        /* get user-input from the Post request parameters */
        JsonNode categoryInput = options.get("categoryJsonValue");
        boolean mIdsColumn = options.get("mIdsColumn").asBoolean();
        boolean categoriesColumn = options.get("categoriesColumn").asBoolean();
        for (JsonNode category: categoryInput) {
            categoriesWithDepth.add(new CategoryWithDepth(category.get("category").asText(),
                    category.get("depth").asInt()));
        }
        String service = "https://commonsreconcile.toolforge.org/en/api";

        // initializes progress reporting with the name of the first category
        setProgress(job, categoriesWithDepth.get(0).categoryName, 0);

        for(CategoryWithDepth categoryWithDepth: categoriesWithDepth) {
            fetchedFiles = Iterators.concat(fetchedFiles,
                    FileFetcher.listCategoryMembers(apiUrl, categoryWithDepth.categoryName, categoryWithDepth.depth));
        }
        if (categoriesColumn) {
            rcf = new RelatedCategoryFetcher(apiUrl, fetchedFiles);
        } else {
            rcf = fetchedFiles;
        }

        TableDataReader dataReader = new FileRecordToRows(rcf, categoriesColumn, mIdsColumn);
        
        List<ColumnMetadata> columns = new ArrayList<>();
        StandardReconConfig cfg = new StandardReconConfig(
                service,
                "https://commons.wikimedia.org/entity/",
                "http://www.wikidata.org/prop/direct/",
                "",
                "entity",
                true,
                new ArrayList<ColumnDetail>(),
                1);
        ColumnMetadata fileColumn = new ColumnMetadata("File")
                .withReconConfig(cfg);
        columns.add(fileColumn);
        if (mIdsColumn) {
            columns.add(new ColumnMetadata("M-ids"));
        }
        if (categoriesColumn) {
            columns.add(new ColumnMetadata("Categories"));
        }
        
        ColumnModel columnModel = new ColumnModel(columns)
                .withHasRecords(categoriesColumn);
        TabularParserHelper tabularParsingHelper = new TabularParserHelper();
        Grid grid = tabularParsingHelper.parseOneFile(
                runner, metadata, job, "", "",
                dataReader, limit, options);

        setProgress(job, categoriesWithDepth.get(categoriesWithDepth.size()-1).categoryName, 100);
        return grid.withColumnModel(columnModel);
    }

    static private void setProgress(ImportingJob job, String category, int percent) {
        job.setProgress(percent, "Reading " + category);
    }

}
