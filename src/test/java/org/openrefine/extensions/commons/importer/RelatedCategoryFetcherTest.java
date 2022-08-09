package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class RelatedCategoryFetcherTest {

    /**
     * Test list generation of categories related to a given file
     */
    @Test
    public void testNext() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponseContinue = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127722\":"
                    + "{\"pageid\":127722,\"ns\":6,\"title\":\"File:LasTres.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponseContinue));
            List<FileRecord> originalRecords = new ArrayList<>();
            FileRecord fr0 = new FileRecord("File:LasTres.jpg", "127722", null);
            originalRecords.add(fr0);
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), originalRecords.iterator());

            List<Object> rows = new ArrayList<>();
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
            Assert.assertFalse(rcf.hasNext());
            List<String> categories = new ArrayList<>();
            categories.add("Category:Costa Rica");
            categories.add("Category:Cute dogs");
            categories.add("Category:Costa Rican dogs");
            FileRecord file0 = new FileRecord("File:LasTres.jpg", "127722", categories);

            Assert.assertEquals(rows.get(0), file0);

        }
    }

}
