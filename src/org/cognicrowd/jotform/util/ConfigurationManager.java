package org.cognicrowd.jotform.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
/**
 * @author csarasua
 */
public class ConfigurationManager {

    private Configuration config;

    static private ConfigurationManager singleton;

    private ConfigurationManager() {

        try {

            String workingDir = System.getProperty("user.dir");
            File f = new File(workingDir + "/config.properties");
            config = new PropertiesConfiguration(f);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static synchronized ConfigurationManager getInstance() {

        if (singleton == null) {

            singleton = new ConfigurationManager();

        }
        return singleton;
    }

    public String getApiKey(){return config.getString("api.key");}


    public String getHttpGetForm(){return config.getString("api.getform");}
    public String getHttpPostForm(){return config.getString("api.postform");}
   /* public void setXX(XXX) {

            this.config.setProperty("xxx.yyy", xxx);

        }

    }*/

}
