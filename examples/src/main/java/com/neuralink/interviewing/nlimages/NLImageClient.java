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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.image.DataBufferByte;
import javax.swing.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
 
/**
 * Client for managing requests to the Neuralink image service.
 */
public class NLImageClient {
  private static final Logger logger = 
      Logger.getLogger(NLImageClient.class.getName());
  
  private final NLImageServiceGrpc.NLImageServiceBlockingStub blockingStub;

  /** 
   * Creates an NLImageClient using the provided channel.
   **/
  public NLImageClient(Channel channel) {
    blockingStub = NLImageServiceGrpc.newBlockingStub(channel);
  }

  /** 
   * Requests an image be rotated.
   */
  public void requestRotate(String filename, boolean isColor, 
      NLImageRotateRequest.Rotation rotation) throws IOException{
    byte[] bytes = readImgFile(filename);
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
    NLImage nlImage = NLImage.newBuilder()
        .setColor(isColor)
        .setData(ByteString.copyFrom(bytes))
        .setWidth(img.getWidth())
        .setHeight(img.getHeight())
        .build();
    NLImageRotateRequest request = NLImageRotateRequest.newBuilder()
        .setImage(nlImage)
        .setRotation(rotation)
        .build();
    System.out.println("bImageFromConvert created...");
    System.out.println("Old height..." + img.getHeight());
    System.out.println("bImageFromConvert height..." + bImageFromConvert.getHeight());

    NLImage response;
    try {
      response = blockingStub.rotateImage(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    ByteString respImgBytes = response.getData();
    byte[] rBytes = response.getData().toByteArray();
    ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight());
    displayResponse(icon);
  }

  public void requestWatermark(String filename, boolean isColor) 
    throws IOException{
      byte[] bytes = readImgFile(filename);
      BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
      NLImage nlImage = NLImage.newBuilder()
          .setColor(isColor)
          .setData(ByteString.copyFrom(bytes))
          .setWidth(img.getWidth())
          .setHeight(img.getHeight())
          .build();
      NLCustomImageEndpointRequest request = NLCustomImageEndpointRequest.newBuilder()
          .setImage(nlImage)
          .build();
  
      NLImage response;
      try {
        response = blockingStub.customImageEndpoint(request).getImage();
        ByteString respImgBytes = response.getData();
        byte[] rBytes = response.getData().toByteArray();
        ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight());
        displayResponse(icon);
      } catch (StatusRuntimeException e) {
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        return;
      }
  }

  private byte[] readImgFile(String filename) throws IOException {
    FileInputStream fis = new FileInputStream(filename);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final int maxBufferSize = 1024;
    byte[] buf = new byte[maxBufferSize];
    try {
        for (int readNum; (readNum = fis.read(buf)) != -1;) {
            bos.write(buf, 0, readNum); 
        }
    } catch (IOException e) {
        logger.info("IOException: " + e.getMessage());
    }
    byte[] bytes = bos.toByteArray();
    return bytes;
  }

  private ImageIcon createRespImage(byte[] b, int width, int height) {
    ImageIcon icon = new ImageIcon(b);
    return icon;
  }

  private void displayResponse(ImageIcon icon) {
    JFrame frame = new JFrame();
    JLabel label = new JLabel(icon);
    frame.add(label);
    frame.pack();
    frame.setVisible(true);
  }

  private static class Option {
    String flag, opt;
    public Option(String flag, String opt) { 
        this.flag = flag; 
        this.opt = opt; 
    }
  }

  private static Map<String, Option> parseArgs(String[] args) {
    Map<String, Option> optsSet = new HashMap<>();
    List<Option> optsList = new ArrayList<Option>();
    List<String> doubleOptsList = new ArrayList<String>();
    List<String> argsList = new ArrayList<String>();  

    for (int i = 0; i < args.length; i++) {
      switch (args[i].charAt(0)) {
      case '-':
          if (args[i].length() < 2) {
              throw new IllegalArgumentException("Not a valid argument: " + args[i]);
          }
          if (args[i].length() < 3) {
              throw new IllegalArgumentException("Not a valid argument: "+args[i]);
          }
          String name = args[i].replace("-", "");
              doubleOptsList.add(args[i].substring(2, args[i].length()));
              if (args.length-1 == i) {
                  throw new IllegalArgumentException("Expected arg after: "+args[i]);
              }
              optsList.add(new Option(args[i], args[i+1]));
              optsSet.put(name, new Option(name, args[i+1]));
              i++;
          break;
      default:
          argsList.add(args[i]);
          break;
      }
    }

    return optsSet;
  }
  /**
   * Runs Neuralink image client.
   */
  public static void main(String[] args) throws Exception {
    final String customEndpoint = "watermark";
    String target = "localhost:9090";
    Map<String, Option> optsSet = parseArgs(args);
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: target [mode] [filename]");
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
    }
    if (args.length > 1) {
      target = args[1];
    }
    boolean watermarkEndpoint = false;
    Option endpoint = optsSet.get("endpoint");
    if (endpoint != null && 
        endpoint.opt.equals(customEndpoint)) {
    }
    boolean grayscaleImg = false;
    if (args.length > 2 && args[2].equals(customEndpoint)) {
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
          System.out.println("Args rotate: " +
              optsSet.get("rotate"));
          NLImageRotateRequest.Rotation rotation = 
              NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG;
          String rotationArg = optsSet.get("rotate").opt;
          if (rotationArg.equals("90")) {
              rotation = NLImageRotateRequest.Rotation.NINETY_DEG;
          } else if (rotationArg.equals("180")) {
             rotation = NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG;
          } else if (rotationArg.equals("270")) {
            rotation = NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG;
          } else if (rotationArg.equals("0")) {
            rotation = NLImageRotateRequest.Rotation.NONE;
          } else {
            System.err.println("Invalid routation specified. Exiting.");
            return;
          }

          client.requestRotate(filename, isColor, rotation); 
        }
      } finally {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
  }
}
