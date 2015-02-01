import org.sensorhub.impl.SensorHub;



public class TestSensorHubAxisSosSps
{

    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"src/test/resources/config_axis_sos_sps.json", "storage"});
    }

}
