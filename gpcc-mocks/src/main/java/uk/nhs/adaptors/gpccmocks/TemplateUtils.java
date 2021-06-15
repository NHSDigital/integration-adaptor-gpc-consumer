package uk.nhs.adaptors.gpccmocks;

import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lombok.SneakyThrows;

public class TemplateUtils {
    private static final String TEMPLATES_RESOURCE_ROOT = "templates";
    private static final String TEMPLATES_EXTENSION = ".mustache";


    private static Mustache loadTemplate(String templateName) {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory(TEMPLATES_RESOURCE_ROOT);
        return mustacheFactory.compile(templateName);
    }

    @SneakyThrows
    public static String fillTemplate(String templateName, Object content) {
        var template = loadTemplate(templateName+TEMPLATES_EXTENSION);
        StringWriter writer = new StringWriter();
        template.execute(writer, content).flush();
        return writer.toString();
    }

}
