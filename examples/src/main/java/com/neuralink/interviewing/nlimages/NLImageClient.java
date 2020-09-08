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
    FileInputStream fis = new FileInputStream(filename);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
    BufferedImage img = ImageIO.read(new File(filename));
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    try {
        for (int readNum; (readNum = fis.read(buf)) != -1;) {
            bos.write(buf, 0, readNum); 
        }
    } catch (IOException e) {
        logger.info("IOException: " + e.getMessage());
    }
    byte[] bytes = bos.toByteArray();
    int width = img.getWidth();
    int height = img.getHeight();
    NLImage nlImage = NLImage.newBuilder()
        .setColor(true) // TODO: set based on cmdline
        .setData(ByteString.copyFrom(bytes))
        .setWidth(width)
        .setHeight(height)
        .build();
    NLImageRotateRequest request = NLImageRotateRequest.newBuilder()
        .setImage(nlImage)
        .build();

    NLImage response;
    try {
      // response = blockingStub.rotateImage(request);
      response = blockingStub.rotateImage(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    ByteString respImgBytes = response.getData();
    logger.info("__ responseImgBytes.size(): " + respImgBytes.size());
    byte[] rBytes = respImgBytes.toByteArray();
    ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight() );
    displayResponse(icon);
  }

   
  public void requestWatermark(String filename, boolean isColor) 
    throws IOException{
      FileInputStream fis = new FileInputStream(filename);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
      BufferedImage img = ImageIO.read(new File(filename));
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      try {
          for (int readNum; (readNum = fis.read(buf)) != -1;) {
              bos.write(buf, 0, readNum); 
          }
      } catch (IOException e) {
          logger.info("IOException: " + e.getMessage());
      }
      byte[] bytes = bos.toByteArray();
      int width = img.getWidth();
      int height = img.getHeight();
      NLImage nlImage = NLImage.newBuilder()
          .setColor(true) // TODO: set based on cmdline
          .setData(ByteString.copyFrom(bytes))
          .setWidth(width)
          .setHeight(height)
          .build();
      NLCustomImageEndpointRequest request = NLCustomImageEndpointRequest.newBuilder()
          .setImage(nlImage)
          .build();
  
      NLImage response;
      try {
        // response = 
        blockingStub.customImageEndpoint(request);
      } catch (StatusRuntimeException e) {
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        return;
      }
      // ByteString respImgBytes = response.getData();
      // logger.info("__ responseImgBytes.size(): " + respImgBytes.size());
      // byte[] rBytes = respImgBytes.toByteArray();
      // ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight() );
      // displayResponse(icon);

  }

  private ImageIcon createRespImage(byte[] b, int width, int height) {
    ImageIcon icon = new ImageIcon(b);
    // java.awt.Image rawImage = icon.getImage();
    // BufferedImage image = convertToBufferedImage(rawImage);
    return icon;
  }



  /**
   * Runs Neuralink image client.
   */
  public static void main(String[] args) throws Exception {
    final String customEndpoint = "watermark";
    String target = "localhost:9090";
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
