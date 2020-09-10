/*
 * Neuralink image server.
 */

package com.neuralink.interviewing.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import javax.imageio.ImageIO;
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

	/** 
	 * NLImageService implementation.
	 */
	static class NLImageServiceImpl extends NLImageServiceGrpc.NLImageServiceImplBase {

		@Override
		public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			NLImage reqImg = req.getImage();
			int height = reqImg.getHeight();
			int width = reqImg.getWidth();
			ByteString reqImgBytes = reqImg.getData();
			byte[] bytes = reqImgBytes.toByteArray();
			ImageIcon icon = 
					createRotatedImage(bytes, width, height, reqImg.getColor(), req.getRotation());
			try {
				int newWidth = width;
				int newHeight = height;
				if (req.getRotation() == NLImageRotateRequest.Rotation.NINETY_DEG ||
					req.getRotation() == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
					newWidth = height;
					newHeight = width;
				}
				NLImage reply = NLImage.newBuilder()
						.setWidth(newWidth)
						.setHeight(newHeight)
						.setData(imgToByteString(icon))
						.build();
				responseObserver.onNext(reply);
				responseObserver.onCompleted();
			} catch(IOException e) {
				System.err.println("IOException: " + e.getMessage());
			}
		}

		@Override 
		public void customImageEndpoint(NLCustomImageEndpointRequest req, 
				StreamObserver<NLCustomImageEndpointResponse> responseObserver) {
			NLImage reqImg = req.getImage();
			int height = reqImg.getHeight();
			int width = reqImg.getWidth();
			ByteString reqImgBytes = reqImg.getData();
			byte[] bytes = reqImgBytes.toByteArray();
			ImageIcon icon = createWatermarkImage(bytes, width, height, reqImg.getColor());
			try {
				NLImage replyImg = NLImage.newBuilder()
						.setWidth(width)
						.setHeight(height)
						.setData(imgToByteString(icon))
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

	private static BufferedImage convertToBufferedImage(
			java.awt.Image image, boolean color) {
		BufferedImage newImage;
		if (color) {
			newImage = new BufferedImage(
					image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
		} else {
			newImage = new BufferedImage(
					image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_BYTE_GRAY);
		}

		Graphics2D g = newImage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}

	private static ByteString imgToByteString(ImageIcon icon) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream stream = new MemoryCacheImageOutputStream(baos);
		ImageIO.write((
				(BufferedImage) icon.getImage()), "png", stream);
		return ByteString.copyFrom(baos.toByteArray());
	}

	private static ImageIcon createRotatedImage(byte[] b, int width, 
			int height, boolean color, 
			NLImageRotateRequest.Rotation rotation) {
		ImageIcon icon = new ImageIcon(b);
		java.awt.Image rawImage = icon.getImage();
		BufferedImage image = convertToBufferedImage(rawImage, color);
		if (rotation == NLImageRotateRequest.Rotation.NONE) { 
			icon.setImage(rotate(image, 0.0));
		} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
			icon.setImage(rotate(image, 90.0));
		} else if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
			icon.setImage(rotate(image, 180.0));
		} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
			icon.setImage(rotate(image, 270.0));
		}

		return icon;
	}

	private static ImageIcon createWatermarkImage(byte[] b, int width, int height, boolean color) {
		ImageIcon icon = new ImageIcon(b);
		java.awt.Image rawImage = icon.getImage();
		BufferedImage image = convertToBufferedImage(rawImage, color);
		icon.setImage(createWatermarked(image));
		return icon;
	}

	private static GraphicsConfiguration getDefaultConfiguration() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		return gd.getDefaultConfiguration();
	}

	private static BufferedImage rotate(BufferedImage image, double angle) {
		int w = image.getWidth(), h = image.getHeight();
		GraphicsConfiguration gc = getDefaultConfiguration();
		BufferedImage result = gc.createCompatibleImage(w, h);
		Graphics2D g = result.createGraphics();
		g.rotate(Math.toRadians(angle), w / 2, h / 2);
		g.drawRenderedImage(image, null);
		g.dispose();
		return result;
	}

	private static BufferedImage addImageWatermark(BufferedImage sourceImage) {
		GraphicsConfiguration gc = getDefaultConfiguration();
		BufferedImage result = 
			gc.createCompatibleImage(sourceImage.getWidth(), 
									 sourceImage.getHeight());
		try {
			BufferedImage watermarkImage = ImageIO.read(new File("watermark.png"));
			Graphics2D g = result.createGraphics();
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
			AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
			g2d.setComposite(alphaChannel);
			int topLeftX = (sourceImage.getWidth() - watermarkImage.getWidth()) / 2;
			int topLeftY = (sourceImage.getHeight() - watermarkImage.getHeight()) / 2;
			g2d.drawImage(watermarkImage, topLeftX, topLeftY, null);
			g.dispose();
			return sourceImage;
		} catch (IOException ex) {
			System.err.println(ex);
		}
		return result;
	}

	private static BufferedImage createWatermarked(BufferedImage image){
		return addImageWatermark(image);
	}

	/**
	 * Launches the server.
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final NLImageServer server = new NLImageServer();
		server.start();
		server.blockUntilShutdown();
	}
}
