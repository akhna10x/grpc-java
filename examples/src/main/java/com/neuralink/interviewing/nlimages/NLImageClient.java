/*
 * Neuralink image client.
 */

package com.neuralink.interviewing.nlimages;

import com.google.protobuf.ByteString;
import com.neuralink.interviewing.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;

/**
 * Client for managing requests to the Neuralink image service.
 */
public class NLImageClient {
	private static final Logger logger = 
			Logger.getLogger(NLImageClient.class.getName());

	private final NLImageServiceGrpc.NLImageServiceBlockingStub blockingStub;

	/** 
	 * Creates an NLImageClient using the provided channel.
	 **/
	public NLImageClient(Channel channel) {
		blockingStub = NLImageServiceGrpc.newBlockingStub(channel);
	}


	private static void displayMatrix(byte mat[][]) { 
		int N = mat.length;
		for (int i = 0; i < N; i++) { 
			for (int j = 0; j < mat[0].length; j++) 
				System.out.print(" " + mat[i][j]); 

			System.out.print("\n"); 
		} 
		System.out.print("\n"); 
	} 

	/** 
	 * Requests an image be rotated.
	 */
	public void requestRotate(String filename, boolean isColor, 
			NLImageRotateRequest.Rotation rotation) throws IOException{
		byte[] bytes = {(byte) 0x7, (byte) 0x7, (byte) 0x7, 
				(byte) 0x8, (byte) 0x8, (byte) 0x8};
		for (int i = 0; i < bytes.length; ++i) {
			System.out.println("byte: " + bytes[i]);
		}
		ByteString byteString = ByteString.copyFrom(bytes);
		System.out.println("Byte string: " + byteString);
		System.out.println("isColor: " + isColor);

		BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
		NLImage nlImage = NLImage.newBuilder()
				.setColor(isColor)
				.setData(byteString)
				//				.setWidth(img.getWidth())
				//				.setHeight(img.getHeight())
				.setWidth(3)
				.setHeight(2)
				.build();
		NLImageRotateRequest request = NLImageRotateRequest.newBuilder()
				.setImage(nlImage)
				.setRotation(rotation)
				.build();

		NLImage response;
		try {
			response = blockingStub.rotateImage(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		//		byte[] rBytes = response.getData().toByteArray();
		//		ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight());
		//		displayResponse(icon);
	}


	/** 
	 * Requests adding a watermark to an image.
	 */
	public void requestWatermark(String filename, boolean isColor) 
			throws IOException{
		byte[] bytes = readImgFile(filename);
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
		NLImage nlImage = NLImage.newBuilder()
				.setColor(isColor)
				.setData(ByteString.copyFrom(bytes))
				.setWidth(img.getWidth())
				.setHeight(img.getHeight())
				.build();
		NLCustomImageEndpointRequest request = NLCustomImageEndpointRequest.newBuilder()
				.setImage(nlImage)
				.build();

		NLImage response;
		try {
			response = blockingStub.customImageEndpoint(request).getImage();
			ByteString respImgBytes = response.getData();
			byte[] rBytes = response.getData().toByteArray();
			ImageIcon icon = createRespImage(rBytes, response.getWidth(), response.getHeight());
			displayResponse(icon);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
	}

	private byte[] readImgFile(String filename) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final int maxBufferSize = 1024;
		byte[] buf = new byte[maxBufferSize];
		try {
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum); 
			}
		} catch (IOException e) {
			logger.info("IOException: " + e.getMessage());
		}
		byte[] bytes = bos.toByteArray();
		return bytes;
	}

	private ImageIcon createRespImage(byte[] b, int width, int height) {
		java.awt.Image img = new ImageIcon(b).getImage().getScaledInstance(
				width, 
				height, 
				java.awt.Image.SCALE_SMOOTH); 
		return new ImageIcon(img);
	}

	private void displayResponse(ImageIcon icon) {
		JFrame frame = new JFrame();
		frame.add(new JLabel(icon));
		frame.pack();
		frame.setVisible(true);
	}

	private static class Option {
		String flag, opt;
		public Option(String flag, String opt) { 
			this.flag = flag; 
			this.opt = opt; 
		}
	}

	private static Map<String, Option> parseArgs(String[] args) {
		Map<String, Option> optsSet = new HashMap<>();
		List<Option> optsList = new ArrayList<Option>();
		List<String> argsList = new ArrayList<String>();  

		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(0)) {
			case '-':
				if (args[i].length() < 2) {
					throw new IllegalArgumentException("Not a valid argument: " + args[i]);
				}
				if (args[i].length() < 3) {
					throw new IllegalArgumentException("Not a argument: " + args[i]);
				}
				String name = args[i].replace("-", "");
				if (args.length - 1 == i) {
					throw new IllegalArgumentException("Expected arg after: " + args[i]);
				}

				optsList.add(new Option(args[i], args[i+1]));
				optsSet.put(name, new Option(name, args[i+1]));
				i++;
				break;
			default:
				argsList.add(args[i]);
				break;
			}
		}

		return optsSet;
	}
	/**
	 * Runs Neuralink image client.
	 */
	public static void main(String[] args) throws Exception {
		// Flags and associated constants.
		final String targetFlag = "target";
		final String endpointFlag = "endpoint";
		final String filenameFlag = "filename";
		final String colorFlag = "color";
		final String rotateFlag = "rotate";
		final String watermark = "watermark";
		String target = "localhost:9090";
		String rotationCmd = "0";
		String filename = "sample-1.png";
		boolean isColor = true;
		boolean watermarkEndpoint = false;
		System.out.println("Running client...");
		if (args.length > 0) {
			if ("--help".equals(args[0])) {
				System.err.println("Usage: [--target target_server] " +
						"[--endpoint [rotate | watermark]] [--filename filename] " +
						"[--color [yes | no]] [--rotate rotation_degrees]");
				System.exit(1);
			}
		}
		Map<String, Option> opts = parseArgs(args);
		if (opts.get(targetFlag) != null) {
			target = opts.get(targetFlag).opt;
		}
		if (opts.get(endpointFlag) != null) {
			if (opts.get(endpointFlag).opt.equals(watermark)) {
				watermarkEndpoint = true;
			}
		}
		if (opts.get(filenameFlag) != null) {
			filename = opts.get(filenameFlag).opt;
		}
		if (opts.get(colorFlag) != null) {
			if (opts.get(colorFlag).opt.equals("false")) {
				isColor = false;
			}
		}
		if (opts.get(rotateFlag) != null) {
			rotationCmd = opts.get(rotateFlag).opt;
		}

		ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
				.usePlaintext()
				.build();
		try {
			NLImageClient client = new NLImageClient(channel);
			if (watermarkEndpoint) {
				client.requestWatermark(filename, isColor); 
			} else {
				NLImageRotateRequest.Rotation rotation = 
						NLImageRotateRequest.Rotation.NONE;
				if (rotationCmd.equals("90")) {
					rotation = NLImageRotateRequest.Rotation.NINETY_DEG;
				} else if (rotationCmd.equals("180")) {
					rotation = NLImageRotateRequest.Rotation.ONE_EIGHTY_DEG;
				} else if (rotationCmd.equals("270")) {
					rotation = NLImageRotateRequest.Rotation.TWO_SEVENTY_DEG;
				} else if (rotationCmd.equals("0")) {
					rotation = NLImageRotateRequest.Rotation.NONE;
				} else {
					System.err.println("Invalid rotation specified. Exiting.");
					return;
				}
				client.requestRotate(filename, isColor, rotation); 
			}
		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}
}
