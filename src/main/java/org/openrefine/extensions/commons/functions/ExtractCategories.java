package org.openrefine.extensions.commons.functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrefine.expr.EvalError;
import org.openrefine.expr.functions.Type;
import org.openrefine.extensions.commons.utils.WikitextParsingUtilities;
import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.PureFunction;
import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;

import de.fau.cs.osr.ptk.common.AstVisitor;

public class ExtractCategories extends PureFunction {

    private static final long serialVersionUID = 2946414542313290087L;

    public static class CategoriesExtractor extends AstVisitor<WtNode>{

        private List<String> categories = new ArrayList<>();

        public void visit(WtNode node) {
            iterate(node);
        }
        public void visit(WtInternalLink internalLink) {
            String currentInternalLink = internalLink.getTarget().getAsString();
            if (currentInternalLink.startsWith("Category:")) {
                categories.add(currentInternalLink);
            }
        }

    }

    // Set-up a simple wiki configuration
    ParserConfig parserConfig = new SimpleParserConfig();

    @Override
    public Object call(Object[] args) {
        if (args.length != 1 || !(args[0] instanceof String)) {
            return new EvalError("Unexpected arguments for "+ControlFunctionRegistry.getFunctionName(this) + "(): got '" + new Type().call(args) + "' but expected a single String as an argument");
        }

        try {
            WtParsedWikitextPage parsedArticle = WikitextParsingUtilities.parseWikitext((String) args[0]);

            CategoriesExtractor extractor = new CategoriesExtractor();
            extractor.go(parsedArticle);
            List<String> result = extractor.categories;

            return result;

        } catch(IOException |xtc.parser.ParseException  e1) {
            return new EvalError("Could not parse wikitext: "+e1.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "extracts the list of categories from the wikitext of a page";
    }

    @Override
    public String getReturns() {
        return "arrays of strings";
    }

}
