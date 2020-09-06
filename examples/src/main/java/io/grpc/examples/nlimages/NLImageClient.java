/*
 * Neuralink image client.
 */

package io.grpc.examples.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;

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

  private void displayImg(String filename) {
    JFrame frame = new JFrame();
    ImageIcon icon = new ImageIcon(filename);
    JLabel label = new JLabel(icon);
    frame.add(label);
    frame.pack();
    frame.setVisible(true);
  }

  // TODO: remove filename parameter
  private ImageIcon getImgFromResponse(String filename) {
    ImageIcon icon = new ImageIcon(filename);
    // ImageIcon icon = new ImageIcon(filename);
    return icon;
  }

  /** 
   * Requests an image be rotated.
   */
  public void requestRotate(String name) throws IOException{
    String filename = "androidBook.jpg";
    displayImg(filename);
    byte[] byteArray = new byte[8]; // TODO: set based on img
    BufferedImage bimg = ImageIO.read(new File(filename));
    int width = bimg.getWidth();
    int height   = bimg.getHeight();
    logger.info("_____Width: " + width);
    logger.info("____Height: " + height);
    NLImage nlImage = NLImage.newBuilder()
        .setColor(true)
        .setData(ByteString.copyFrom(byteArray))
        .setWidth(width)
        .setHeight(height)
        .build();
 
    logger.info("Created NL object");
    NLImageRotateRequest request = NLImageRotateRequest.newBuilder().build();
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

    
    // JFrame frame = new JFrame("FrameDemo");
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // JLabel emptyLabel = new JLabel();
    // frame.getContentPane().add(emptyLabel, BorderLayout.CENTER);
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
    String user = "world-x";
    // Access a service running on the local machine on port 50051
    String target = "localhost:50051";
    // Allow passing in the user and target strings as command line arguments
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: [name [target]]");
        System.err.println("");
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
      user = args[0];
    }
    if (args.length > 1) {
      target = args[1];
    }

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable.
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build();
    try {
      NLImageClient client = new NLImageClient(channel);
      client.requestRotate(user);
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
