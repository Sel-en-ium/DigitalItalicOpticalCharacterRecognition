# DigitalItalicOpticalCharacterRecognition
An Optical Character Recognition tool for italic (and non-italic) fonts.

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
