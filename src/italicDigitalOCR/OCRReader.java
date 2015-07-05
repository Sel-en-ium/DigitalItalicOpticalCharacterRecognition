package italicDigitalOCR;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

public class OCRReader {
	
	/* Config Constants */
	private static boolean DEBUG = false;
	private static boolean TRAINING = true;
	private static String ERROR_LOG = "OcrErrorLog.txt";
	private static String trainingFolder = "OcrTraining";
	private static String characterFolder = "Characters";
	private static String errorFolder = "Errors";
	
	public static void main(String[] args) {
		try {
			OCRReader reader = new OCRReader();
			reader.printCharMap();
			BufferedImage image = ImageIO.read(new File("screenshot.png"));
			String readin = reader.readLines(image);
			System.out.println("SUCCESS LOL!  Well here it is.. '" + readin + "'");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	

	private static int nonTextColor = -16777215; // Just off black, RGB = 0,0,1
	private static int[] textColors = { -1, // White
			-10582070, // Met Blue
			-1571587, // Elite/Item bottom line Pink
			-16711936, // Equipment Bonus Green
			-65536, // Lock Text Red
			-3866137, // Equipment Bonus purple
			-1317505, // Super Gold/Yellow
			-494583, // Tortoise Gem Bonus Orange
			-7549187, // Magic Def% Bonus Blue
			-8521341, // Sockected Gem Green
			-256, // Npc Yellow
			-32704 // Gourd kills Orange
	};

	// Leftmost cols first
	private static int[][] generalInterferenceZoneLeft = { { 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 },
			{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

	// Rightmost cols first
	private static int[][] generalInterferenceZoneRight = { { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 } };

	/* Config Chars and stuff */
	private OCRChar space;
	
	private int expectedHeight;
	private int whiteSpaceLeft;
	
	
	// 1st index = whiteSpaceLeft, 2nd index = yLeftTopCharPixel, 3rd = ordered by numPixels(largest), height(largest) (maybe add width)
	private LinkedList<LinkedList<LinkedList<OCRChar>>> charMap;   
	private BufferedImage image;
	private int height;
	private int width;
	
	private int topLine;
	private int textColor;
	private int x = 0;
	private int y = 0;

	public OCRReader() throws Exception {

		 this.loadConfiguration();

		LinkedList<OCRChar> allChars = new LinkedList<OCRChar>();
		this.getAllChars(allChars);
		
		this.populateCharMap(allChars);		
	}
	
	private void loadConfiguration() throws Exception {
		this.expectedHeight = this.getTrainingImage("Height.png").getHeight();
		this.whiteSpaceLeft = this.getTrainingImage("WhiteSpaceLeft.png").getWidth() + 1;
		
		this.space = new OCRChar(this.getTrainingImage("Space.png"), ' ');
	}
	
	private BufferedImage getTrainingImage(String fileName) throws IOException {
		return ImageIO.read(new File(this.trainingFolder + "/" + fileName));
	}
	
	private void populateCharMap(List<OCRChar> allChars) {
		LinkedList<LinkedList<OCRChar>> heightList;
		LinkedList<OCRChar> charList;
		
		// Initialize lists
		this.charMap = new LinkedList<LinkedList<LinkedList<OCRChar>>>();
		for (int i = 0; i < this.whiteSpaceLeft; i++) {
			this.charMap.add(new LinkedList<LinkedList<OCRChar>>());
			heightList = this.charMap.get(i);
			for (int j = 0; j < this.expectedHeight; j ++) {
				heightList.add(new LinkedList<OCRChar>());
			}
		}
		
		// Add all chars to map in particular location and order
		OCRChar c;
		for (int a = 0; a < allChars.size(); a++) {
			c = allChars.get(a);
			charList = this.charMap.get(c.whiteSpaceLeft).get(c.yLeftTopPixel);
			
			int i = 0;
			while (i < charList.size()) {
				if (charList.get(i).numCharacterPixels > c.numCharacterPixels) {
					i++;
				} else if (charList.get(i).charHeight > c.charHeight) {
					i++;
				} else {
					break;
				}
			}
			charList.add(i, c);
		}					
	}
	
	private void getAllChars(List<OCRChar> list) throws IOException {
		// Iterates through all files in the directory
		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(this.trainingFolder + "/" + this.characterFolder));
		for (Path currentFile : stream) {

			if (Files.isDirectory(currentFile)) {
				String folder = currentFile.getFileName().toString();
				DirectoryStream<Path> subStream = Files.newDirectoryStream(currentFile);
				
				for (Path subFile : subStream) {
					addCharToList(list, folder, subFile.getFileName().toString());
				}
			} else {
				addCharToList(list, "", currentFile.getFileName().toString());
			}
		}
	}

	private void addCharToList(List<OCRChar> list, String folder, String fileName) {
		try {
			OCRChar c;
			if (folder.equals("")) {
				c = new OCRChar(this.trainingFolder + "/" + this.characterFolder, fileName);
			} else {
				c = new OCRChar(this.trainingFolder + "/" + this.characterFolder, folder, fileName);
			}
			
			if (c.imageHeight != this.expectedHeight) {
				throw new Exception("Character " + folder + "/" + fileName + " is unexpected height.");
			}
			list.add(c);
		} catch (Exception e) {
			this.log(e.toString(), false);
		}
	}

	public String readLines(BufferedImage image) {
		this.image = image;
		this.height = image.getHeight();
		this.width = image.getWidth();
		try {
			return this.readLine(0);
		} catch (Exception e) {
			return "";
		}
		
	}

	public String readLine(BufferedImage image) throws Exception {
		this.image = image;
		this.height = image.getHeight();
		this.width = image.getWidth();
		return this.readLine(0);
	}

	private String readLine(int topLineSearchStart) throws Exception {
		
		// If an exception is thrown from here that means there is no line to find (hopefully :D)
		this.textColor = this.getFirstColor(topLineSearchStart);
		this.topLine = this.getNextTopLine(this.textColor, topLineSearchStart);

		String result = "";
		boolean expectedChar = false;
		this.x = 0;
		while (this.x < this.width) {
			try {
				result += getChar(this.x, expectedChar);
				expectedChar = true;
			} catch (Exception e) {
				// Should mean we've finished reading the line.
				result = result.trim();
			}
		}
		return result;
	}

	private int getNextTopLine(int textColor, int yStart) throws Exception {
		int topLineTemp = this.topLine;

		for (int y = yStart; y < this.height - this.expectedHeight; y++) {
			for (int x = 0; x < this.width; x++) {

				// Found a character pixel, try to match it
				if (image.getRGB(x, y) == textColor) {

					// Iterate through all characters to find a match (Check highest heights first)
					LinkedList<OCRChar> charList;
					OCRChar chara;
					for (int heightLv = 0; heightLv < this.expectedHeight; heightLv++) {
						for (int whiteSpaceLv = 0; whiteSpaceLv < this.whiteSpaceLeft; whiteSpaceLv++) {
							charList = this.charMap.get(whiteSpaceLv).get(heightLv);
							
							for (int c = 0; c < charList.size(); c++) {
								chara = charList.get(c);
								
								this.topLine = topLineTemp + y - chara.yLeftTopPixel;
								if (this.testMatch(chara, x - chara.xLeftTopPixel)) {
									return this.topLine;
								}
							}
						}
					}
				}
			}
		}

		throw new Exception("Could not find the top line.");
	}

	private int getFirstColor(int startY) throws Exception {

		for (int y = startY; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				if (matchesTextColors(image.getRGB(x, y))) {
					return image.getRGB(x, y);
				}
			}
		}

		throw new Exception("Could not find any known text color.");
	}

	/**
	 * Returns the character match of a suspected pixel at a certain offset.
	 * 
	 * @param xStart
	 * @return
	 * @throws Exception
	 */
	private char getChar(int xStart, boolean expectedChar) throws Exception {
		OCRChar match = findMatch(xStart, expectedChar);

		return match.charName;
	}

	/**
	 * Delegates to expected or unexpected match.
	 * 
	 * @param xStart
	 * @return
	 * @throws Exception
	 */
	private OCRChar findMatch(int xStart, boolean expectedChar) throws Exception {
		
		if (expectedChar) {
			try {
				return this.findExpectedMatch(xStart);
			} catch (Exception e) {
				// We may have encountered an unknown character, the result may be off
				this.logImage("May have encountered unknown character at x=" + xStart + " y=" + this.topLine, "unknownkCharacter", TRAINING);
				return this.findUnexpectedMatch(xStart);
			}
		} else {
			return this.findUnexpectedMatch(xStart);
		}
	}
	
	private OCRChar findExpectedMatch(int xStart) throws Exception {
		LinkedList<OCRChar> list;
		OCRChar c;
		
		for (int x = xStart; x < xStart + this.whiteSpaceLeft; x++) {
			for (int y = this.topLine; y < this.topLine + this.expectedHeight; y++) {
				
				if (this.image.getRGB(x, y) == this.textColor) {
					list = this.charMap.get(x-xStart).get(y - this.topLine);
				
					for (int i = 0; i < list.size(); i++) {
						c = list.get(i);
						if (testMatch(c, xStart)) {
							this.x = x + c.nonInterferingZoneRight - c.xLeftTopPixel;
							return c;
						}
					}
					if (!this.isLeftGeneralInterferencePixel(x - xStart, y - this.topLine)) {
						String msg = "Failed to find match for non-interference pixel on expected match at x=" + xStart + " y=" + this.topLine + ".  ";
						this.logImage(msg, "unmatchedExpectedChar", false);
						throw new Exception(msg);
					}
				}				
			}
		}
		
		if (this.testMatch(this.space, xStart)) {
			this.x = xStart + this.space.nonInterferingZoneRight;
			return this.space;
		}
		String msg = "Failed to find match for non-interference pixel on expected match at x=" + xStart + " y=" + this.topLine + ".  ";
		this.logImage(msg, "unmatchedExpectedChar", false);
		throw new Exception(msg);
	}

	
	private OCRChar findUnexpectedMatch(int xStart) throws Exception {
		LinkedList<OCRChar> list;
		OCRChar c;
		
		for (int x = xStart; x < this.width; x++) {
			for (int y = this.topLine; y < this.topLine + this.expectedHeight; y++) {
				if (this.image.getRGB(x, y) == this.textColor) {
					
					// Try all whitespace offsets
					for (int whiteSpaceLeftLV = 0; whiteSpaceLeftLV < this.charMap.size(); whiteSpaceLeftLV++) {
						list = this.charMap.get(whiteSpaceLeftLV).get(y - this.topLine);
						
						for (int j = 0; j < list.size(); j++) {
							c = list.get(j);
							if (testMatch(c, x - whiteSpaceLeftLV)) {
								this.x = x + c.nonInterferingZoneRight - c.xLeftTopPixel;
								return c;
							}
						}
					}
				}				
			}
		}
		String msg = "Failed to find character on this line: " + this.topLine + " after this x: " + xStart;
		this.logImage(msg, "unmatchedCharOnLine", false);
		this.x = this.width;
		throw new Exception(msg);
	}
	
	/**
	 * Decides whether a given charIcon matches the designated location.
	 * 
	 * @param charIcon
	 * @param xStart
	 * @return
	 */
	private boolean testMatch(OCRChar chara, int xStart) {
		
		if (chara.imageWidth + xStart > this.width || chara.imageHeight + this.topLine > this.height) {
			return false; // icon doesn't fit
		}

		for (int x = 0; x < chara.imageWidth; x++) {
			for (int y = 0; y < chara.imageHeight; y++) {

				// Character pixels should have text color in image
				if (chara.isCharacterPixel(x, y)) {
					if (image.getRGB(xStart + x, this.topLine + y) != textColor) {
						return false;
					}
					// And non-interfering, non-character pixels should not have
					// text color in image
				} else if (!isInterferencePixel(chara, x, y)) {
					if (image.getRGB(xStart + x, this.topLine + y) == textColor) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Sets all matched character pixels to an off black color that hopefully is
	 * not a textColor.
	 * 
	 * @param charIcon
	 * @param image
	 * @param xStart
	 * @param yStart
	 * @param offset
	 */
	private void eraseCharAt(OCRChar chara, int xStart, int yStart, int offset) {
		int xImg = xStart - chara.xLeftTopPixel;
		int yImg = yStart - chara.yLeftTopPixel;

		for (int x = chara.nonInterferingZoneLeft; x < chara.nonInterferingZoneRight; x++) {
			for (int y = 0; y < chara.imageHeight; y++) {
				if (chara.isCharacterPixel(x, y) && !isRightInterferencePixel(chara, x, y)) {
					image.setRGB(xImg + x, yImg + y, nonTextColor);
				}
			}
		}
		try {
			if (DEBUG) {
				ImageIO.write(image, "png", new File("Errors/erasred" + chara.charName + ".png"));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean isInterferencePixel(OCRChar chara, int x, int y) {
		// In own interference zone
		if (this.isLeftInterferencePixel(chara, x, y) || this.isRightInterferencePixel(chara, x, y)) {
			return true;
		}
		return false;
	}

	private boolean isLeftInterferencePixel(OCRChar chara, int x, int y) {
		// In own interference zone
		if (x < chara.nonInterferingZoneLeft) {
			return true;
		}

		// In general interference zone
		int colsFromLeftOfInterferenceZone = x - chara.nonInterferingZoneLeft;
		if (colsFromLeftOfInterferenceZone < generalInterferenceZoneLeft.length
				&& generalInterferenceZoneLeft[colsFromLeftOfInterferenceZone][y] == 1) {
			return true;
		}

		return false;
	}
	
	private boolean isLeftGeneralInterferencePixel(int x, int y) {
		// In general interference zone
		if (generalInterferenceZoneLeft[x][y] == 1) {
			return true;
		}
		return false;
	}

	private boolean isRightInterferencePixel(OCRChar chara, int x, int y) {
		// In own interference zone
		if (x >= chara.nonInterferingZoneRight) {
			return true;
		}

		// In general interference zone
		int colsFromRightOfInterferenceZone = chara.nonInterferingZoneRight - x - 1;
		if (colsFromRightOfInterferenceZone < generalInterferenceZoneRight.length
				&& generalInterferenceZoneRight[colsFromRightOfInterferenceZone][y] == 1) {
			return true;
		}

		return false;
	}

	private boolean matchesTextColors(int color) {
		for (int i = 0; i < textColors.length; i++) {
			if (color == textColors[i]) {
				return true;
			}
		}
		return false;
	}
	
	private void logImage(String msg, String fileNamePrepend, boolean override) throws Exception {
		String additionalInfo = "";
		if (DEBUG || override) {
			try {
				String filename = this.errorFolder + "/" + fileNamePrepend + new Date().getTime() + ".png";
				ImageIO.write(image, "png", new File(filename));
				additionalInfo = "File saved to " + filename;
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.log(msg + additionalInfo, override);
		}
	}
	
	private void log(String msg, boolean override) {
		if (DEBUG || override) {
			try {
			    FileWriter out = new FileWriter(ERROR_LOG, true);
			    out.write(msg + System.lineSeparator());
			    out.close();
			} catch (IOException e) {
			    e.printStackTrace();
			}
		}
	}

	private void printCharMap() {
		LinkedList<LinkedList<OCRChar>> heightList;
		LinkedList<OCRChar> charList;
		for (int a = 0; a < this.charMap.size(); a++) {
			heightList = this.charMap.get(a);
			System.out.print("whitespace: " + a + "\n");
			for (int b = 0; b < heightList.size(); b++) {
				System.out.print("    height: " + b + "\n[");
				charList = heightList.get(b);
				
				for (int c = 0; c < charList.size(); c++) {
					if (c != 0) {
						System.out.print(", ");
					}
					System.out.print(charList.get(c).charName);
				}
				System.out.print("]\n");
			}
			System.out.println("]");
		}
	}
}
