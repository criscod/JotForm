package org.cognicrowd.jotform.formgeneration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cognicrowd.jotform.datamodel.Form;
import org.cognicrowd.jotform.datamodel.TypeOfForm;
import org.cognicrowd.jotform.util.ConfigurationManager;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author csarasua
 *         Manages the generation of forms made out of shuffled combinations of a set of forms (i.e. forms for ETS tests and forms for tasks).
 */
public class FormManager {

    // Main directory of the code.
    static String workingDir = System.getProperty("user.dir");
    static String workingDirForFileName = workingDir.replace("\\", "/");

    // All tests that there are available.
    List<Form> listOfTests = new ArrayList<Form>();
    // All tasks that there are available.
    List<Form> listOfTasks = new ArrayList<Form>();

    // The set of shuffleLists created by the method "createSetOfShuffledLists"
    Set<List<Form>> setOfShuffledLists = new HashSet<List<Form>>();

    // Counter for calculating the order (position in form) of each question.
    int countCurrentOrder = 0;

    /**
     * Constructor
     *
     * @param tests file containing the list of available tests
     * @param tasks file containing the list of available tasks
     */
    public FormManager(File tests, File tasks) {
        this.readForms(tests, this.listOfTests, TypeOfForm.Test);
        this.readForms(tasks, this.listOfTasks, TypeOfForm.Task);
    }

