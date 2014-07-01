package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.*;
import java.awt.event.*;

/**
 *  This plugin implements the Analyze/Tools/Draw Scale Bar command. Divakar
 *  Ramachandran added options to draw a background and use a serif font on 23
 *  April 2006.
 *
 *@author     Thomas
 *@created    30 novembre 2007
 */
public class ScaleBar implements PlugIn {

	final static String[] locationKeys = {"CaliUpR", "CaliDoR", "CaliDoL", "CaliUpL", "CaliSel"};
	final static int UPPER_RIGHT = 0, LOWER_RIGHT = 1, LOWER_LEFT = 2, UPPER_LEFT = 3, AT_SELECTION = 4;
	final static String[] colorKeys = {"WhiteColor", "BlackColor", "LGrayColor", "GrayColor", "DGrayColor", "RedColor", "GreenColor", "BlueColor", "YellowColor"};
	final static String[] bcolorKeys = {"ColorNone", "BlackColor", "WhiteColor", "DGrayColor", "GrayColor", "LGrayColor", "YellowColor", "BlueColor", "GreenColor", "RedColor"};
	static String[] colors;
	static String[] locations;
	static String[] bcolors;

	final static String[] checkboxLabelsKeys = {"ScaleBarBold", "ScaleBarHide", "ScaleBarSerif"};
	static String[] checkboxLabels;
	static double barWidth;
	static int defaultBarHeight = 4;
	static int barHeightInPixels = defaultBarHeight;
	static String location;// = locations[LOWER_RIGHT];
	static String color;// = colors[0];
	static String bcolor;// = bcolors[0];
	static boolean boldText = true;
	static boolean hideText;
	static int defaultFontSize = 14;
	static int fontSize;
	static boolean labelAll;
	ImagePlus imp;
	double imageWidth;
	double mag;
	int xloc, yloc;
	int barWidthInPixels;
	int roiX = -1, roiY, roiWidth, roiHeight;
	boolean serifFont;
	boolean[] checkboxStates = new boolean[3];


