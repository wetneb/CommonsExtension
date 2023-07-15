package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.importers.TabularParserHelper.TableDataReader;
import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.StandardReconConfig;

/**
 * This class takes an Iterator<FileRecord> and converts each FileRecord to one or more rows
 *
 * @param iteratorFileRecords
 */
public class FileRecordToRows implements TableDataReader {

    protected StandardReconConfig reconConfig;

    protected String identifierSpace;
    protected String schemaSpace;
    protected String service;
    final Iterator<FileRecord> iteratorFileRecords;
    FileRecord fileRecord;
    final boolean categoriesColumn;
    final boolean mIdsColumn;
    int relatedCategoriesIndex = 0;

    public FileRecordToRows(Iterator<FileRecord> iteratorFileRecords, boolean categoriesColumn, boolean mIdsColumn) {

        this.iteratorFileRecords = iteratorFileRecords;
        this.categoriesColumn = categoriesColumn;
        this.mIdsColumn = mIdsColumn;
        this.identifierSpace = "https://commons.wikimedia.org/entity/";
        this.schemaSpace = "http://www.wikidata.org/prop/direct/";
        this.service = "https://commonsreconcile.toolforge.org/en/api";
        this.reconConfig = new StandardReconConfig(service, identifierSpace, schemaSpace, null, null, true, Collections.emptyList());

    }

    /**
     * This method iterates over the parameters of a file record spreading them in rows
     *
     * @return a row containing a cell per file record parameter
     */
    @Override
    public List<Object> getNextRowOfCells() throws IOException {

         
        List<Object> rowsOfCells;
        rowsOfCells = new ArrayList<>();
        // check if there's rows remaining from a previous call
        if (fileRecord != null && fileRecord.relatedCategories != null
                && relatedCategoriesIndex < fileRecord.relatedCategories.size()) {
            rowsOfCells.add(null);
            if (mIdsColumn) {
                rowsOfCells.add(null);
            }
            rowsOfCells.add(fileRecord.relatedCategories.get(relatedCategoriesIndex++));
            return rowsOfCells;
        } else if (iteratorFileRecords.hasNext()) {
            fileRecord = iteratorFileRecords.next();
            relatedCategoriesIndex = 0;
            if (fileRecord.fileName != null && ExpressionUtils.isNonBlankData(fileRecord.fileName)) {
                String id = "M" + fileRecord.pageId;
                if(id.startsWith(identifierSpace)) {
                    id = id.substring(identifierSpace.length());
                }

                ReconCandidate match = new ReconCandidate(id, fileRecord.fileName, new String[0], 100);
                Recon newRecon = reconConfig.createNewRecon(0)
                        .withMatch(match)
                        .withCandidates(Collections.singletonList(match))
                        .withMatchRank(0)
                        .withJudgment(Judgment.Matched)
                        .withJudgmentAction("mass");
                
                Cell newCell = new Cell(
                        fileRecord.fileName,
                        newRecon
                );

                rowsOfCells.add(newCell);
            }

            if (mIdsColumn) {
                rowsOfCells.add("M" + fileRecord.pageId);
            }
            if (categoriesColumn) {
                if (fileRecord.error != null) {
                    rowsOfCells.add(fileRecord.error);
                } else if (fileRecord.relatedCategories == null || fileRecord.relatedCategories.isEmpty()) {
                    rowsOfCells.add(null);
                } else if (fileRecord.relatedCategories != null) {
                    rowsOfCells.add(fileRecord.relatedCategories.get(relatedCategoriesIndex++));
                } else {
                    rowsOfCells.add(fileRecord.relatedCategories);
                }
            }
            return rowsOfCells;
        } else {
            return null;
        }

    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

}
