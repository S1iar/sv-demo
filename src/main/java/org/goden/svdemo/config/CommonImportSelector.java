package org.goden.svdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommonImportSelector implements ImportSelector {
    private static final Logger log = LoggerFactory.getLogger(CommonImportSelector.class);

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {

        List<String> imports = new ArrayList<>();
        InputStream is = CommonImportSelector.class.getClassLoader().getResourceAsStream("common-imports");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                imports.add(line);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }

        }


        return imports.toArray(new String[0]);

    }
}
