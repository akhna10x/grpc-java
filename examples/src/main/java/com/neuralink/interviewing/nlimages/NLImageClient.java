/*
 * Neuralink image client.
 */

package com.neuralink.interviewing.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.DataBufferByte;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
 
/**
 * Client for managing requests to the Neuralink image service.
 */
public class NLImageClient {
  private static final Logger logger = Logger.getLogger(NLImageClient.class.getName());

  private final NLImageServiceGrpc.NLImageServiceBlockingStub blockingStub;

  /** 
   * Creates an NLImageClient using the provided channel.
   **/
  public NLImageClient(Channel channel) {
    blockingStub = NLImageServiceGrpc.newBlockingStub(channel);
  }

  private void displayResponse(ImageIcon icon) {
    // TODO: remove this method
    JFrame frame = new JFrame();
    JLabel label = new JLabel(icon);
    frame.add(label);
    frame.pack();
    frame.setVisible(true);
  }

  /** 
   * Requests an image be rotated.
   */
  public void requestRotate(String filename, boolean isColor) throws IOException{
    BufferedImage bimg = ImageIO.read(new File(filename));
    byte[] byteArray = 
        ((DataBufferByte) bimg.getData().getDataBuffer()).getData();
    int width = bimg.getWidth();
    int height   = bimg.getHeight();
    logger.info("_____Width: " + width);
    logger.info("____Height: " + height);

    /**
     * Reads img object -> bytes.
     */
    FileInputStream fis = new FileInputStream(filename);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
    BufferedImage img = ImageIO.read(new File(filename));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    try {
        for (int readNum; (readNum = fis.read(buf)) != -1;) {
            bos.write(buf, 0, readNum); 
            System.out.println("read " + readNum + " bytes,");
        }
    } catch (IOException e) {
        logger.info("IOException: " + e.getMessage());
    }
    byte[] bytes = bos.toByteArray();

    NLImage nlImage = NLImage.newBuilder()
        .setColor(true)
        .setData(ByteString.copyFrom(bytes))
        .setWidth(width)
        .setHeight(height)
        .build();
    NLImageRotateRequest request = NLImageRotateRequest.newBuilder()
        .setImage(nlImage)
        .build();

    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    Object source = bis; 
    ImageInputStream iis = ImageIO.createImageInputStream(source); 
    Iterator<?> readers = ImageIO.getImageReadersByFormatName("png");
    ImageReader reader = (ImageReader) readers.next();
    
    reader.setInput(iis, true);
    ImageReadParam param = reader.getDefaultReadParam();
    java.awt.Image image = reader.read(0, param);
    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    //bufferedImage is the RenderedImage to be written
    Graphics2D g2 = bufferedImage.createGraphics();
    g2.drawImage(image, null, null);
    System.out.println("g2.drawImage() img drawn...");
    displayResponse(bufferedImage);
      
    NLImage response;
    try {
      response = blockingStub.rotateImage(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Neuralink response color: " + response.getColor());
    logger.info("Neuralink response width : " + response.getWidth());
    logger.info("Neuralink response height : " + response.getHeight());
    // TODO display response
    // logger.info("Displayed response img...");
    // NLImage respImg = response.getImage();
    ByteString respImgBytes = response.getData();
    logger.info("__ responseImgBytes.size(): " + respImgBytes.size());
    byte[] rBytes = respImgBytes.toByteArray();
    ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight() );
    // displayResponse(icon);
    JFrame frame = new JFrame();
    JLabel label = new JLabel(icon);
    frame.add(label);
    frame.pack();
    frame.setVisible(true);
  
   


    // /**
    //  * Reads bytes back to img object.
    //  */
    // ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    // Iterator<?> readers = ImageIO.getImageReadersByFormatName("png");
    // ImageReader reader = (ImageReader) readers.next();
    // Object source = bis; 
    // ImageInputStream iis = ImageIO.createImageInputStream(source); 
    // reader.setInput(iis, true);
    // ImageReadParam param = reader.getDefaultReadParam();
    // java.awt.Image image = reader.read(0, param);
    // BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    // //bufferedImage is the RenderedImage to be written
    // Graphics2D g2 = bufferedImage.createGraphics();
    // g2.drawImage(image, null, null);
    // System.out.println("g2.drawImage() img drawn...");
    // displayResponse(bufferedImage);
  }

  
  public void requestWatermark(String filename, boolean isColor) 
                                      { 
    // throws IOException{

    // displayImg(filename);
    // BufferedImage bimg = ImageIO.read(new File(filename));
    // byte[] byteArray = 
    //     ((DataBufferByte) bimg.getData().getDataBuffer()).getData();
    // int width = bimg.getWidth();
    // int height   = bimg.getHeight();
    // logger.info("_____Width: " + width);
    // logger.info("____Height: " + height);

  }

  private ImageIcon createRespImage(byte[] b, int width, int height) {
    ImageIcon icon = new ImageIcon(b);
    // java.awt.Image rawImage = icon.getImage();
    // BufferedImage image = convertToBufferedImage(rawImage);
    return icon;
  }

  private void displayResponse(BufferedImage bufferedImage) {
    // TODO: implement

    // JFrame frame = new JFrame();
    // ImageIcon icon = new ImageIcon(bufferedImage);
    // JLabel label = new JLabel(icon);
    // frame.add(label);
    // frame.pack();
    // frame.setVisible(true);
  }

  /**
   * Runs Neuralink image client.
   * 
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    final String customEndpoint = "watermark";
    String target = "localhost:9090";
    // Allow passing in the user and target strings as command line arguments
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: [name [mode] [filename]]");
        System.err.println("");
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
    }
    if (args.length > 1) {
      target = args[1];
    }
    boolean watermarkEndpoint = false;
    if (args.length > 1 && args[1] == customEndpoint) {
      watermarkEndpoint = true;
    }

    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        .usePlaintext()
        .build();
    try {
      NLImageClient client = new NLImageClient(channel);
      String filename = "s-result.png";
      boolean isColor = true;
      if (watermarkEndpoint) {
        client.requestWatermark(filename, isColor); 
      } else {
        client.requestRotate(filename, isColor); 
      }
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
