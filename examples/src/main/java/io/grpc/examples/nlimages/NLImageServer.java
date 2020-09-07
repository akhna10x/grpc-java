/*
 * Neuralink image server.
 */
 
package io.grpc.examples.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.awt.Graphics2D;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
import java.awt.image.WritableRaster;
import javax.swing.*;
import java.awt.*;
import java.awt.image.DataBufferByte;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.AffineTransform;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.AlphaComposite;

import javax.swing.*;
import java.awt.*;

/**
 * Neuralink image service server.
 */
public class NLImageServer {
  private static final Logger logger = Logger.getLogger(NLImageServer.class.getName());

  private Server server;

  private void start() throws IOException {
    int port = 50051;
    server = ServerBuilder.forPort(port)
        .addService(new NLImageServiceImpl())
        .build()
        .start();
    logger.info("Listening on " + port);
    NLImage nlImage =  NLImage.newBuilder().build();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
           NLImageServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Blocks until server is shutdown.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final NLImageServer server = new NLImageServer();
    server.start();
    server.blockUntilShutdown();
  }


  // NLImageService
  static class NLImageServiceImpl extends NLImageServiceGrpc.NLImageServiceImplBase  {
    
    public static ImageIcon getImageFromArray(byte[] bytes, int width, int height) {
      // TODO: verify conversion  
      int[] pixels = new int[bytes.length];
      // ALt just send file byes
      for (int i = 0; i < bytes.length; ++i) {
        pixels[i] = bytes[i];
      }
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      WritableRaster raster = (WritableRaster) image.getData();
      raster.setPixels(0,0,width,height,pixels);
      return new ImageIcon(image);
    }

    public static BufferedImage convertToBufferedImage(
            java.awt.Image image) {
        BufferedImage newImage = new BufferedImage(
            image.getWidth(null), image.getHeight(null),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    public ImageIcon createImage(byte[] b, int width, int height) {
        ImageIcon icon = new ImageIcon(b);
        java.awt.Image rawImage = icon.getImage();
        BufferedImage image = convertToBufferedImage(rawImage);
        icon.setImage(createFlipped(image));
        return icon;
    }

    public ImageIcon createWatermarkImage(byte[] b, int width, int height) {
      ImageIcon icon = new ImageIcon(b);
      java.awt.Image rawImage = icon.getImage();
      BufferedImage image = convertToBufferedImage(rawImage);
      // icon.setImage(createFlipped(image));
      icon.setImage(createWatermarked(image));
      return icon;
  }
  

    public static GraphicsConfiguration getDefaultConfiguration() {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gd = ge.getDefaultScreenDevice();
      return gd.getDefaultConfiguration();
    }
    
    public static BufferedImage rotate(BufferedImage image, double angle) {
        int w = image.getWidth(), h = image.getHeight();
        GraphicsConfiguration gc = getDefaultConfiguration();
        BufferedImage result = gc.createCompatibleImage(w, h);
        Graphics2D g = result.createGraphics();
        g.rotate(Math.toRadians(angle), w / 2, h / 2);
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }

    static BufferedImage addImageWatermark(BufferedImage sourceImage) {
      int w = sourceImage.getWidth();
      int h = sourceImage.getHeight();
      GraphicsConfiguration gc = getDefaultConfiguration();
      BufferedImage result = gc.createCompatibleImage(w, h);
      try {
          // BufferedImage sourceImage = ImageIO.read(sourceImageFile);
          // File sourceImageFile
          String filename = "watermark.png";
          BufferedImage watermarkImage = ImageIO.read(new File("watermark.png"));
          
          
          Graphics2D g = result.createGraphics();
          Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
          AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
          g2d.setComposite(alphaChannel);
          int topLeftX = (sourceImage.getWidth() - watermarkImage.getWidth()) / 2;
          int topLeftY = (sourceImage.getHeight() - watermarkImage.getHeight()) / 2;
          g2d.drawImage(watermarkImage, topLeftX, topLeftY, null);
          System.out.println("The image watermark is added to the image.");
   
          g.drawRenderedImage(watermarkImage, null);
          g.dispose();
          
      } catch (IOException ex) {
          System.err.println(ex);
      }
      return result;
  }

    private static BufferedImage createFlipped(BufferedImage image){
      return rotate(image, 180.0);
    }
    private static BufferedImage createWatermarked(BufferedImage image){
      return rotate(image, 180.0);
    }

      @Override
      public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
        logger.info("Runng RotateImage() impl...."); 
        // TODO handle error
        // TODO: return valid response
        // int TMP_SIZE = 70;
        NLImage reqImg = req.getImage();
        int height = reqImg.getHeight();
        int width = reqImg.getWidth();
        byte[] rotatedImg = new byte[width * height];
        ByteString reqImgBytes = reqImg.getData();
        logger.info("__ reqImgBytes.size(): " + reqImgBytes.size());
        byte[] bytes = reqImgBytes.toByteArray();
        // ImageIcon icon = createImage(bytes, width, height);
        // displayResponse(icon);
        ImageIcon icon = createWatermarkImage(bytes, width, height);
        displayResponse(icon);
        logger.info("displayResponse");
        ByteString responseData = reqImgBytes;

        /**
         * Sends reply back.
         */ 
        NLImage reply = NLImage.newBuilder()
            .setWidth(width)
            .setHeight(height)
            // .setData()
            .build();
            // TODO: set data
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }

      private void displayResponse(ImageIcon icon) {
        // TODO: remove this method
        JFrame frame = new JFrame();
        JLabel label = new JLabel(icon);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
      }

      private void displayResponse(BufferedImage bufferedImage) {
        // TODO: remove this method
        JFrame frame = new JFrame();
        ImageIcon icon = new ImageIcon(bufferedImage);
        JLabel label = new JLabel(icon);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
      }
    

      private ImageIcon bytesToIcon() {
        return null;
      }

      private byte[] iconToBytes() {
        return null;
      }
      
      @Override 
      public void customImageEndpoint(NLCustomImageEndpointRequest req, 
                                      StreamObserver<NLCustomImageEndpointResponse> responseObserver) {
        // TODO: embed Neuralink logo
        logger.info("Running customImageEndpoint() ...");
      }
  }
}

