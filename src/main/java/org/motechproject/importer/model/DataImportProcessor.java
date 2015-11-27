
package org.motechproject.importer.model;



import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.motechproject.export.annotation.SyncFlw;
import org.motechproject.importer.annotation.Post;
import org.motechproject.importer.annotation.Sync;
import org.motechproject.importer.annotation.Validate;
import org.motechproject.importer.domain.CSVImportResponse;
import org.motechproject.importer.domain.Error;
import org.motechproject.importer.domain.ValidationException;
import org.motechproject.importer.domain.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class DataImportProcessor {
    private Object importer;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Method postMethod;
    private Method validateMethod;
    private Method syncMethod;
    private Method syncFlwMethod;
    protected Class bean;

    protected DataImportProcessor(Class bean) {
        this.bean = bean;
    }

    public DataImportProcessor(Object importer, Class bean) {
        this.importer = importer;
        this.bean = bean;
        for (Method method : this.importer.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Post.class)) {
                postMethod = method;
            }
            if (method.isAnnotationPresent(Validate.class)) {
                validateMethod = method;
            }
            if(method.isAnnotationPresent(Sync.class)) {
            	syncMethod = method;
            }
            if(method.isAnnotationPresent(SyncFlw.class)) {
            	syncFlwMethod = method;
            }
        }
    }

    public CSVImportResponse process(Boolean shouldUpdateValidRecords, String... filePaths) {
        String lastRunFilePath = StringUtils.EMPTY;
        try {
            for (String filePath : filePaths) {
                ValidationResponse validationResponse = process(new FileReader(filePath), shouldUpdateValidRecords);

                if (!validationResponse.isValid()) {
                    processErrors(validationResponse.getErrors(), filePath);
                    throw new ValidationException();
                }
                lastRunFilePath = new File(filePath).getName();
            }
        } catch (Exception e) {
            System.err.println("Error while importing csv : " + ExceptionUtils.getFullStackTrace(e));
            logger.error("Error while importing csv : " + ExceptionUtils.getFullStackTrace(e));
            return new CSVImportResponse(lastRunFilePath, false);
        }
        return new CSVImportResponse(lastRunFilePath, true);
    }

    private ValidationResponse process(Reader reader, Boolean shouldUpdateValidRecords) throws Exception {
        List<Object> valuesFromFile = parse(reader);
        ValidationResponse validationResponse = validate(valuesFromFile);
        if (validationResponse.isValid()) {
            invokePostMethod(valuesFromFile);
        } else if (shouldUpdateValidRecords) {
            List<Object> validRecords = (List<Object>) CollectionUtils.disjunction(valuesFromFile, validationResponse.getInvalidRecords());
            invokePostMethod(validRecords);
        }
        return validationResponse;
    }

    public String processContent(String content, Boolean shouldUpdateValidRecords) throws Exception {
        ValidationResponse validationResponse = process(new StringReader(content), shouldUpdateValidRecords);

        if (validationResponse.isValid()) {
            return null;
        }

        StringWriter stringWriter = new StringWriter();
        processErrors(validationResponse.getErrors(), stringWriter);
        return stringWriter.toString();
    }
    
    public String processValidate(String content) throws Exception {
        List<Object> valuesFromFile = parse(new StringReader(content));
        ValidationResponse validationResponse = validate(valuesFromFile);
        if (validationResponse.isValid()) {
            return "passed";
        } else {
        	return "failed";
        }
    }
    
    public String download(String content) throws Exception{
    	 List<Object> valuesFromFile = parse(new StringReader(content));
         ValidationResponse validationResponse = validate(valuesFromFile);
    	 StringWriter stringWriter = new StringWriter();
         processErrors(validationResponse.getErrors(), stringWriter);
         return stringWriter.toString();
    }
    
    public String processPersist(String content) throws Exception {
        List<Object> valuesFromFile = parse(new StringReader(content));
        return invokePostMethod(valuesFromFile);
    }
    
   
    
    
    public Object processSync(String content) throws Exception{
    	 List<Object> entities = new ArrayList<Object>();
    	 entities.add(content);
         return invokeSyncMethod(entities);
    	
    }
    
    private String invokePostMethod(List<Object> entities) throws IllegalAccessException, InvocationTargetException {
        String result = null;
    	if (postMethod != null){
    		result = (String) postMethod.invoke(importer, entities);
    	}
    	return result;
    }
    
    private String invokeSyncMethod(List<Object> entities) throws IllegalAccessException, InvocationTargetException {
       String result = null;
    	if (syncMethod != null){
    		result = (String) syncMethod.invoke(importer, entities);
    	}
    	return result;
    }

    private ValidationResponse validate(List<Object> valuesFromFile) throws IllegalAccessException, InvocationTargetException {
        ValidationResponse validationResponse = new ValidationResponse(true);
        if (validateMethod != null) {
            validationResponse = (ValidationResponse) validateMethod.invoke(importer, valuesFromFile);
        }
        return validationResponse;
    }

    private void processErrors(List<Error> errors, String filePath) throws IOException {
        String fileDirectory = new File(new File(filePath).getAbsolutePath()).getParent();
        File errorsFile = new File(fileDirectory + File.separator + "errors.csv");
        errorsFile.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(errorsFile));
        processErrors(errors, writer);
    }

    private void processErrors(List<Error> errors, Writer writer) throws IOException {
        for (Error error : errors) {
            writer.write(error.getMessage());
            writer.write("\n");
        }
        writer.close();
    }

    public abstract String entity();

    public abstract List<Object> parse(Reader reader) throws Exception;
}
