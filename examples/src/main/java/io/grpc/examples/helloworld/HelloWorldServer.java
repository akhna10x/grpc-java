/*
 * Neuralink image server.
 */

package io.grpc.examples.helloworld;

import com.neuralink.interviewing.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class HelloWorldServer {
  private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 50051;
    server = ServerBuilder.forPort(port)
        // .addService(new GreeterImpl())
        .addService(new NLImageServiceImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    logger.info("Initializing NL objects....");
    // new NLImageBuilder();
    // NLImage.builder();
    // new NLImage().builder();
    // NLImage.newBuilder().build();
    NLImage nlImage = 
        NLImage.newBuilder().build();
    //new NLImageBuilder.newBuilder().build();
    // ;// = new NLImage();
    logger.info("Created NL object...");
    
    //1. Create the frame.
    // JFrame frame = new JFrame("FrameDemo");

    // //2. Optional: What happens when the frame closes?
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // // //3. Create components and put them in the frame.
    // // //...create emptyLabel...
    // JLabel emptyLabel = new JLabel();
    // frame.getContentPane().add(emptyLabel, BorderLayout.CENTER);

    // // //4. Size the frame.
    // frame.pack();

    // // //5. Show it.
    // frame.setVisible(true);

    // 9-4-2020
    JFrame frame = new JFrame();
    ImageIcon icon = new ImageIcon("androidBook.jpg");
    JLabel label = new JLabel(icon);
    frame.add(label);
    frame.setDefaultCloseOperation
          (JFrame.EXIT_ON_CLOSE);
    frame.pack();
    // frame.setVisible(true);

    logger.info("Created window frame...");

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          HelloWorldServer.this.stop();
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
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final HelloWorldServer server = new HelloWorldServer();
    server.start();
    server.blockUntilShutdown();
  }
// NLImageService
static class NLImageServiceImpl extends NLImageServiceGrpc.NLImageServiceImplBase  {

    @Override
    public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
      logger.info("Runng RotateImage() impl...."); 
    }
    
    @Override 
    public void customImageEndpoint(NLCustomImageEndpointRequest req, 
                                    StreamObserver<NLCustomImageEndpointResponse> responseObserver) {
       // TODO: embed Neuralink logo
       logger.info("Running customImageEndpoint() ...");
    }
}

  static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      logger.info("Runng sayHello() impl...."); 
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();

      //  TODO: remove                                               

      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}
