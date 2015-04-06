import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.sensorhub.impl.SensorHub;
import org.vast.sensorML.ProcessLoader;
import org.vast.sensorML.test.TestSMLProcessing;


public class TestSensorHub
{

    public static void main(String[] args) throws Exception
    {
        URL processMapUrl = TestSMLProcessing.class.getResource("ProcessMap.xml");
        ProcessLoader.loadMaps(processMapUrl.toString(), false);
        
        LogManager.getLogManager().reset();
        Logger.getLogger("global").setLevel(Level.SEVERE);
        
        //SensorHub.main(new String[] {"src/test/resources/config_fakesensors_with_storage.json", "storage"});
        //SensorHub.main(new String[] {"src/test/resources/config_fakesensors_with_process.json", "storage"});
        SensorHub.main(new String[] {"src/test/resources/config_sat_process.json", "storage"});
    }

}
