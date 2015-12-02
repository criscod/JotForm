import org.cognicrowd.jotform.formgeneration.FormManager;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author csarasua
 */
public class Main {

    // Main directory of the code.
    static String workingDir = System.getProperty("user.dir");
    static String workingDirForFileName = workingDir.replace("\\", "/");

    public static void main(String args[]) {

      // composeShuffledVersionOfTestsAndTasks();
        generateShuffledListsOfForms();


    }

    private static void composeShuffledVersionOfTestsAndTasks()
    {

        File testsFile = new File(workingDirForFileName+"/data/testsFile.csv");
        File tasksFile = new File(workingDirForFileName+"/data/tasksFile.csv");

        FormManager fm = new FormManager(testsFile, tasksFile);
        fm.createSetOfShuffledLists(2);
        fm.serialiseSetOfShuffledLists(1);

    }

    private static void generateShuffledListsOfForms()
    {
        File testsFile = new File(workingDirForFileName+"/data/testsFile.csv");
        File tasksFile = new File(workingDirForFileName+"/data/tasksFile.csv");

        FormManager fm = new FormManager(testsFile, tasksFile);
        try {
            fm.generateShuffledLists(80,3);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}


