package org.motechproject.export.model;

import org.motechproject.export.annotation.ExportValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

public class Columns extends ArrayList<Column> {

    public Columns(Class typeParameter) {
        for (Method declaredMethod : typeParameter.getMethods()) {
            if (declaredMethod.isAnnotationPresent(ExportValue.class)) {
                validateValueMethod(declaredMethod);
                this.add(new Column(declaredMethod));
            }
        }
        Collections.sort(this);
    }

    private void validateValueMethod(Method method) {
        if (method.getParameterTypes().length > 0) {
            throw new RuntimeException("value method should have no parameter");
        }
    }
}
