package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrefine.model.Cell;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FileRecordToRowsTest {

    /**
     * Test row generation from file records
     */
    @Test
    public void testGetNextRowOfCells() throws Exception {

        List<String> categories = Arrays.asList("Category:Costa Rica", "Category:Cute dogs", "Category:Costa Rican dogs");
        FileRecord file0 = new FileRecord("File:LasTres.jpg", "127722", categories, null);
        FileRecord file1 = new FileRecord("File:Playa Gandoca.jpg", "112933", null, null);
        List<FileRecord> fileRecords = Arrays.asList(file0, file1);
        FileRecordToRows frtr = new FileRecordToRows(fileRecords.iterator(), true, true);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        
        
        Assert.assertEquals(((Cell) rows.get(0).get(0)).value, "File:LasTres.jpg");
        Assert.assertEquals(((Cell) rows.get(0).get(0)).recon.match.id, "M127722");
        Assert.assertEquals(rows.get(0).get(1), "M127722");
        Assert.assertEquals(rows.get(0).get(2), "Category:Costa Rica");
        Assert.assertEquals(rows.get(1), Arrays.asList(null, null, "Category:Cute dogs"));
        Assert.assertEquals(rows.get(2), Arrays.asList(null, null, "Category:Costa Rican dogs"));
        Assert.assertEquals(((Cell) rows.get(3).get(0)).value, "File:Playa Gandoca.jpg");
        Assert.assertEquals(((Cell) rows.get(3).get(0)).recon.match.id, "M112933");
        Assert.assertEquals(rows.get(3).get(1), "M112933");
        Assert.assertNull(rows.get(3).get(2));
        Assert.assertEquals(rows.get(4), null);

    }

}
