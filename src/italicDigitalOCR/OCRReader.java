package italicDigitalOCR;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

public class OCRReader {
	
	/* Configurations Constants */
	private static boolean DEBUG = false;
	private static boolean TRAINING = true;
	private static String ERROR_LOG = "OcrErrorLog.txt";
	private static String CHAR_NAME_LIST = "NameConfig.txt";
	private static String ERROR_FOLDER = "ErrorLogs";
	private static String NO_LOAD_FOLDER = "WillNotLoadFolder";

	/* Initialization variables */
	private String TRAINING_FOLDER;
	
	/* Configuration Chars and other stuff from training folder*/
	private OCRChar space;
	
	private int expectedHeight;
	private int whiteSpaceLeft = 0; // The longest x distance of whitespace in a char image before we hit a pixel.  (The distance is the first pixel.)
	
	private LinkedList<Integer> textColors;
	private LinkedList<boolean[]> generalInterferencePixelsLeft = new LinkedList<boolean[]>();
	private LinkedList<boolean[]> generalInterferencePixelsRight = new LinkedList<boolean[]>();
	
	/* Object variables for current progress*/
	
	// 1st index = whiteSpaceLeft, 2nd index = yLeftTopCharPixel, 3rd = ordered by numPixels(largest), height(largest) (maybe add width)
	private LinkedList<LinkedList<LinkedList<OCRChar>>> charMap;   
	private BufferedImage image;
	private int height;
	private int width;
	
	private int topLine;
	private int textColor;
	private int x = 0;
	private OCRChar prevChar;

	/*
	 * For testing
	 */
	public static void main(String[] args) {
		try {
			OCRReader reader = new OCRReader("FontPopUp");
			reader.printCharMap();
			BufferedImage image = ImageIO.read(new File("ExampleImage.png"));
			String readin = reader.readLines(image);
			System.out.println("SUCCESS LOL!  Well here it is: " + System.lineSeparator() + readin);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public OCRReader(String trainingFolder) throws Exception {
		this.TRAINING_FOLDER = trainingFolder;

		this.loadTrainingfFolder();
	}
	
	private void loadTrainingfFolder() throws Exception {
		this.textColors = this.getPossibleTextColors("PossibleTextColors.png");
		this.space = new OCRChar(this.getTrainingImage("Space.png"), " ");
		this.expectedHeight = this.space.imageHeight;

		LinkedList<OCRChar> allChars = new LinkedList<OCRChar>();
		this.getAllChars(allChars);
		this.initInterferenceArrays(allChars);
		this.initWhiteSpaceLeft(allChars);
		this.populateCharMap(allChars);	
	}
	
	private BufferedImage getTrainingImage(String fileName) throws IOException {
		return ImageIO.read(new File(this.TRAINING_FOLDER + File.separator + fileName));
	}
	
	private LinkedList<Integer> getPossibleTextColors(String fileName) throws IOException {
		LinkedList<Integer> colors = new LinkedList<Integer>();
		BufferedImage img = this.getTrainingImage(fileName);
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				colors.add(img.getRGB(x, y));
			}
		}
		return colors;
	}
	
	private void initInterferenceArrays(LinkedList<OCRChar> allChars) throws Exception {
		boolean[] temp;

		int leftSize = this.generalInterferencePixelsLeft.size();
		int rightSize = this.generalInterferencePixelsRight.size();
		
		for (OCRChar chara : allChars) {
		
			if (chara.image.getHeight() != this.expectedHeight) {
				throw new Exception("Unexpected for char, " + chara.charName + ", should be " + this.expectedHeight + "px.");
			}
			
			// Ensure array is large enough
			while (chara.nonInterferingZoneLeft > rightSize) {
				this.generalInterferencePixelsRight.add(new boolean[this.expectedHeight]);
				rightSize++;
			}

			// Ensure array is large enough
			while (chara.imageWidth - chara.nonInterferingZoneRight > leftSize) {
				this.generalInterferencePixelsLeft.add(new boolean[this.expectedHeight]);
				leftSize++;
			}
			
			for (int x = 0; x < chara.nonInterferingZoneLeft; x++) {
				temp = this.generalInterferencePixelsRight.get(x);
				for (int y = 0; y < this.expectedHeight; y++) {
					if (chara.image.getRGB(x, y) == OCRChar.RED) {
						temp[y] = true;
					}
				}
			}
			
			for (int x = chara.imageWidth; x > chara.nonInterferingZoneRight; x--) {
				temp = this.generalInterferencePixelsLeft.get(chara.imageWidth - x);
				for (int y = 0; y < this.expectedHeight; y++) {
					if (chara.image.getRGB(x-1, y) == OCRChar.RED) {
						temp[y] = true;
					}
				}
			}
		}
	}
	
