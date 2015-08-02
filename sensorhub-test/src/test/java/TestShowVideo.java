import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420pToRgb;
import org.vast.cdm.common.DataStreamParser;
import org.vast.data.DataBlockMixed;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.GetResultTemplateRequest;
import org.vast.ows.sos.GetResultTemplateResponse;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;


/**
 * <p>
 * This test connects to SOS server and gets video feed;
 * It then tries to decode and display the video in a JFrame.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 26, 2015
 */
public class TestShowVideo
{

    public static void main(String[] args) throws Exception
    {
        final OWSUtils utils = new OWSUtils();
        String endPoint = "http://localhost:8181/sensorhub/sos";
        //String offering = "urn:android:device:060693280a28e015-sos";
        String offering = "urn:offering:cam";
        String obsProp = "http://sensorml.com/ont/swe/property/VideoFrame";        
        
        // GetResultTemplate
        GetResultTemplateRequest gtr = new GetResultTemplateRequest();
        gtr.setGetServer(endPoint);
        gtr.setOffering(offering);
        gtr.getObservables().add(obsProp);
        gtr.setVersion("2.0");       
        URLConnection conn = utils.sendGetRequest(gtr);
        final GetResultTemplateResponse grtResp = (GetResultTemplateResponse)utils.readXMLResponse(conn.getInputStream(), OWSUtils.SOS, "GetResultTemplateResponse");
        final DataArray imgArray;
        if (grtResp.getResultStructure() instanceof DataArray)
            imgArray = (DataArray)grtResp.getResultStructure();
        else
            imgArray = (DataArray)grtResp.getResultStructure().getComponent("videoFrame");
        final int height = imgArray.getComponentCount();
        final int width = imgArray.getElementType().getComponentCount();
        utils.writeXMLResponse(System.out, grtResp);
        
        // get video data, parse it and display it
        final GetResultRequest gt = new GetResultRequest();
        gt.setGetServer(endPoint);
        gt.setOffering(offering);
        gt.getObservables().add(obsProp);
        gt.setTime(TimeExtent.getPeriodStartingNow(System.currentTimeMillis()/1000. + 3600.));
        gt.setVersion("2.0");
        
        Runnable job = new Runnable() {
            
            public void run()
            {
                try
                {
                    boolean decode = false;
                    
                    // prepare decoder
                    /*VideoFormat inputFormat = new VideoFormat("MP42", new Dimension(width, height), -1, byte[].class, -1);
                    VideoDecoder h264Decoder = new VideoDecoder();
                    h264Decoder.setInputFormat(inputFormat);
                    h264Decoder.setOutputFormat(new RGBFormat());   
                    Buffer inputBuf = new Buffer();
                    Buffer outputBuf = new Buffer();*/
                    
                    H264Decoder h264Decoder = new H264Decoder();
                    Transform transform = new Yuv420pToRgb(0, 0);
                    Picture yuv = Picture.create((width + 15) & ~0xf, (height + 15) & ~0xf, ColorSpace.YUV420);
                    Picture rgb = Picture.create(width, height, ColorSpace.RGB);
                    ByteBuffer inputBuf = ByteBuffer.allocate(256);
                                        
                    // give SPS header to decoder                    
                    String header = "00 00 00 01 67 42 80 1F DA 03 20 4D F9 60 1B 42 84 D4 00 00 00 01 68 CE 06 E2";
                    for (String val: header.split(" ")) {
                        int b = Integer.parseInt(val, 16);
                        inputBuf.put((byte)b);
                    }
                    
                    // open window
                    JFrame f = new JFrame("Video");
                    f.getContentPane().setSize(width, height);
                    f.setLocation(100, 100);
                    f.setVisible(true);
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                    
                    // we need to sync here because instantiation of GetResultWriter is not threadsafe
                    // probably because of FES.getInstance() call
                    InputStream is = null;
                    synchronized(utils)
                    {
                        System.out.println(utils.buildURLQuery(gt));
                        HttpURLConnection conn = utils.sendGetRequest(gt);
                        is = new BufferedInputStream(conn.getInputStream());
                        //OGCExceptionReader.parseException(is);
                    }
                    
                    DataStreamParser parser = SWEHelper.createDataParser(grtResp.getResultEncoding());
                    parser.setDataComponents(grtResp.getResultStructure());
                    parser.setRenewDataBlock(false);
                    parser.setInput(is);
                                 
                    DataBlock data;
                    WritableRaster raster = null;
                    long t0 = System.currentTimeMillis();
                    while ((data = parser.parseNextBlock()) != null)
                    {
                        byte[] decodedData = null;
                        DataBlock imgData;
                        if (data instanceof DataBlockMixed)
                            imgData = ((DataBlock[])data.getUnderlyingObject())[1];
                        else
                            imgData = data;
                        
                        // decode frame
                        if (decode)
                        {
                            // decode with jmf
                            /*inputBuf.setData(encodedData);
                            inputBuf.setLength(encodedData.length);
                            h264Decoder.process(inputBuf, outputBuf);
                            decodedData = (byte[])outputBuf.getData();*/
                            
                            // decode with jcodec
                            byte[] encodedData = (byte[])imgData.getUnderlyingObject();
                            inputBuf.put(encodedData);
                            inputBuf.flip();
                            Picture dec = h264Decoder.decodeFrame(inputBuf, yuv.getData());
                            inputBuf.rewind();
                            transform.transform(dec, rgb);
                            AWTUtil.toBufferedImage(rgb, img);
                        }
                        else
                        {
                            decodedData = (byte[])imgData.getUnderlyingObject();
                        
                            if (raster == null)
                            {
                                DataBufferByte buf = new DataBufferByte(decodedData, width);                            
                                raster = Raster.createWritableRaster(img.getSampleModel(), buf, new Point(0,0));
                                img = new BufferedImage(img.getColorModel(), raster, false, null);
                            }
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
                        
                        // draw image in JFrame
                        f.getContentPane().getGraphics().drawImage(img, 0, 0, null);
                        long t1 = System.currentTimeMillis();
                        System.out.println(t1 - t0);
                        t0 = t1;
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
        //threadPool.execute(job);
    }

}
