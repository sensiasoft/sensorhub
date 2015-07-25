import java.net.URL;
import org.sensorhub.impl.SensorHub;
import org.vast.sensorML.ProcessLoader;
import org.vast.sensorML.test.TestSMLProcessing;


public class SensorHubDemos
{

    public static void main(String[] args) throws Exception
    {
        //URL processMapUrl = TestSMLProcessing.class.getResource("ProcessMap.xml");
        //ProcessLoader.loadMaps(processMapUrl.toString(), false);
        
        SensorHub.main(new String[] {"src/test/resources/config_android_trupulse_process.json", "storage"});
    }

}
