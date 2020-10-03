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
import java.util.Arrays;
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
	
	private static void displayMatrix(byte matrix[][]) { 
        for (int i = 0; i < matrix.length; i++) { 
            for (int j = 0; j < matrix[0].length; j++) {
            	byte cur = matrix[i][j];
             	System.out.print(String.format("%02X ",cur));
            } 
            System.out.print("\n"); 
        }
	} 

	/** 
	 * NLImageService implementation.
	 */
	static class NLImageServiceImpl extends NLImageServiceGrpc.NLImageServiceImplBase {
		
		private NLImage rotateGrayScale(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			final int numPaddingBytes = 8;
			NLImage img = req.getImage();
			int height = img.getHeight();
			int width = img.getWidth();
			int newHeight = img.getHeight();
			int newWidth = img.getWidth();
			byte[] imgBytes = img.toByteArray();
			System.out.println("Length: " + imgBytes.length);
			System.out.println(Arrays.toString(imgBytes));
			// TODO: validate color
//			if (height * width + numPaddingBytes != imgBytes.length) {
//				System.err.println("Invalid img size args");
//				return null;
//			}
					
			int index = numPaddingBytes / 2; // Skip header 
			byte[][] matrix = new byte[height][width];
			for (int row = 0; row < height; ++row) {
				for (int col = 0; col < width; ++col) {
				   matrix[row][col] = imgBytes[index];
				   index++;
				}
			}
			NLImageRotateRequest.Rotation rotation = req.getRotation();
			// Rotate (90 degrees is counterclockwise)
			byte[][] rotated = null;
			if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
				rotated = new byte[newHeight][newWidth];
				for (int row = 0; row < height; ++row) {
					for (int col = 0; col < width; ++col) {
						byte[] oldRow = matrix[height - row - 1];
						rotated[row] = oldRow;
					}
				}
			} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
				newHeight = width;
				newWidth = height;
				rotated = new byte[newHeight][newWidth];
				for (int row = 0; row < newHeight; ++row) {
					byte[] newRow = new byte[newWidth];
					for (int col = 0; col < newWidth; ++ col) {
						newRow[col] = matrix[col][0];
						
					}
					rotated[row] = newRow;
				}
			} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
				newHeight = width;
				newWidth = height;
				rotated = new byte[newHeight][newWidth];
				
				for (int row = 0; row < newHeight; ++row) {
					byte[] newRow = new byte[newWidth];
					for (int col = 0; col < newWidth; ++ col) {
						newRow[col] = matrix[newWidth - col -1][0];
						
					}
					rotated[row] = newRow;
				}
					
			} else if (rotation == NLImageRotateRequest.Rotation.NONE) {
				rotated = matrix;
				System.out.println("Skipping rotation ");
			} else {
				rotated = new byte[newHeight][newWidth];
				System.err.println("Unhandled request type: ");
			}
			matrix = rotated;
			displayMatrix(matrix);
		
			
			byte[] matrixBytes = new byte[newHeight * newWidth];
			index = 0;
			for (int row = 0; row < newHeight; ++ row) {
				for (int col = 0; col < newWidth; ++col) {
					matrixBytes[index] = matrix[row][col];
					++index;
				}
			}
			
			
			// TODO: update newHeight / width
			System.out.println("Iscolor: " + img.getColor());
			System.out.println("Iscolor: " + img.getColor());
			
			// TODO fix
			return null;
		}
		
		private class RGB {
			byte r;
			byte g;
			byte b;
			
			public RGB(byte r, byte g, byte b) {
				this.r = r;
				this.g = g;
				this.b = b;
//				r
			}
			
			public String toString() {
				return "RGB: " + r + " " + g + " " + b;
			}
		}
		
		
		private RGB[][] toRGB(byte[][] bytes) {
			System.out.println("DEBUG: toRGB");
			RGB[][] rgb = 
				new RGB[bytes.length][bytes[0].length / 3];
			for (int row = 0; row < rgb.length; ++row) {
				for (int col = 0; col < rgb[0].length; ++col) {
					int bytesCol = col * 3;
					byte r = bytes[row][bytesCol];
					byte g = bytes[row][bytesCol + 1];
					byte b = bytes[row][bytesCol + 2];
					
					rgb[row][col] = new RGB(r, g, b);
					
					
				}
			}

			
			
			return rgb;
		}
		
		private byte[][] toBytes(RGB[][] rgb) {
			
			
			return null;
		}
		
		private NLImage rotateColor(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			final int numPaddingBytes = 8;
			System.out.println("...inside rotateColor()");
			NLImage img = req.getImage();
			int height = img.getHeight();
			final int colorTriplet = 3;
			int width = img.getWidth() * 3;
//			System
//					* colorTriplet;
			int newHeight = img.getHeight();
			int newWidth = img.getWidth();
			byte[] imgBytes = img.toByteArray();
//			RGB[][] imgBytesX = toRGB(img.toByteArray());
			
			
			
			
			
			
			System.out.println("Length: " + imgBytes.length);
			System.out.println(Arrays.toString(imgBytes));
			// TODO: validate color
//			if (height * width + numPaddingBytes != imgBytes.length) {
//				System.err.println("Invalid img size args");
//				return null;
//			}
					
			int index = numPaddingBytes / 2; // Skip header 
			byte[][] matrix = new byte[height][width];
			for (int row = 0; row < height; ++row) {
				for (int col = 0; col < width; ++col) {
				   matrix[row][col] = imgBytes[index];
				   index++;
				}
			}
			
			RGB[][] rgbMatrix = toRGB(matrix);
			System.out.println("Displaying RGB: ");
			for (int row = 0; row < rgbMatrix.length; ++row) {
				for (int col = 0; col < rgbMatrix[0].length; ++col) {
					System.out.print(rgbMatrix[row][col]+ " ");
					
					
				}
				System.out.println();
			}
			
			
			
			NLImageRotateRequest.Rotation rotation = req.getRotation();
			// TODO: remove
			rotation = NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG;
			// Rotate (90 degrees is counterclockwise)
			byte[][] rotated = null;
//			RGB
			RGB[][] rgbTmp = new RGB[img.getHeight()][img.getWidth()];
			
			if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
//				rotated = new byte[newHeight][newWidth];
				for (int row = 0; row < height; ++row) {
					for (int col = 0; col < img.getWidth(); ++col) {
//						byte[] oldRow = matrix[height - row - 1];
//						rotated[row] = oldRow;
						
						RGB[] oldRow = rgbMatrix[height - row - 1];
						rgbTmp[row] = oldRow;
						
					}
				}
			} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
				newHeight = width;
				newWidth = height;
				rotated = new byte[newHeight][newWidth];
				for (int row = 0; row < newHeight; ++row) {
					byte[] newRow = new byte[newWidth];
					for (int col = 0; col < newWidth; ++ col) {
						newRow[col] = matrix[col][0];
						
					}
					rotated[row] = newRow;
				}
			} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
				newHeight = width;
				newWidth = height;
				rotated = new byte[newHeight][newWidth];
				
				for (int row = 0; row < newHeight; ++row) {
					byte[] newRow = new byte[newWidth];
					for (int col = 0; col < newWidth; ++ col) {
						newRow[col] = matrix[newWidth - col -1][0];
						
					}
					rotated[row] = newRow;
				}
					
			} else if (rotation == NLImageRotateRequest.Rotation.NONE) {
				rotated = matrix;
				System.out.println("Skipping rotation ");
			} else {
				rotated = new byte[newHeight][newWidth];
				System.err.println("Unhandled request type: ");
			}
			// TODO: add back
			matrix = rotated;
			
			
