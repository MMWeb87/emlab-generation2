package emlab.gen.scenarios;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ScenarioPropertiesReader {
	
	String result = "";
	InputStream inputStream;
	Properties prop = new Properties();
 
	public String loadProperties(String scenarioName) {
 
		try {
			
			String propFileName = "scenarios/" + scenarioName + ".properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
 
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public String get(String propertyName){
		return prop.getProperty(propertyName);
	}
	
	public Boolean getAsBoolean(String propertyName){
		return Boolean.valueOf(prop.getProperty(propertyName));
	}
	
	public double getAsDouble(String propertyName){
		return Double.valueOf(prop.getProperty(propertyName));
	}
	
	public long getAsLong(String propertyName){
		return Long.valueOf(prop.getProperty(propertyName));
	}


}