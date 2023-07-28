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
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtTemplateArgument;
import org.sweble.wikitext.parser.nodes.WtTemplateArguments;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;
import org.sweble.wikitext.parser.utils.WtPrettyPrinter;

import de.fau.cs.osr.ptk.common.AstVisitor;

public class ExtractFromTemplate extends PureFunction {

    private static final long serialVersionUID = 6466726945401794218L;

    public class FindTemplateValues extends AstVisitor<WtNode> {

        private String templateName;
        private String paramName;
        private List<String> values = new ArrayList<>();

        // Constructor
        public FindTemplateValues(String tName, String pName) {
            this.templateName = tName;
            this.paramName = pName;
        }

        public void visit(WtNode node) {
            iterate(node);
        }
        public void visit(WtTemplate template) {
            if (templateName.trim().equals(template.getName().getAsString().trim())) {
                WtTemplateArguments args = template.getArgs();
                for (int i = 0; i != args.size(); i++) {
                    WtTemplateArgument arg = (WtTemplateArgument) args.get(i);

                    if (paramName.equals(arg.getName().getAsString().trim())) {
                        values.add(WtPrettyPrinter.print(arg.getValue()).trim());
                    }
                }
            }
        }

    }

    // Set-up a simple wiki configuration
    ParserConfig parserConfig = new SimpleParserConfig();

    @Override
    public Object call(Object[] args) {
        if (args.length != 3 || !(args[0] instanceof String)) {
            return new EvalError("Unexpected arguments for "+ControlFunctionRegistry.getFunctionName(this) + "(): got '" + new Type().call(args) + "' but expected a single String as an argument");
        }

        try {
            WtParsedWikitextPage parsedArticle = WikitextParsingUtilities.parseWikitext((String) args[0]);
            String tName = (String) args[1];
            String pName = (String) args[2];

            FindTemplateValues extractor = new FindTemplateValues(tName, pName);
            extractor.go(parsedArticle);

            List<String> values = extractor.values;

            return values;

        } catch(IOException |xtc.parser.ParseException  e1) {
            return new EvalError("Could not parse wikitext: "+e1.getMessage());
        }
    }


    @Override
    public String getDescription() {
        return "extracts the list of values of a given parameter from the wikitext of a template";
    }

    public String getParams() {
        return "";
    }

    @Override
    public String getReturns() {
        return "arrays of strings";
    }

}
