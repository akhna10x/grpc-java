/*
 * Neuralink image server.
 */

package com.neuralink.interviewing.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Iterator;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBufferByte;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

/**
 * Neuralink image service server.
 */
public class NLImageServer {
	private static final Logger logger = Logger.getLogger(NLImageServer.class.getName());

	private Server server;

	private void start() throws IOException {
		int port = 9090;
		server = ServerBuilder.forPort(port)
				.addService(new NLImageServiceImpl())
				.build()
				.start();
		logger.info("Listening on " + port);
		NLImage nlImage =  NLImage.newBuilder().build();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					NLImageServer.this.stop();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
				System.err.println("Server shut down");
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

	// NLImageService
	static class NLImageServiceImpl extends NLImageServiceGrpc.NLImageServiceImplBase  {n
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

		public ImageIcon createImage(byte[] b, int width, 
				int height, 
				NLImageRotateRequest.Rotation rotation) {
			ImageIcon icon = new ImageIcon(b);
			java.awt.Image rawImage = icon.getImage();
			BufferedImage image = convertToBufferedImage(rawImage);
			icon.setImage(createFlipped(image));
			if (rotation == NLImageRotateRequest.Rotation.NONE) { 
				icon.setImage(rotate(image,0.0));
			} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
				icon.setImage(rotate(image, 90.0));
			} else if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
				icon.setImage(rotate(image, 180.0));
			} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
				icon.setImage(rotate(image, 270.0));
			}

			return icon;
		}

		public ImageIcon createWatermarkImage(byte[] b, int width, int height) {
			ImageIcon icon = new ImageIcon(b);
			java.awt.Image rawImage = icon.getImage();
			BufferedImage image = convertToBufferedImage(rawImage);
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
				g.dispose();
				return sourceImage;

			} catch (IOException ex) {
				System.err.println(ex);
			}
			return result;
		}

		private static BufferedImage createFlipped(BufferedImage image){
			return rotate(image, 180.0);
		}
		private static BufferedImage createWatermarked(BufferedImage image){
			return addImageWatermark(image);
		}

		@Override
		public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			NLImage reqImg = req.getImage();
			int height = reqImg.getHeight();
			int width = reqImg.getWidth();
			byte[] rotatedImg = new byte[width * height];
			ByteString reqImgBytes = reqImg.getData();
			byte[] bytes = reqImgBytes.toByteArray();
			ImageIcon icon = 
					createImage(bytes, width, height, req.getRotation());
			displayResponse(icon);
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream ();
				ImageOutputStream stream = new MemoryCacheImageOutputStream(baos);
				ImageIO.write((
						(BufferedImage) icon.getImage()), "png", stream);
				stream.close();
				baos.toByteArray();


				int newWidth = width;
        int newHeight = height;
        if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG ||
            rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
              newWidth = height;
              newHeight = width;
        }
				NLImage reply = NLImage.newBuilder()
						.setWidth(newWidth)
						.setHeight(newHeight)
						.setData(ByteString.copyFrom(baos.toByteArray()))
						.build();
				responseObserver.onNext(reply);
				responseObserver.onCompleted();
			} catch(IOException e) {
				System.err.println("IOExceotion: " + e.getMessage());
			}
		}

		private void displayResponse(ImageIcon icon) {
			JFrame frame = new JFrame();
			JLabel label = new JLabel(icon);
			frame.add(label);
			frame.pack();
			frame.setVisible(true);
		}

		@Override 
		public void customImageEndpoint(NLCustomImageEndpointRequest req, 
				StreamObserver<NLCustomImageEndpointResponse> responseObserver) {
			NLImage reqImg = req.getImage();
			int height = reqImg.getHeight();
			int width = reqImg.getWidth();
			ByteString reqImgBytes = reqImg.getData();
			byte[] bytes = reqImgBytes.toByteArray();
			ImageIcon icon = createWatermarkImage(bytes, width, height);
			displayResponse(icon);
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageOutputStream stream = new MemoryCacheImageOutputStream(baos);
				ImageIO.write((
						(BufferedImage) icon.getImage()), "png", stream);
				stream.close();
				baos.toByteArray();


				NLImage replyImg = NLImage.newBuilder()
						.setWidth(width)
						.setHeight(height)
						.setData(ByteString.copyFrom(baos.toByteArray()))
						.build();
				NLCustomImageEndpointResponse reply = NLCustomImageEndpointResponse.newBuilder()
						.setImage(replyImg)
						.build();
				responseObserver.onNext(reply);
				responseObserver.onCompleted();
			} catch(IOException e) {
				System.err.println("IOException: " + e.getMessage());
			}
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
}