	private void initWhiteSpaceLeft(LinkedList<OCRChar> allChars) {
		for (OCRChar chara : allChars) {
			if (chara.whiteSpaceLeft > this.whiteSpaceLeft) {
				this.whiteSpaceLeft = chara.whiteSpaceLeft;
			}
		}
		this.whiteSpaceLeft++;
	}
	
	private void populateCharMap(List<OCRChar> allChars) {
		LinkedList<LinkedList<OCRChar>> heightList;
		LinkedList<OCRChar> charList;
		
		// Initialize a list for each whitespace distance
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
		HashMap<String, String> nameConfig = null;
		
		// Iterates through all files in the directory
		DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(this.TRAINING_FOLDER));
		for (Path currentEntity : stream) {

			// We will only be looking at files inside of directories
			String folder = currentEntity.getFileName().toString();
			if (Files.isDirectory(currentEntity) && !folder.equals(ERROR_FOLDER) && !folder.equals(NO_LOAD_FOLDER)) {
				try {
					nameConfig = getNameConfig(currentEntity.toString());
				} catch (Exception e) {
					// No name config file
					nameConfig = null;
				}
				
				DirectoryStream<Path> subStream = Files.newDirectoryStream(currentEntity);
				
				for (Path subFile : subStream) {
					String fileName = subFile.toString();
					if (fileName.toLowerCase().contains(".png")) {
						addCharToList(list, fileName, nameConfig);
					}
				}
			}
		}
	}
	
	private HashMap<String, String> getNameConfig(String folder) throws Exception {
		HashMap<String, String> config = new HashMap<String, String>();
		File file = new File(folder + "/" + CHAR_NAME_LIST);
		if (file.exists()) {
			// load the config file in
			String[] entry;
			BufferedReader br = new BufferedReader(new FileReader(file));
		 
			String line = null;
			while ((line = br.readLine()) != null) {
				entry = line.split(",");
				config.put(entry[0], entry[1]);
			}
			br.close();

			return config;
		}
		throw new Exception("No config File");
	}

	private void addCharToList(List<OCRChar> list, String fileName, HashMap<String, String> nameConfig) {
		try {
			OCRChar c;
			c = new OCRChar(fileName, nameConfig);
			if (c.imageHeight != this.expectedHeight) {
				throw new Exception("Character " + fileName + " is unexpected height.");
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
		
		String result = "";
		try {
			this.topLine = 0;
			while (this.topLine <= this.height - this.expectedHeight) {
				result += this.readLine(this.topLine) + System.lineSeparator();
				this.topLine += this.expectedHeight;
			}
		} catch (Exception e) {
			// Reached End of Lines hopefully.
			if (DEBUG) {
				System.out.println(e);
				e.printStackTrace();
			}
		}
		return result.trim();
	}

	public String readLine(BufferedImage image) throws Exception {
		this.image = image;
		this.height = image.getHeight();
		this.width = image.getWidth();
		this.topLine = 0;
		return this.readLine(this.topLine);
	}

	private String readLine(int topLineSearchStart) throws Exception {
		
		// If an exception is thrown from here that means there is no line to find (hopefully :D)
		this.textColor = this.getFirstColor(topLineSearchStart);
		this.topLine = this.getNextTopLine(this.textColor);

		String result = "";
		boolean expectedChar = false;
		this.x = 0;
		while (this.x < this.width) {
			try {
				this.prevChar = getOCRChar(this.x, expectedChar);
				result += this.prevChar.charName;
				expectedChar = true;
			} catch (Exception e) {
				// Should mean we've finished reading the line.
				result = result.trim();
			}
		}
		return result;
	}

	private int getNextTopLine(int textColor) throws Exception {
		int yStart = this.topLine;
	
		for (int x = 0; x < this.width; x++) {
			for (int y = yStart; y <= yStart + this.expectedHeight && y < this.height; y++) {
	
				// Found a character pixel, try to match it
				if (image.getRGB(x, y) == textColor) {

					// Iterate through all characters to find a match (Check highest heights first)
					LinkedList<OCRChar> charList;
					OCRChar chara;
					for (int heightLv = 0; heightLv < this.expectedHeight; heightLv++) {
						for (int whiteSpaceLv = 0; whiteSpaceLv < this.charMap.size(); whiteSpaceLv++) {
							charList = this.charMap.get(whiteSpaceLv).get(heightLv);
							
							for (int c = 0; c < charList.size(); c++) {
								chara = charList.get(c);
									
								this.topLine = y - chara.yLeftTopPixel;
								if (this.testMatch(chara, x - chara.xLeftTopPixel)) {
									return this.topLine;
								}
							}
						}
					}
				}
			}
		}
		
		if (yStart + this.expectedHeight <= this.height) {
			this.topLine = yStart + this.expectedHeight;
			return this.getNextTopLine(textColor);
		}

		throw new Exception("Could not find the top line.");
	}

	private int getFirstColor(int startY) throws Exception {

		for (int y = startY; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				if (matchesTextColors(image.getRGB(x, y))) {
					this.topLine = y;
					return image.getRGB(x, y);
				}
			}
		}

		throw new Exception("Could not find any known text color.");
	}

	/**
	 * Delegates to expected or unexpected match.
	 * 
	 * @param xStart
	 * @return
	 * @throws Exception
	 */
	private OCRChar getOCRChar(int xStart, boolean expectedChar) throws Exception {
		
		if (expectedChar) {
			try {
				return this.findExpectedMatch(xStart);
			} catch (Exception e) {
				// We either hit the end of line or may have encountered an unknown character, the result may be off
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
					// We may have found an unidentified character If the found character pixel is not a character interference pixel from the previous char
					if (!this.prevChar.isCharacterPixel(x - xStart + this.prevChar.nonInterferingZoneRight, y - this.topLine) && this.prevChar.isInterferingPixel(x - xStart  + this.prevChar.nonInterferingZoneRight, y - this.topLine)) {
						String msg = "Failed to find match for non-interference pixel on expected match at x=" + xStart + " y=" + this.topLine + ".  ";
						this.logImage(msg, "unmatchedExpectedChar", TRAINING);
						throw new Exception(msg);
					}
				}				
			}
		}
		
		if (this.testMatch(this.space, xStart)) {
			this.x = xStart + this.space.nonInterferingZoneRight;
			return this.space;
		}
		throw new Exception("EndOfLine");
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
		String msg = "Failed to find character on this line: " + this.topLine + " after this x: " + xStart + ".  ";
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
		xStart -= chara.nonInterferingZoneLeft; // To handle chars with left-side interference zones
		
		if (xStart < 0 || chara.imageWidth + xStart > this.width || this.topLine < 0 || chara.imageHeight + this.topLine > this.height) {
			return false; // icon doesn't fit
		}

		for (int x = 0; x < chara.imageWidth; x++) {
			for (int y = 0; y < chara.imageHeight; y++) {

				// Character pixels should have text color in image
				if (chara.isCharacterPixel(x, y)) {
					if (this.image.getRGB(xStart + x, this.topLine + y) != textColor) {
						return false;
					}
					// And non-interfering, non-character pixels should not have
					// text color in image
				} else if (!isInterferencePixel(chara, x, y)) {
					if (this.image.getRGB(xStart + x, this.topLine + y) == textColor) {
						return false;
					}
				}
			}
		}
		return true;
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
		if (colsFromLeftOfInterferenceZone < this.generalInterferencePixelsLeft.size()
				&& this.generalInterferencePixelsLeft.get(colsFromLeftOfInterferenceZone)[y]) {
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
		if (colsFromRightOfInterferenceZone < this.generalInterferencePixelsRight.size()
				&& this.generalInterferencePixelsRight.get(colsFromRightOfInterferenceZone)[y]) {
			return true;
		}

		return false;
	}

	private boolean matchesTextColors(int color) {
		for (int textColor : this.textColors) {
			if (color == textColor) {
				return true;
			}
		}
		return false;
	}
	
	private void logImage(String msg, String fileNamePrepend, boolean override) throws Exception {
		String additionalInfo = "";
		if (DEBUG || override) {
			try {
				String filename = this.TRAINING_FOLDER + File.separator + ERROR_FOLDER + File.separator + fileNamePrepend + new Date().getTime() + ".png";
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
			    FileWriter out = new FileWriter(this.TRAINING_FOLDER + File.separator + ERROR_FOLDER + File.separator + ERROR_LOG, true);
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