//			if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
//				rotated = new byte[newHeight][newWidth];
//				for (int row = 0; row < height; ++row) {
//					for (int col = 0; col < width; ++col) {
//						byte[] oldRow = matrix[height - row - 1];
//						rotated[row] = oldRow;
//					}
//				}
//			} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
//				newHeight = width;
//				newWidth = height;
//				rotated = new byte[newHeight][newWidth];
//				for (int row = 0; row < newHeight; ++row) {
//					byte[] newRow = new byte[newWidth];
//					for (int col = 0; col < newWidth; ++ col) {
//						newRow[col] = matrix[col][0];
//						
//					}
//					rotated[row] = newRow;
//				}
//			} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
//				newHeight = width;
//				newWidth = height;
//				rotated = new byte[newHeight][newWidth];
//				
//				for (int row = 0; row < newHeight; ++row) {
//					byte[] newRow = new byte[newWidth];
//					for (int col = 0; col < newWidth; ++ col) {
//						newRow[col] = matrix[newWidth - col -1][0];
//						
//					}
//					rotated[row] = newRow;
//				}
//					
//			} else if (rotation == NLImageRotateRequest.Rotation.NONE) {
//				rotated = matrix;
//				System.out.println("Skipping rotation ");
//			} else {
//				rotated = new byte[newHeight][newWidth];
//				System.err.println("Unhandled request type: ");
//			}
//			matrix = rotated;
			displayMatrix(matrix);
		
			
			byte[] matrixBytes = new byte[newHeight * newWidth];
			index = 0;
			for (int row = 0; row < newHeight; ++ row) {
				for (int col = 0; col < newWidth; ++col) {
					matrixBytes[index] = matrix[row][col];
					++index;
				}
			}
			
			
			// TODO: update newHeight / width
			System.out.println("Iscolor: " + img.getColor());
			System.out.println("Iscolor: " + img.getColor());
			
			// TODO fix
			return null;
		}
		
		private void handleRequest(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			// Validate image
			System.out.println("DEBUG: running handleRequest()");
			final int numPaddingBytes = 8;
			NLImage img = req.getImage();
			int height = img.getHeight();
			int width = img.getWidth();
			
			NLImage reply;
			
			if (!img.getColor()) {
				reply = 
						rotateGrayScale(req, responseObserver);
						
	//					NLImage.newBuilder()
	//					.setHeight(newHeight)
//	//					.setWidth(newWidth)
//	//					 .setData(ByteString.copyFrom(matrixBytes))
//	//					.build();
//				return reply;
			} else {
				reply = 
						rotateColor(req, responseObserver);
						
////						NLImage.newBuilder()
////						.setHeight(newHeight)
////						.setWidth(newWidth)
////						 .setData(ByteString.copyFrom(matrixBytes))
////						.build();
//				return reply;
			}
			
			
//			// TODO: update newHeight / width
//			NLImage reply = NLImage.newBuilder()
//					.setHeight(newHeight)
//					.setWidth(newWidth)
//					 .setData(ByteString.copyFrom(matrixBytes))
//					.build();
//			responseObserver.onNext(reply);
//			responseObserver.onCompleted();

			
			
//			int newHeight = img.getHeight();
//			int newWidth = img.getWidth();
//			byte[] imgBytes = img.toByteArray();
//			System.out.println("Length: " + imgBytes.length);
//			System.out.println(Arrays.toString(imgBytes));
//			if (height * width + numPaddingBytes != imgBytes.length) {
//				System.err.println("Invalid img size args");
//				return;
//			}
//					
//			int index = numPaddingBytes / 2; // Skip header 
//			byte[][] matrix = new byte[height][width];
//			for (int row = 0; row < height; ++row) {
//				for (int col = 0; col < width; ++col) {
//				   matrix[row][col] = imgBytes[index];
//				   index++;
//				}
//			}
//			
//			
//			// TODO: update newHeight / width
//			NLImage reply = NLImage.newBuilder()
//					.setHeight(newHeight)
//					.setWidth(newWidth)
//					 .setData(ByteString.copyFrom(matrixBytes))
//					.build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			handleRequest(req, responseObserver);
			// TODO: reject overly large images
			return;
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
			java.awt.Image image, boolean color, int width, int height) {
		BufferedImage newImage;
		if (color) {
			newImage = new BufferedImage(
					width, height, BufferedImage.TYPE_INT_RGB);
		} else {
			newImage = new BufferedImage(
					width, height, BufferedImage.TYPE_BYTE_GRAY);
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
		BufferedImage image = 
				convertToBufferedImage(rawImage, color, width, height);
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

	private static ImageIcon createWatermarkImage(byte[] b, int width, int height, boolean color) {
		ImageIcon icon = new ImageIcon(b);
		java.awt.Image rawImage = icon.getImage();
		BufferedImage image = convertToBufferedImage(rawImage, color, width, height);
		icon.setImage(addImageWatermark(image));
		return icon;
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

	private static GraphicsConfiguration getDefaultConfiguration() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		return gd.getDefaultConfiguration();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final NLImageServer server = new NLImageServer();
		server.start();
		server.blockUntilShutdown();
	}
}
