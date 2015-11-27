package org.motechproject.export.model;

import org.motechproject.export.annotation.DataProvider;
import org.motechproject.export.annotation.ExcelDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;

public class ExcelExportProcessor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Object excelDataSource;

    public ExcelExportProcessor(Object excelDataSource) {
        this.excelDataSource = excelDataSource;
    }

    public String name() {
        return excelDataSource.getClass().getAnnotation(ExcelDataSource.class).name();
    }

    public String title() {
        return join(splitByCharacterTypeCamelCase(capitalize(name())), " ");
    }

    public static boolean isValidDataSource(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ExcelDataSource.class);
    }

    public List<Object> dataForPage(String reportName, int pageNumber) {
        try {
            Method method = getDataMethod(reportName);
            if (method != null) {
                return (List<Object>) method.invoke(excelDataSource, pageNumber);
            }
        } catch (IllegalAccessException e) {
            logger.error("Data method should be public" + e.getMessage());
            throw new RuntimeException("Data method should be public" + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        } catch (ClassCastException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        }
        return new ArrayList<Object>();
    }

    public List<Object> data(String reportName, Map<String, String> criteria) {
        try {
            Method method = getDataMethod(reportName);
            if (method != null) {
                return (List<Object>) method.invoke(excelDataSource, criteria);
            }
        } catch (IllegalAccessException e) {
            logger.error("Data method should be public" + e.getMessage());
            throw new RuntimeException("Data method should be public" + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        } catch (ClassCastException e) {
            logger.error("Format of data method is invalid." + e.getMessage());
            throw new RuntimeException("Format of data method is invalid." + e.getMessage());
        }
        return new ArrayList<Object>();
    }

    public List<String> columnHeaders(Type returnType) {
        return new ExportDataModel(returnType).columnHeaders();
    }

    public List<String> rowData(Object model, Type returnType) {
        return new ExportDataModel(returnType).rowData(model);
    }

    public ExportData getEntirExcelData(String reportName, Map<String, String> criteria) {
        List<String> headers = new ArrayList<>();
        List<List<String>> allRowData = new ArrayList<>();
        List<Object> data = data(reportName, criteria);
        if (data != null && !data.isEmpty()) {
            Class<? extends Object> clazz = data.get(0).getClass();
            headers = columnHeaders(clazz);
            for (Object datum : data) {
                allRowData.add(rowData(datum, clazz ));
            }
        }

        return new ExportData(headers, allRowData);
    }

    public ExportData getPaginatedExcelData(String reportName) {
        boolean doneBuilding = false;
        int pageNumber = 1;
        List<String> headers = new ArrayList<>();
        List<List<String>> allRowData = new ArrayList<>();

        while (!doneBuilding) {
            List<Object> data = dataForPage(reportName, pageNumber);
            if (data != null && !data.isEmpty()) {
                Class<? extends Object> clazz = data.get(0).getClass();
                headers = columnHeaders(clazz);
                List<List<String>> rowsOfAPage = new ArrayList<>();
                for (Object datum : data) {
                    rowsOfAPage.add(rowData(datum, clazz));
                }
                allRowData.addAll(rowsOfAPage);
                pageNumber++;
            } else {
                doneBuilding = true;
            }
        }

        return new ExportData(headers, allRowData);
    }

    private Method getDataMethod(String reportName) {
        for (Method method : excelDataSource.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(DataProvider.class) && method.getName().equalsIgnoreCase(reportName)) {
                return method;
            }
        }
        return null;
    }
}
