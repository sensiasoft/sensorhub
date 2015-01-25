import org.sensorhub.impl.SensorHub;



public class TestSensorHubSosT
{

    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"src/test/resources/config_empty_sost.json", "storage"});
    }

}
