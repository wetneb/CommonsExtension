package org.openrefine.extensions.commons.functions;

import java.util.Arrays;

import org.openrefine.grel.ControlFunctionRegistry;
import org.openrefine.grel.PureFunction;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ExtractFromTemplateTest {

    PureFunction function = new ExtractFromTemplate();

    @BeforeClass
    public void registerFunction() {
        ControlFunctionRegistry.registerFunction("extractFromTemplate", function);
    }

    @Test
    public void testTemplateName() {
        String wikitext = "{{some template|bar=test}}\n"
                + "{{foo|bar={{other template}}}}\n"
                + "{{foo| foo = not important| bar = second value }}";

        Object result = function.call(new Object[] {wikitext, "foo", "bar"});

        Assert.assertEquals(result, Arrays.asList("{{other template}}", "second value"));
    }

    @Test
    public void testTemplateNameNewLine() {
        String wikitext = "{{foo\n|bar=hello}}";

        Object result = function.call(new Object[] {wikitext, "foo\n", "bar"});

        Assert.assertEquals(result, Arrays.asList("hello"));
    }
}
