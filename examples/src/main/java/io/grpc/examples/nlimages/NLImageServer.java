/*
 * Neuralink image server.
 */

package io.grpc.examples.nlimages;

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
    
      @Override
      public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
        logger.info("Runng RotateImage() impl...."); 
        // TODO handle error

        // TODO: return valid response
        int TMP_SIZE = 70;
        byte[] rotatedImg = new byte[TMP_SIZE];
        NLImage reply = NLImage.newBuilder()
          .setWidth(TMP_SIZE)
          .setHeight(TMP_SIZE)
          // .setData()
          .build();
        // TODO: set data

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
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

