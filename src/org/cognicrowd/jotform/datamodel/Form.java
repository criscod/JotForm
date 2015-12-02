package org.cognicrowd.jotform.datamodel;

import java.util.List;

/**
 * @author csarasua
 */
public class Form {

    private String formId;
    private String name;

    private TypeOfForm type;

    private List<Question> listOfQuestions;

    public Form()
    {

    }

    public Form (String id, String name, TypeOfForm type)
    {
        this.formId = id;
        this.name = name;
        this.type = type;
        // Reads the other properties via the JotForm API


    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeOfForm getType() {
        return type;
    }

    public void setType(TypeOfForm type) {
        this.type = type;
    }

    public List<Question> getListOfQuestions() {
        return listOfQuestions;
    }

    public void setListOfQuestions(List<Question> listOfQuestions) {
        this.listOfQuestions = listOfQuestions;
    }
}
