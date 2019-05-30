package helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import models.DataModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;


public class ApiHelper {

    public static DataModel dataModel;
    private static Gson gson;
    Properties prop = new Properties();

    public ApiHelper(DataModel dataModel){
        this.dataModel = dataModel;
    }

    public static Gson gson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        // if uncommented will also create Json for fields which are null
        //   gsonBuilder.serializeNulls();
        gson = gson(gsonBuilder);
        return gson;
    }

    public static Gson gson(GsonBuilder gsonBuilder) {
        gson = gsonBuilder.create();
        return gson;
    }

    public String getEnvProperties(String key) throws IOException {
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("properties/"+Env.get()+".properties");
        Reader reader = new InputStreamReader(input, "UTF-8");
        prop.load(reader);
        return prop.getProperty(key);
    }
}