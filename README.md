# DigitalItalicOpticalCharacterRecognition
An Optical Character Recognition tool for italic (and non-italic) fonts.

This tool was specifically built to handle italic/overlapping digital characters


This tool should work for you if:
- Characters are consistent to the pixel
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

#How to Set it up to Use Your Font
All the required configuration is done by populating your training folder.

###Training Folder Structure
View an example of the interior structure with example images loaded  [here](https://github.com/Sel-en-ium/DigitalItalicOpticalCharacterRecognition/tree/master/OcrTraining).  
If you are unsure how to format any of the files please look at the "Descriptions" section.

```java
--YourTrainingFolder
 |
 |--------ErrorLogs (Reserved Folder - Error logs will be saved here)
 |--------WillNotLoadFolder (Reserved Folder - For yor notes and things)
 |
 |--------PossibleTextColors.png
 |--------Space.png
 |--------Height.png
 |--------WhiteSpaceLeft.png
 |
 |--------CharacterFolder1 [Name unimportant, up to you]
 |       |------NameConfig.txt [Optional]
 |       |------All character images you wish to include
 |
 |--------CharacterFolder2 [Name unimportant, up to you]
 |       |------NameConfig.txt [Optional]
 |       |------All character images you wish to include
```

###Descriptions (WIP)
#####Space.png:
Should be line height (as all character images should be).  Should be the width that the space takes up.
#####CharacterFolder: 
Folder - you can make folders with your own names with the following structure, they are loaded dynamically.  They should contain all characters you wish to load [except for the space character].
#####NameConfig.txt: 
Used for translating image names to their respective character.  Optional, otherwise the character images must be named [character].png

