package italicDigitalOCR;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;


public class OCRChar
{	
	/* Constants */
	
	private static int BLACK = -16777216; // Non interfering character pixel
	private static int WHITE = -1; // Non interfering whitespace
	private static int RED = -1237980; // Interfering character pixel
	private static int GREY = -3947581; // Interfering whitespace
	
	/* Class Properties */
	
	BufferedImage image;
	char charName;
	int imageWidth;
	int imageHeight;
	int xLeftTopPixel;
	int yLeftTopPixel;
	int xTopLeftPixel;
	int yTopLeftPixel;
	int nonInterferingZoneLeft; // Inclusive
	int nonInterferingZoneRight; // Exclusive (aka, pixel where interfering starts [or the width])
	int numCharacterPixels;
	int charHeight;
	int whiteSpaceLeft;

	public OCRChar(BufferedImage image, char charName) throws Exception {
		this.image = image;
		this.charName = charName;
		this.imageWidth = image.getWidth();
		this.imageHeight = image.getHeight();
		this.xLeftTopPixel = 0;
		this.yLeftTopPixel = 0;
		this.xTopLeftPixel = 0;
		this.yTopLeftPixel = 0;
		this.nonInterferingZoneLeft = 0;
		this.nonInterferingZoneRight = this.imageWidth;
		this.numCharacterPixels = 0;
		this.charHeight = this.imageHeight;
		this.whiteSpaceLeft = this.imageWidth;
	}
	
	public OCRChar(String file) throws Exception {
		this.image = ImageIO.read(new File(file));
		
		this.charName = getCharName(file, null);
		
		this.imageWidth = image.getWidth();
		this.imageHeight = image.getHeight();
		this.setNonInterferingZone();
		this.setLeftTopPixel();
		this.setTopLeftPixel();
		this.setNumCharacterPixels();
		this.setCharHeight();
		this.setWhiteSpaceLeft();
	}
	
	public OCRChar(String file, HashMap<String, Character> nameConfig) throws Exception {
		this.image = ImageIO.read(new File(file));
		
		this.charName = getCharName(file, nameConfig);
		
		this.imageWidth = image.getWidth();
		this.imageHeight = image.getHeight();
		this.setNonInterferingZone();
		this.setLeftTopPixel();
		this.setTopLeftPixel();
		this.setNumCharacterPixels();
		this.setCharHeight();
		this.setWhiteSpaceLeft();
	}
	
	public char getCharName(String file, HashMap<String, Character> nameConfig) throws Exception {
		String name = file.substring(0, file.indexOf(".png"));
		name = name.substring(name.lastIndexOf(File.separator)+1);
		
		if (nameConfig != null) {
			return nameConfig.get(name);
		} else {
			if (name.length() > 1) {
				throw new Exception("Failed to get charName.");
			} else {
				return name.charAt(0);
			}
		}
	}
	
	private void setNonInterferingZone() {
		int width = this.image.getWidth();
		
		int x = 0;
		while (isInterferingPixel(x, 0) && x < width) {
			x++;
		}
		this.nonInterferingZoneLeft = x;
		
		x = width;
		while (isInterferingPixel(x-1, 0) && x-1 > 0) {
			x--;
		}
		this.nonInterferingZoneRight = x;
	}
	
	private void setLeftTopPixel() throws Exception {		
		for (int x = this.nonInterferingZoneLeft; x < imageWidth; x++) {
			for (int y = 0; y < imageHeight; y++) {
				if (isCharacterPixel(x, y)) {
					this.xLeftTopPixel = x;
					this.yLeftTopPixel = y;
					return;
				}
			}
		}
		throw new Exception("Could not find the left top pixel for " + this.charName);
	}
	
	private void setTopLeftPixel() throws Exception {		
		for (int y = 0; y < imageHeight; y++) {
			for (int x = this.nonInterferingZoneLeft; x < imageWidth; x++) {
				if (isCharacterPixel(x, y)) {
					this.xTopLeftPixel = x;
					this.yTopLeftPixel = y;
					return;
				}
			}
		}
		throw new Exception("Could not find the left top pixel for " + this.charName);
	}
	
	private void setNumCharacterPixels() {
		int num = 0;
		for (int x = 0; x < imageWidth; x++) {
			for (int y = 0; y < imageHeight; y++) {
				if (isCharacterPixel(x, y)) {
					num++;
				}
			}
		}
		this.numCharacterPixels = num;
	}

	private void setWhiteSpaceLeft() {
		int whiteSpace = 0;
		for (int x = this.nonInterferingZoneLeft; x < this.nonInterferingZoneRight; x++) {
			for (int y = 0; y < imageHeight; y++) {
				if (isCharacterPixel(x, y)) {
					this.whiteSpaceLeft = whiteSpace;
					return;
				}
			}
			whiteSpace++;
		}
		this.whiteSpaceLeft = whiteSpace;
	}
	
	private void setCharHeight() {
		this.charHeight = this.getBottomCharacterPixel() - this.getTopCharacterPixel();
	}
	
	private int getTopCharacterPixel() {
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				if (isCharacterPixel(x, y)) {
					return y;
				}
			}
		}
		return 0;
	}
	
	private int getBottomCharacterPixel() {
		for (int y = this.imageHeight; y > 0; y--) {
			for (int x = 0; x < imageWidth; x++) {
				if (isCharacterPixel(x, y-1)) {
					return y;
				}
			}
		}
		return 0;
	}
	
	public boolean isCharacterPixel(int x, int y) {
		if (!this.inBounds(x, y)) {
			return false;
		}
		int pixelColor = this.image.getRGB(x, y);
		if (pixelColor == BLACK || pixelColor == RED) {
			return true;
		}
		return false;
	}
	
	public boolean isInterferingPixel(int x, int y) {
		if (!this.inBounds(x, y)) {
			return false;
		}
		int pixelColor = this.image.getRGB(x, y);
		if (pixelColor == RED || pixelColor == GREY) {
			return true;
		}
		return false;
	}
	
	private boolean inBounds(int x, int y) {
		if (x < 0 || x >= this.imageWidth) {
			return false;
		}
		if (y < 0 || y >= this.imageHeight) {
			return false;
		}
		return true;
	}
}
