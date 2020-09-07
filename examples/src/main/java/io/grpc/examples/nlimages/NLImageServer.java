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
      // @nxt: __ 

      // ALt just send file byes
      for (int i = 0; i < bytes.length; ++i) {
        pixels[i] = bytes[i];
      }

      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      WritableRaster raster = (WritableRaster) image.getData();
      raster.setPixels(0,0,width,height,pixels);

      return new ImageIcon(image);
    }

    public ImageIcon createImage(byte[] b){
      // byte[] b = {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82,
      //    0, 0, 0, 15, 0, 0, 0, 15, 8, 6, 0, 0, 0, 59, -42, -107,
      //    74, 0, 0, 0, 64, 73, 68, 65, 84, 120, -38, 99, 96, -64, 14, -2,
      //    99, -63, 68, 1, 100, -59, -1, -79, -120, 17, -44, -8, 31, -121, 28, 81,
      //    26, -1, -29, 113, 13, 78, -51, 100, -125, -1, -108, 24, 64, 86, -24, -30,
      //    11, 101, -6, -37, 76, -106, -97, 25, 104, 17, 96, -76, 77, 97, 20, -89,
      //    109, -110, 114, 21, 0, -82, -127, 56, -56, 56, 76, -17, -42, 0, 0, 0,
      //    0, 73, 69, 78, 68, -82, 66, 96, -126};
      return new ImageIcon(b);
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
        ImageIcon icon = createImage(bytes);
        displayResponse(icon);
        logger.info("displayResponse");

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