	/**
	 *  Main processing method for the ScaleBar object
	 *
	 *@param  arg  Description of the Parameter
	 */
	public void run(String arg) {
		/*
		 *  EU_HOU CHANGES
		 */
		colors = new String[colorKeys.length];
		locations = new String[locationKeys.length];
		bcolors = new String[bcolorKeys.length];
		checkboxLabels = new String[checkboxLabelsKeys.length];
		for (int i = 0; i < colorKeys.length; ++i) {
			//EU_HOU Bundle
			colors[i] = IJ.getColorBundle().getString(colorKeys[i]);
		}
		for (int i = 0; i < locationKeys.length; ++i) {
			//EU_HOU Bundle
			locations[i] = IJ.getPluginBundle().getString(locationKeys[i]);
		}
		for (int i = 0; i < bcolorKeys.length; ++i) {
			//EU_HOU Bundle
			bcolors[i] = IJ.getColorBundle().getString(bcolorKeys[i]);
		}
		for (int i = 0; i < checkboxLabelsKeys.length; ++i) {
			//EU_HOU Bundle
			checkboxLabels[i] = IJ.getPluginBundle().getString(checkboxLabelsKeys[i]);
		}

		location = new String(locations[1]);
		color = new String(colors[0]);
		bcolor = new String(bcolors[0]);
		/*
		 *  EU_HOU END
		 */
		imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (showDialog(imp) && imp.getStackSize() > 1 && labelAll) {
				labelSlices(imp);
			}
		} else {
			IJ.noImage();
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	void labelSlices(ImagePlus imp) {
	int slice = imp.getCurrentSlice();
		for (int i = 1; i <= imp.getStackSize(); i++) {
			imp.setSlice(i);
			drawScaleBar(imp);
		}
		imp.setSlice(slice);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 *@return      Description of the Return Value
	 */
	boolean showDialog(ImagePlus imp) {
	Roi roi = imp.getRoi();
		if (roi != null) {
		Rectangle r = roi.getBounds();
			roiX = r.x;
			roiY = r.y;
			roiWidth = r.width;
			roiHeight = r.height;
			location = locations[AT_SELECTION];
		} else if (location.equals(locations[AT_SELECTION])) {
			location = locations[UPPER_RIGHT];
		}

	Calibration cal = imp.getCalibration();
	ImageWindow win = imp.getWindow();
		mag = (win != null) ? win.getCanvas().getMagnification() : 1.0;
		if (mag > 1.0) {
			mag = 1.0;
		}
		if (fontSize < (defaultFontSize / mag)) {
			fontSize = (int) (defaultFontSize / mag);
		}
	String units = cal.getUnits();
		// Handle Digital Micrograph unit microns
		if (units.equals("micron")) {
			units = IJ.micronSymbol + "m";
		}
	double pixelWidth = cal.pixelWidth;
		if (pixelWidth == 0.0) {
			pixelWidth = 1.0;
		}
	double scale = 1.0 / pixelWidth;
		imageWidth = imp.getWidth() * pixelWidth;
		if (roiX > 0 && roiWidth > 10) {
			barWidth = roiWidth * pixelWidth;
		} else if (barWidth == 0.0 || barWidth > 0.67 * imageWidth) {
			barWidth = (80.0 * pixelWidth) / mag;
			if (barWidth > 0.67 * imageWidth) {
				barWidth = 0.67 * imageWidth;
			}
			if (barWidth > 5.0) {
				barWidth = (int) barWidth;
			}
		}
	int stackSize = imp.getStackSize();
	int digits = (int) barWidth == barWidth ? 0 : 1;
		if (barWidth < 1.0) {
			digits = 2;
		}
	int percent = (int) (barWidth * 100.0 / imageWidth);
		if (mag < 1.0 && barHeightInPixels < defaultBarHeight / mag) {
			barHeightInPixels = (int) (defaultBarHeight / mag);
		}
		imp.getProcessor().snapshot();
		if (!IJ.macroRunning()) {
			updateScalebar();
		}
	//EU_HOU Bundle
	GenericDialog gd = new BarDialog(IJ.getPluginBundle().getString("ScalerTitle"));
		gd.addNumericField(IJ.getPluginBundle().getString("ScaleBarWidth") + " " + units + ": ", barWidth, digits);
		gd.addNumericField(IJ.getPluginBundle().getString("ScaleBarHeight") + units + ": ", barHeightInPixels, 0);
		gd.addNumericField(IJ.getPluginBundle().getString("ScaleBarFont") + ": ", fontSize, 0);
		gd.addChoice(IJ.getPluginBundle().getString("ScaleBarColor") + ": ", colors, color);
		gd.addChoice(IJ.getPluginBundle().getString("ScaleBarBackground") + ": ", bcolors, bcolor);
		gd.addChoice(IJ.getPluginBundle().getString("ScaleBarLocation") + ": ", locations, location);
		checkboxStates[0] = boldText;
		checkboxStates[1] = hideText;
		checkboxStates[2] = serifFont;
		gd.addCheckboxGroup(2, 2, checkboxLabels, checkboxStates);
		if (stackSize > 1) {
			//EU_HOU Bundle
			gd.addCheckbox("Label all Slices", labelAll);
		}
		gd.showDialog();
		if (gd.wasCanceled()) {
			imp.getProcessor().reset();
			imp.updateAndDraw();
			return false;
		}
		barWidth = gd.getNextNumber();
		barHeightInPixels = (int) gd.getNextNumber();
		fontSize = (int) gd.getNextNumber();
		color = gd.getNextChoice();
		bcolor = gd.getNextChoice();
		location = gd.getNextChoice();
		boldText = gd.getNextBoolean();
		hideText = gd.getNextBoolean();
		serifFont = gd.getNextBoolean();
		if (stackSize > 1) {
			labelAll = gd.getNextBoolean();
		}
		if (IJ.macroRunning()) {
			updateScalebar();
		}
		return true;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	void drawScaleBar(ImagePlus imp) {
		if (!updateLocation()) {
			return;
		}
	ImageProcessor ip = imp.getProcessor();
		Undo.setup(Undo.FILTER, imp);
	Color color = getColor();
	Color bcolor = getBColor();
	//if (!(color==Color.white || color==Color.black)) {
	//    ip = ip.convertToRGB();
	//    imp.setProcessor(null, ip);
	//}

	int x = xloc;
	int y = yloc;
	int fontType = boldText ? Font.BOLD : Font.PLAIN;
	String font = serifFont ? "Serif" : "SanSerif";
		ip.setFont(new Font(font, fontType, fontSize));
		ip.setAntialiasedText(true);
	int digits = (int) barWidth == barWidth ? 0 : 1;
		if (barWidth < 1.0) {
			digits = 1;
		}
	// Handle Digital Micrograph unit microns
	String units = imp.getCalibration().getUnits();
		if (units.equals("microns")) {
			units = IJ.micronSymbol + "m";
		}
	String label = IJ.d2s(barWidth, digits) + " " + units;
	int swidth = hideText ? 0 : ip.getStringWidth(label);
	int xoffset = (barWidthInPixels - swidth) / 2;
	int yoffset = barHeightInPixels + (hideText ? 0 : fontSize + fontSize / (serifFont ? 8 : 4));

		// Draw bkgnd box first,  based on bar width and height (and font size if hideText is not checked)
		if (bcolor != null) {
		int w = barWidthInPixels;
		int h = yoffset;
			if (w < swidth) {
				w = swidth;
			}
		int x2 = x;
			if (x + xoffset < x2) {
				x2 = x + xoffset;
			}
		int margin = w / 20;
			if (margin < 2) {
				margin = 2;
			}
			x2 -= margin;
		int y2 = y - margin;
			w = w + margin * 2;
			h = h + margin * 2;
			ip.setColor(bcolor);
			ip.setRoi(x2, y2, w, h);
			ip.fill();
		}

		ip.resetRoi();
		ip.setColor(color);
		ip.setRoi(x, y, barWidthInPixels, barHeightInPixels);
		ip.fill();
		ip.resetRoi();
		if (!hideText) {
			ip.drawString(label, x + xoffset, y + yoffset);
		}
		imp.updateAndDraw();
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
	boolean updateLocation() {
	Calibration cal = imp.getCalibration();
		barWidthInPixels = (int) (barWidth / cal.pixelWidth);
	int width = imp.getWidth();
	int height = imp.getHeight();
	int fraction = 20;
	int x = width - width / fraction - barWidthInPixels;
	int y = 0;
		if (location.equals(locations[UPPER_RIGHT])) {
			y = height / fraction;
		} else if (location.equals(locations[LOWER_RIGHT])) {
			y = height - height / fraction - barHeightInPixels - fontSize;
		} else if (location.equals(locations[UPPER_LEFT])) {
			x = width / fraction;
			y = height / fraction;
		} else if (location.equals(locations[LOWER_LEFT])) {
			x = width / fraction;
			y = height - height / fraction - barHeightInPixels - fontSize;
		} else {
			if (roiX == -1) {
				return false;
			}
			x = roiX;
			y = roiY;
		}
		xloc = x;
		yloc = y;
		return true;
	}


	/**
	 *  Gets the color attribute of the ScaleBar object
	 *
	 *@return    The color value
	 */
	Color getColor() {
	Color c = Color.black;
		if (color.equals(colors[0])) {
			c = Color.white;
		} else if (color.equals(colors[2])) {
			c = Color.lightGray;
		} else if (color.equals(colors[3])) {
			c = Color.gray;
		} else if (color.equals(colors[4])) {
			c = Color.darkGray;
		} else if (color.equals(colors[5])) {
			c = Color.red;
		} else if (color.equals(colors[6])) {
			c = Color.green;
		} else if (color.equals(colors[7])) {
			c = Color.blue;
		} else if (color.equals(colors[8])) {
			c = Color.yellow;
		}
		return c;
	}

	// Div., mimic getColor to write getBColor for bkgnd
	/**
	 *  Gets the bColor attribute of the ScaleBar object
	 *
	 *@return    The bColor value
	 */
	Color getBColor() {
		if (bcolor == null || bcolor.equals(bcolors[0])) {
			return null;
		}
	Color bc = Color.white;
		if (bcolor.equals(bcolors[1])) {
			bc = Color.black;
		} else if (bcolor.equals(bcolors[3])) {
			bc = Color.darkGray;
		} else if (bcolor.equals(bcolors[4])) {
			bc = Color.gray;
		} else if (bcolor.equals(bcolors[5])) {
			bc = Color.lightGray;
		} else if (bcolor.equals(bcolors[6])) {
			bc = Color.yellow;
		} else if (bcolor.equals(bcolors[7])) {
			bc = Color.blue;
		} else if (bcolor.equals(bcolors[8])) {
			bc = Color.green;
		} else if (bcolor.equals(bcolors[9])) {
			bc = Color.red;
		}
		return bc;
	}


	/**
	 *  Description of the Method
	 */
	void updateScalebar() {
		updateLocation();
		imp.getProcessor().reset();
		drawScaleBar(imp);
	}


	/**
	 *  Description of the Class
	 *
	 *@author     Thomas
	 *@created    30 novembre 2007
	 */
	class BarDialog extends GenericDialog {

		/**
		 *  Constructor for the BarDialog object
		 *
		 *@param  title  Description of the Parameter
		 */
		BarDialog(String title) {
			super(title);
		}


		/**
		 *  Description of the Method
		 *
		 *@param  e  Description of the Parameter
		 */
		public void textValueChanged(TextEvent e) {
		TextField widthField = ((TextField) numberField.elementAt(0));
		Double d = getValue(widthField.getText());
			if (d == null) {
				return;
			}
			barWidth = d.doubleValue();
		TextField heightField = ((TextField) numberField.elementAt(1));
			d = getValue(heightField.getText());
			if (d == null) {
				return;
			}
			barHeightInPixels = (int) d.doubleValue();
		TextField fontSizeField = ((TextField) numberField.elementAt(2));
			d = getValue(fontSizeField.getText());
			if (d == null) {
				return;
			}
		int size = (int) d.doubleValue();
			if (size > 5) {
				fontSize = size;
			}
			updateScalebar();
		}


		/**
		 *  Description of the Method
		 *
		 *@param  e  Description of the Parameter
		 */
		public void itemStateChanged(ItemEvent e) {
		Choice col = (Choice) (choice.elementAt(0));
			color = col.getSelectedItem();
		Choice bcol = (Choice) (choice.elementAt(1));
			bcolor = bcol.getSelectedItem();
		Choice loc = (Choice) (choice.elementAt(2));
			location = loc.getSelectedItem();
			boldText = ((Checkbox) (checkbox.elementAt(0))).getState();
			hideText = ((Checkbox) (checkbox.elementAt(1))).getState();
			serifFont = ((Checkbox) (checkbox.elementAt(2))).getState();
			updateScalebar();
		}

	}//BarDialog inner class

}

