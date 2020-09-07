/*
 * Neuralink image server.
 */
 
package io.grpc.examples.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
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

    public ImageIcon createImage(byte[] b, int width, int height) {
        ImageIcon icon = new ImageIcon(b);
        java.awt.Image rawImage = icon.getImage();

        // BufferedImage image = new  BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // WritableRaster raster = (WritableRaster) image.getData();
        // raster.setPixels(0,0,width,height,pixels);
        // icon.setImage(createFlipped(new BufferedImage(icon.getImage())));
        return icon;
    }

    private static BufferedImage createTransformed(BufferedImage image, AffineTransform at) {
        BufferedImage newImage = new BufferedImage(
            image.getWidth(), image.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.transform(at);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private static BufferedImage createFlipped(BufferedImage image){
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(1, -1));
        at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        return createTransformed(image, at);
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
        ImageIcon icon = createImage(bytes, width, height);
        displayResponse(icon);
        logger.info("displayResponse");
        ByteString responseData = reqImgBytes;

        /**
         * Reads bytes back to img object.
         */
        // try {
        //   byte[] bytes = reqImgBytes.toByteArray();

        //   ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        //   // Iterator<?> readers = ImageIO.getImageReadersByFormatName("png");
        //   Iterator<?> readers = ImageIO.getImageReadersByFormatName("bmp");
        //   ImageReader reader = (ImageReader) readers.next();
        //   Object source = bis; 
        //   ImageInputStream iis = ImageIO.createImageInputStream(source); 
        //   reader.setInput(iis, true);
        //   ImageReadParam param = reader.getDefaultReadParam();
        //   java.awt.Image image = reader.read(0, param);
        //   BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        //   //bufferedImage is the RenderedImage to be written
        //   Graphics2D g2 = bufferedImage.createGraphics();
        //   g2.drawImage(image, null, null);
        //   System.out.println("g2.drawImage() img drawn...");
        //   displayResponse(bufferedImage);
        // } catch(IOException e) {
        //   logger.info("IOException: " + e.getMessage());
        // }

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

