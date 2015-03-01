import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.impl.SensorHub;
import org.vast.cdm.common.DataStreamParser;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.GetResultTemplateRequest;
import org.vast.ows.sos.GetResultTemplateResponse;
import org.vast.swe.SWEHelper;


public class TestSensorHub
{

    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"src/test/resources/config_fakesensors_with_storage.json", "storage"});
        
        Thread.sleep(1000);
        final OWSUtils utils = new OWSUtils();
        
        // GetResultTemplate
        GetResultTemplateRequest gtr = new GetResultTemplateRequest();
        gtr.setGetServer("http://localhost:8080/sensorhub/sos");
        gtr.setOffering("urn:mysos:offering01");
        gtr.getObservables().add("http://sensorml.com/ont/swe/property/VideoFrame");
        gtr.setVersion("2.0");        
        URLConnection conn = utils.sendGetRequest(gtr);
        final GetResultTemplateResponse grtResp = (GetResultTemplateResponse)utils.readXMLResponse(conn.getInputStream(), OWSUtils.SOS, "GetResultTemplateResponse");
        final int height = grtResp.getResultStructure().getComponentCount();
        final int width = ((DataArray)grtResp.getResultStructure()).getElementType().getComponentCount();
        utils.writeXMLResponse(System.out, grtResp);
        
        // get video data, parse it and display it
        final GetResultRequest gt = new GetResultRequest();
        gt.setGetServer("http://localhost:8080/sensorhub/sos");
        gt.setOffering("urn:mysos:offering01");
        gt.getObservables().add("http://sensorml.com/ont/swe/property/VideoFrame");
        gt.setVersion("2.0");
        
        Runnable job = new Runnable() {
            
            public void run()
            {
                try
                {
                    JFrame f = new JFrame("Video");
                    f.setSize(width, height);
                    f.setVisible(true);
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                    
                    // we need to sync here because instantiation of GetResultWriter is not threadsafe
                    // probably because of FES.getInstance() call
                    InputStream is = null;
                    synchronized(utils)
                    {
                        is = new BufferedInputStream(utils.sendGetRequest(gt).getInputStream());
                    }
                    
                    DataStreamParser parser = SWEHelper.createDataParser(grtResp.getResultEncoding());
                    parser.setDataComponents(grtResp.getResultStructure());
                    parser.setRenewDataBlock(false);
                    parser.setInput(is);
                                        
                    DataBlock data;
                    WritableRaster raster = null;
                    //long t0 = System.currentTimeMillis();
                    while ((data = parser.parseNextBlock()) != null)
                    {
                        if (raster == null)
                        {
                            DataBufferByte buf = new DataBufferByte((byte[])data.getUnderlyingObject(), width);
                            raster = Raster.createWritableRaster(img.getSampleModel(), buf, new Point(0,0));
                            img = new BufferedImage(img.getColorModel(), raster, false, null);
                        }
//                        for (int i=0; i<data.getAtomCount(); i+=3)
//                        {
//                            int p = i/3;
//                            int v = p/width;
//                            int u = p%width;
//                            img.getRaster().setSample(u, v, 0, data.getIntValue(i));
//                            img.getRaster().setSample(u, v, 1, data.getIntValue(i+1));
//                            img.getRaster().setSample(u, v, 2, data.getIntValue(i+2));
//                        }          
                        f.getContentPane().getGraphics().drawImage(img, 0, 0, null);
                        //long t1 = System.currentTimeMillis();
                        //System.out.println(t1 - t0);
                        //t0 = t1;
                        //System.out.println(Runtime.getRuntime().freeMemory());
                    }
                    
                    f.setVisible(false);
                    f.dispose();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        threadPool.execute(job);
        threadPool.execute(job);
    }

}
