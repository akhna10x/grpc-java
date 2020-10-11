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
 * Neuralink image service.
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
		final int colorTriplets = 3;
		
		private NLImage rotateGrayScale(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			final int numPaddingBytes = 8;
			NLImage img = req.getImage();
			int height = img.getHeight();
			int width = img.getWidth();
			int newHeight = img.getHeight();
			int newWidth = img.getWidth();
			byte[] imgBytes = img.toByteArray();
			if (height * width + numPaddingBytes != imgBytes.length) {
				System.err.println("Invalid img size args");
				return null;
			}
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
			
			System.out.println("Iscolor: " + img.getColor());
		    NLImage reply = 	NLImage.newBuilder()
					.setHeight(newHeight)
					.setWidth(newWidth)
					 .setData(ByteString.copyFrom(matrixBytes))
					.build();
			return reply;
		}
		
		private class RGB {
			byte r;
			byte g;
			byte b;
			
			public RGB(byte r, byte g, byte b) {
				this.r = r;
				this.g = g;
				this.b = b;
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
			// TODO: implement
			
			return null;
		}
		
		private NLImage rotateColor(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			final int numPaddingBytes = 8;
			NLImage img = req.getImage();
			int height = img.getHeight();
			final int colorTriplet = 3;
			int width = img.getWidth() * 3;
			int newHeight = img.getHeight();
			int newWidth = img.getWidth();
			byte[] imgBytes = img.toByteArray();
			// if (height * width  * colorTriplets + numPaddingBytes != imgBytes.length) {
			// 	System.err.println("Invalid img size args: " + imgBytes.length);
			// 	return null;
			// }
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
			rotation = NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG;
			// Rotate (90 degrees is counterclockwise)
			byte[][] rotated = null;
			RGB[][] rgbTmp = new RGB[img.getHeight()][img.getWidth()];
			
			if (rotation == NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG) {
				for (int row = 0; row < rgbMatrix.length; ++row) {
					for (int col = 0; col < rgbMatrix[0].length; ++col) {
						RGB[] oldRow = rgbMatrix[height - row - 1];
						rgbTmp[row] = oldRow;
					}
				}
			} else if (rotation == NLImageRotateRequest.Rotation.NINETY_DEG) {
				newHeight = img.getWidth();
				newWidth = img.getHeight();
				rgbTmp = new RGB[newHeight][newWidth];
				
				for (int row = 0; row < newHeight; ++row) {
					RGB[] newRow = new RGB[newWidth];
					for (int col = 0; col < newWidth; ++col) {
						newRow[col] = rgbMatrix[rgbMatrix[0].length -col - 1][row];
					}
					System.out.println("New row: " + newRow[0] + " " + newRow[1]);
					rgbTmp[row] = newRow;
				}
			} else if (rotation == NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG) {
				newHeight = img.getWidth();
				newWidth = img.getHeight();
				rgbTmp = new RGB[newHeight][newWidth];
				
				for (int row = 0; row < newHeight; ++row) {
					RGB[] newRow = new RGB[newWidth];
					for (int col = 0; col < newWidth; ++col) {
						newRow[col] = rgbMatrix[col][rgbMatrix.length - row -1];
					}
					System.out.println("New row: " + newRow[0] + " " + newRow[1]);
					rgbTmp[row] = newRow;
				}
			} else if (rotation == NLImageRotateRequest.Rotation.NONE) {
				rotated = matrix;
				System.out.println("Skipping rotation ");
			} else {
				rotated = new byte[newHeight][newWidth];
				System.err.println("Unhandled request type: ");
			}
			matrix = rotated;
			rgbMatrix = rgbTmp;
			
			System.out.println("Displaying RGB: ");
			for (int row = 0; row < rgbMatrix.length; ++row) {
				for (int col = 0; col < rgbMatrix[0].length; ++col) {
					System.out.print(rgbMatrix[row][col]+ " ");
					
					
				}
				System.out.println();
			}
			
			// TODO: update newHeight / width
			System.out.println("Iscolor: " + img.getColor());
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
						

			} else {
				reply = 
						rotateColor(req, responseObserver);
			}
			
			
//			NLImage.newBuilder()
//			.setHeight(newHeight)
//			.setWidth(newWidth)
//			 .setData(ByteString.copyFrom(matrixBytes))
//			.build();
			
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void rotateImage(NLImageRotateRequest req, StreamObserver<NLImage> responseObserver) {
			handleRequest(req, responseObserver);
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
			System.out.println("Running create grayscale image from customImageEndpoint()");
			// displayMatrix(bytes);
			for (int i = 0; i < bytes.length; ++i) {
				System.out.println("Byte " + i + ": " + bytes[i]);
			}

			NLImage replyImg = NLImage.newBuilder()
					.setWidth(width)
					.setHeight(height)
					.setData(ByteString.copyFrom(createGrayscaleImage(bytes, width, height, reqImg.getColor())))
					.build();
			NLCustomImageEndpointResponse reply = NLCustomImageEndpointResponse.newBuilder()
					.setImage(replyImg)
					.build();


			responseObserver.onNext(reply);
			responseObserver.onCompleted();
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

	private static byte[] createGrayscaleImage(byte[] b, int width, int height, boolean color) {
		byte[] ret = new byte[0];
		// TODO: implement
		// New grayscale image = ( (0.3 * R) + (0.59 * G) + (0.11 * B) ).
		final double rFactor = 0.3;
		final double gFactor = 0.59;
		final double bFactor = 0.11;
		
		System.out.println("Inside createGrayscaleImage");
		if (color) {
			

		} else {
		
		}
		
		
		
		ImageIcon icon = new ImageIcon(b);
		return ret;
	} 

	public static void main(String[] args) throws IOException, InterruptedException {
		final NLImageServer server = new NLImageServer();
		server.start();
		server.blockUntilShutdown();
	}
}
