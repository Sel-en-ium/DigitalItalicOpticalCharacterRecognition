# DigitalItalicOpticalCharacterRecognition
An Optical Character Recognition tool for italic (and non-italic) fonts.

This tool was specifically built to handle italic/overlapping digital characters


This tool should work for you if:
- Characters are consistant to the pixel
- Characters appear on a perfect horizontal line
- The text color of individual lines of characters is consistent
- The expected possible colors of the text do not appear often in the background colors

Here is [example](https://github.com/Sel-en-ium/DigitalItalicOpticalCharacterRecognition/blob/master/ExampleImage.png) image that the tool can successfully handle. 


# Usage
```java
// Create the reader
OCRReader reader = new OCRReader(StringToYourTrainingFolder);
// Get the image you want to read from
BufferedImage image = ImageIO.read(new File(StringToTheImageFileYouWantToReadFrom));

// Read all lines in the image:
String lines = reader.readLines(image);
System.out.println(lines);

// You can also choose to just read the first line:
String line = reader.readLine(image);
System.out.println(line);
```