    /**
     * Reads the CSV file and creates a list of forms (either tasks or tests).
     *
     * @param f    CSV file containing the description of the forms to load into memory.
     * @param list list to which the forms need to be added.
     * @param type the type of forms that need to be created.
     */
    private void readForms(File f, List<Form> list, TypeOfForm type) {
        Reader in = null;
        try {
            // Reads the tests

            // Creates the file reader.
            in = new FileReader(f);
            //Creates the header of the CSV file.
            final String[] FILE_HEADER_MAPPING = {"'id'", "'title'"};

            // Gets all the CSV records according to the header defined before.
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING).parse(in);

            // note: When including the header in the file it does not leave out the header in the records iterable

            // Goes through all available CSV records.
            for (CSVRecord record : records) {
                // Gets the two attributes for each record (id and title).
                String id = record.get("'id'");
                String title = record.get("'title'");

                // Creates a new form.
                Form form = new Form(id, title, type);
                // Adds the created form to the input list, in which the corresponding forms are kept.
                list.add(form);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the set of shuffled lists of tests and tasks.
     * Each shuffled list is a random permutation of tests and tasks.
     * Each shuffled list includes ALL tests and tasks.
     *
     * @param size the number of shuffled lists to be created (one per user).
     */
    public void createSetOfShuffledLists(int size) {
        List<Form> shuffledList = new ArrayList<Form>();

        // Creates a list containing ALL forms.
        List<Form> listOfEverything = new ArrayList<Form>();
        listOfEverything.addAll(this.listOfTests);
        listOfEverything.addAll(this.listOfTasks);

        // Creates the list that is going to be shuffled, containing all forms (of tests and tasks).
        List<Form> listOfEverythingSuffleable = new ArrayList<Form>();
        listOfEverythingSuffleable.addAll(listOfEverything);

        // Create shuffled forms as many times as the value of "size". This number represents the number of subjects.
        for (int i = 0; i < size; i++) {
            // Creates a new shuffled list --- one list for a particular user
            List<Form> newShuffledList = new ArrayList<Form>();
            boolean newShuffled = false;
            // Assumes that the number of possible permutations is smaller or equal to the size (the number of shuffled lists to be built).

            // While no new unique shuffled list has been created, keeps on generating a new one.
            while (!newShuffled) {

                // Shuffles the list.
                Collections.shuffle(listOfEverythingSuffleable);
                // Creates a new object for the new shuffled list.
                newShuffledList = new ArrayList<Form>();
                // Copies all the elements in the current status of the shuffleable list
                newShuffledList.addAll(listOfEverythingSuffleable);
                // Adds the new shuffled list to the set of shuffled lists
                // Checks that the candidate shuffled list of forms has not yet been generated and kept.
                if (this.setOfShuffledLists.size() == 0 || (this.setOfShuffledLists.size() != 0 && !isRepeatedShuffledList(newShuffledList)))
                {
                    newShuffled = true;
                }

            }
            // It is new, therefore it is kept in the final list.
            this.setOfShuffledLists.add(newShuffledList);
        }

    }

    /**
     * Checks whether a (candidate shuffled) list of forms has been already generated and stored in the setOfShuffledLists.
     * @param list
     * @return true / false indicating whether it is or not a repeated list.
     */
    private boolean isRepeatedShuffledList(List<Form> list) {

        boolean result = false;

        for (List<Form> listI : this.setOfShuffledLists) {

            if (listI.size() == list.size()) {
                int equals = 0;
                for (int i = 0; i < list.size(); i++) {
                    System.out.println("list shuffle candidate " + list.get(i).getFormId());
                    System.out.println("list in set already " + listI.get(i).getFormId());


                    if (list.get(i).getFormId() != listI.get(i).getFormId()) {
                        break;
                    } else {
                        equals = equals + 1;
                    }
                }
                if (equals == list.size()) {
                    result = true;
                }
            }


        }


        return result;
    }

    /**
     * Creates the set of forms in JotForm out of the shuffledLists.
     *
     * @param parts number of parts to build per shuffledList (in case two sessions need to be created).
     */
    public void serialiseSetOfShuffledLists(int parts) {
        // For all the shuffledLists in the set generated with the method "createSetOfShuffledLists"

        int countList = 0;
        for (List<Form> list : this.setOfShuffledLists) {
            countList++;
            this.countCurrentOrder = 0;

            // Creates the HTTP clients.
            HttpClient client = new DefaultHttpClient();
            HttpClient client2 = new DefaultHttpClient();
            HttpClient client3 = new DefaultHttpClient();

            // Gets the key of the API.
            String key = ConfigurationManager.getInstance().getApiKey();

            HttpResponse response = null;

            // Creates the Http POST request to generate the new forms.
            HttpPost postForm = new HttpPost(ConfigurationManager.getInstance().getHttpPostForm() + "?apiKey=" + key);
            System.out.println(postForm.getURI());
            postForm.setHeader("Accept", "application/json");


            String formData = "{\"properties\":{ \n " +
                    " \"title\":\" Form" + countList + "\"" +
                    " }}";

            StringEntity formDataEntity = new StringEntity(formData, ContentType.create(
                    "application/json", "UTF-8"));
            System.out.println("data: " + formData);
            postForm.setEntity(formDataEntity);


            int statusNewForm = 1;
            String newCreatedId = null;

            // Tries until it gives a 200 as response.
            while (statusNewForm == 1) {
                try {
                    response = client.execute(postForm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int statusCode = response.getStatusLine().getStatusCode();
                // If the response is positive
                if (statusCode == 200) {
                    // Reads the content of the response.
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        InputStream in = null;
                        try {
                            in = responseEntity.getContent();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Map<String, Object> newFormCall = null;
                        try {
                            newFormCall = mapper.readValue(in,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        for (Map.Entry<String, Object> entry : newFormCall.entrySet()) {
                            if (entry.getKey().equals("content") && entry.getValue() instanceof Map) {
                                Map<String, Object> formMap = (Map<String, Object>) entry.getValue();
                                for (Map.Entry<String, Object> formAttribute : formMap.entrySet()) {
                                    if (formAttribute.getKey().equals("id") && (formAttribute.getValue() instanceof String)) {
                                        newCreatedId = formAttribute.getValue().toString();
                                    }
                                }
                            }
                        }

                    }


                    statusNewForm = 0;


                } else {
                    System.out.println("there was a problem in post form: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                }

            }


            postForm.releaseConnection();

            HttpResponse response2 = null;
            String data = new String();
            data = data + "{\n" +
                    "   \"questions\":[\n";

            // Creates the Http PUT request to create the questions in the new form(s).
            HttpPut putQuestionsToForm = new HttpPut(ConfigurationManager.getInstance().getHttpPostForm() + "/" + newCreatedId + "/questions?apiKey=" + ConfigurationManager.getInstance().getApiKey());
            System.out.println(putQuestionsToForm.getURI());


            // Retrieves all the questions from the individual forms of tests and tasks
            for (Form form : list) {
                // Gets the questions in the form

                try {

                    // Creates the Http GET request to get the questions of forms.
                    HttpGet getQuestionsOfForm = new HttpGet(ConfigurationManager.getInstance().getHttpGetForm() + "/" + form.getFormId() + "/questions?apiKey=" + key);

                    System.out.println(getQuestionsOfForm.getURI());
                    getQuestionsOfForm.setHeader("Accept", "application/json");

                    int status = 1;

                    // While there is something wrong (e.g. Internet broke or the server was offline) retry.
                    while (status == 1) {
                        response2 = client2.execute(getQuestionsOfForm);

                        // Gets the status line.
                        int statusCode = response2.getStatusLine().getStatusCode();
                        // If everything went OK (status == 200)

                        /**
                         * Sample response:
                         * {
                         "responseCode": 200,
                         "message": "success",
                         "content": {
                         "1": {
                         "hint":" ",
                         "labelAlign":"Auto",
                         "name":"textboxExample1",
                         "order":"1",
                         "qid":"1",
                         "readonly":"No",
                         "required":"No",
                         "shrink": "No",
                         "size":"20",
                         "text":"Textbox Example",
                         "type":"control_textbox",
                         "validation":"None"
                         },
                         "2": {
                         "labelAlign":"Auto",
                         "middle":"No",
                         "name":"fullName2",
                         "order":"1",
                         "prefix":"No",
                         "qid":"2",
                         "readonly":"No",
                         "required":"No",
                         "shrink": "Yes",
                         "sublabels":
                         {
                         "prefix":"Prefix",
                         "first":"First Name",
                         "middle":"Middle Name",
                         "last":"Last Name",
                         "suffix":"Suffix"
                         },
                         "suffix":"No",
                         "text":"Full Name",
                         "type":"control_fullname"
                         },
                         ...}
                         */
                        if (statusCode == 200) {


                            // Gets the entity in the response (body).
                            HttpEntity responseEntity = response2.getEntity();
                            if (responseEntity != null) {
                                ObjectMapper mapper = new ObjectMapper();
                                InputStream in = null;
                                try {
                                    in = responseEntity.getContent();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Map<String, Object> formQuestionsCall = null;
                                try {
                                    formQuestionsCall = mapper.readValue(in,
                                            new TypeReference<Map<String, Object>>() {
                                            });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                                //most outer level
                                for (Map.Entry<String, Object> entry : formQuestionsCall.entrySet()) {
                                    //Gets the questions
                                    //if(entry.getKey().equals("content") && entry.getValue() instanceof List)
                                    if (entry.getKey().equals("content")) {
                                        JsonFactory jsonFactory = new JsonFactory().configure(
                                                JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                                        Map<String, Object> mapOfQuestions = (Map<String, Object>) entry.getValue();
                                        ObjectMapper mapper2 = new ObjectMapper(jsonFactory);


                                        for (Map.Entry<String, Object> questionEntry : mapOfQuestions.entrySet()) {


                                            // Map<String,Object> questionI = (Map<String,Object>)questionEntry.getValue();
                                            // for (Map.Entry<String,Object> questionAttribute: questionI.entrySet())
                                            //{

                                            /** each of shape:
                                             *   "1": {
                                             "hint":" ",
                                             "labelAlign":"Auto",
                                             "name":"textboxExample1",
                                             "order":"1",
                                             "qid":"1",
                                             "readonly":"No",
                                             "required":"No",
                                             "shrink": "No",
                                             "size":"20",
                                             "text":"Textbox Example",
                                             "type":"control_textbox",
                                             "validation":"None"
                                             },
                                             */

                                            String idQuestionInMap = (String) questionEntry.getKey();

                                            Map<String, Object> questionAttributeMap = (Map<String, Object>) questionEntry.getValue();

                                            putQuestionsToForm.setHeader("Content-type", "application/json");

                                            /* Makes the qid of each question unique, by using the id of the new form created, together with the position in the current list of forms.
                                            * There will be several question with the same ID.
                                            * each qid=1 of each individual form will be aggregated into the global one).
                                             */
                                            //Integer index = list.indexOf(form);

                                            String newId = form.getFormId() +"-" + questionAttributeMap.get("qid");
                                            questionAttributeMap.put("qid", newId);

                                            /**
                                             * Makes the order of each question unique, for the same reason as for the qid.
                                             */
                                            if (list.indexOf(form) > 0) {
                                                String originalOrder = (String) questionAttributeMap.get("order");
                                                Integer orderInt = new Integer(originalOrder);
                                                Integer newOrder = new Integer(orderInt.intValue() + this.countCurrentOrder);
                                                questionAttributeMap.put("order", newOrder.toString());
                                            }


                                            data = data + mapper2.writeValueAsString(questionAttributeMap);

                                            data = data + ", ";
                                            //For the next one


                                        }
                                        this.countCurrentOrder = this.countCurrentOrder + mapOfQuestions.size();


                                    }

                                }


                            }
                            status = 0;
                        } else {
                            System.out.println("there was a problem in response 2 : " + response2.getStatusLine().getStatusCode() + " " + response2.getStatusLine().getReasonPhrase());
                        }

                        getQuestionsOfForm.releaseConnection();

                        putQuestionsToForm.releaseConnection();


                    }

                } catch (Exception e) {
                    e.printStackTrace();


                }


                // Adds the retrieved questions to the list of questions
            }
            if (data.endsWith(", ")) {
                String dataTemp = new String(data);
                data = new String(dataTemp.substring(0, dataTemp.length() - 2));
            }
            data = data + "\n ]";
            data = data + "\n }";

            StringEntity sEntE = new StringEntity(data, ContentType.create(
                    "application/json", "UTF-8"));
            System.out.println("data: " + data);
            putQuestionsToForm.setEntity(sEntE);

            int statusPut = 1;
            // Creates the form in JotForm including all the questions of the individual forms

            while (statusPut == 1) {
                HttpResponse response3 = null;
                try {
                    response3 = client3.execute(putQuestionsToForm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int statusPutQuestionsForm = response3.getStatusLine().getStatusCode();
                if (statusPutQuestionsForm == 200) {
                    statusPut = 0;
                } else {
                    System.out.print("there was a problem in put form: " + response3.getStatusLine().getStatusCode() + " " + response3.getStatusLine().getStatusCode() + " " + response3.getStatusLine().getReasonPhrase());
                }
            }


        }
    }

    /**
     * Generates a file containing a set of tests and tasks to be accomplished by each user.
     * The file is a CSV file. Each row has shape:  "userid", "form1", ..., "formn" .
     * form1 ... formn will be any of the tests and tasks designed in JotForm. At the beginning there is always the instructions, 
     * @param numberOfUsers the number of total users that will participate in the experiment.
     * @param numberOfSessions the number of sessions in which the experiment should be split.
     */
    public void generateShuffledLists(int numberOfUsers, int numberOfSessions) throws FileNotFoundException {

        List<Form> frontForms = new ArrayList<Form>();
        List<Form> middleForms = new ArrayList<Form>();
        List<Form> endForms = new ArrayList<Form>();


        // generate lists of shuffled forms
        this.createSetOfShuffledLists(numberOfUsers);

        // create a copy of the generated set of shuffled lists
        Set<List<Form>> tempShuffledSet = new HashSet<List<Form>>();

        // go through all the content of the set of shuffled lists and include  instructions in the front and a break inbetween
        File frontFile = new File(workingDirForFileName+"/data/front.csv");
        this.readForms(frontFile, frontForms, TypeOfForm.Other);

        File middleFile = new File(workingDirForFileName+"/data/middle.csv");
        this.readForms(middleFile, middleForms, TypeOfForm.Other);

        File endFile = new File(workingDirForFileName+"/data/end.csv");
        this.readForms(endFile, endForms, TypeOfForm.Other);




        for (List<Form> listFi: this.setOfShuffledLists)
        {
            List<Form> newList = new ArrayList<Form>();
           // add the front forms at the beginning of the list
           for(Form form: frontForms)
           {
               newList.add(form);
           }
           int sizeOfSession = Math.round(listFi.size() / numberOfSessions);
           int sizeNoBreak = Math.round(sizeOfSession / 2);
           Iterator listFiIt =  listFi.iterator();
            int currentSession = 1;
            int count = 0;
           while(listFiIt.hasNext())
           {

               newList.add((Form)listFiIt.next());
               count++;
               if(count==(((currentSession-1)*sizeOfSession)+sizeNoBreak))
               {
                   // only one form for break
                   newList.add(middleForms.get(0));
               }
               else if(count==currentSession*sizeOfSession)
               {
                   // only one form for the end of session
                   newList.add(endForms.get(0));
                   currentSession++;
               }
           }

           //add it to the temp set of shuffled lists
            tempShuffledSet.add(newList);
        }


        // remove the previous content of the set of shuffledlists and replace it with the updated content (including instructions and breaks)
        this.setOfShuffledLists.clear();
        this.setOfShuffledLists.addAll(tempShuffledSet);


        this.writeShuffledResult();

    }

    private void writeShuffledResult()
    {

        String defaultJotFormURLBase = "http://form.jotformpro.com/form/";
        File f = new File(workingDirForFileName+"/output/listOfShuffledTestsAndTasks.csv");

        try {
            Files.write("Forms for users", f,
                    Charset.defaultCharset());
            String ls = System.getProperty("line.separator");
            Files.append(ls, f, Charset.defaultCharset());

            int count =0;
           for(List<Form> listOfOneUser: this.setOfShuffledLists)
           {
               //write one list of shuffled lists per line - one line is one user
               count++;
               Files.append("'user"+count+"',", f, Charset.defaultCharset());
               for(Form userFormI: listOfOneUser)
               {
                   Files.append("'"+defaultJotFormURLBase+userFormI.getFormId()+"',", f, Charset.defaultCharset());
               }
               //new line
               Files.append(ls, f, Charset.defaultCharset());

           }




        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
