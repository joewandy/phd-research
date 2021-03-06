/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * 2D visualizer's bottom panel
 */
class TwoDBottomPanel extends JPanel implements TreeModelListener,
	ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private JComboBox peakListSelector;
    private JComboBox thresholdCombo;
    private JTextField peakTextField;
    private PeakThresholdParameter thresholdSettings;

    private TwoDVisualizerWindow masterFrame;
    private RawDataFile dataFile;

    TwoDBottomPanel(TwoDVisualizerWindow masterFrame, RawDataFile dataFile,
	    ParameterSet parameters) {

	this.dataFile = dataFile;
	this.masterFrame = masterFrame;

	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

	setBackground(Color.white);
	setBorder(new EmptyBorder(5, 5, 5, 0));

	add(Box.createHorizontalGlue());

	GUIUtils.addLabel(this, "Show: ", SwingConstants.RIGHT);

	thresholdCombo = new JComboBox(PeakThresholdMode.values());
	thresholdCombo.setBackground(Color.white);
	thresholdCombo.setFont(smallFont);
	thresholdCombo.addActionListener(this);
	add(thresholdCombo);

	JPanel peakThresholdPanel = new JPanel();
	peakThresholdPanel.setBackground(Color.white);
	peakThresholdPanel.setLayout(new BoxLayout(peakThresholdPanel,
		BoxLayout.X_AXIS));

	GUIUtils.addLabel(peakThresholdPanel, "Value: ", SwingConstants.RIGHT);

	peakTextField = new JTextField();
	peakTextField.setPreferredSize(new Dimension(50, 15));
	peakTextField.setFont(smallFont);
	peakTextField.addActionListener(this);
	peakThresholdPanel.add(peakTextField);
	add(peakThresholdPanel);

	GUIUtils.addLabel(this, " from peak list: ", SwingConstants.RIGHT);

	peakListSelector = new JComboBox();
	peakListSelector.setBackground(Color.white);
	peakListSelector.setFont(smallFont);
	peakListSelector.addActionListener(masterFrame);
	peakListSelector.setActionCommand("PEAKLIST_CHANGE");
	add(peakListSelector);

	thresholdSettings = parameters
		.getParameter(TwoDParameters.peakThresholdSettings);

	thresholdCombo.setSelectedItem(thresholdSettings.getMode());

	add(Box.createHorizontalStrut(10));

	add(Box.createHorizontalGlue());

    }

    /**
     * Returns a peak list different peaks depending on the selected option of
     * the "peak Threshold" combo box
     */
    PeakList getPeaksInThreshold() {

	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	PeakThresholdMode mode = (PeakThresholdMode) thresholdCombo
		.getSelectedItem();

	switch (mode) {
	case ABOVE_INTENSITY_PEAKS:
	    double threshold = thresholdSettings.getIntensityThreshold();
	    return getIntensityThresholdPeakList(threshold);

	case ALL_PEAKS:
	    return selectedPeakList;
	case TOP_PEAKS:
	case TOP_PEAKS_AREA:
	    int topPeaks = thresholdSettings.getTopPeaksThreshold();
	    return getTopThresholdPeakList(topPeaks);
	}

	return null;
    }

    /**
     * Returns a peak list with the peaks which intensity is above the parameter
     * "intensity"
     */
    PeakList getIntensityThresholdPeakList(double intensity) {
	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	if (selectedPeakList == null)
	    return null;
	SimplePeakList newList = new SimplePeakList(selectedPeakList.getName(),
		selectedPeakList.getRawDataFiles());

	for (PeakListRow peakRow : selectedPeakList.getRows()) {
	    ChromatographicPeak peak = peakRow.getPeak(dataFile);
	    if (peak == null)
		continue;
	    if (peak.getRawDataPointsIntensityRange().getMax() > intensity) {
		newList.addRow(peakRow);
	    }
	}
	return newList;
    }

    /**
     * Returns a peak list with the top peaks defined by the parameter
     * "threshold"
     */
    PeakList getTopThresholdPeakList(int threshold) {

	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	if (selectedPeakList == null)
	    return null;
	SimplePeakList newList = new SimplePeakList(selectedPeakList.getName(),
		selectedPeakList.getRawDataFiles());

	Vector<PeakListRow> peakRows = new Vector<PeakListRow>();

	Range mzRange = selectedPeakList.getRowsMZRange();
	Range rtRange = selectedPeakList.getRowsRTRange();

	PeakThresholdMode selectedPeakOption = (PeakThresholdMode) thresholdCombo
		.getSelectedItem();
	if (selectedPeakOption == PeakThresholdMode.TOP_PEAKS_AREA) {
	    mzRange = masterFrame.getPlot().getXYPlot().getAxisRange();
	    rtRange = masterFrame.getPlot().getXYPlot().getDomainRange();
	}

	for (PeakListRow peakRow : selectedPeakList.getRows()) {
	    if (mzRange.contains(peakRow.getAverageMZ())
		    && rtRange.contains(peakRow.getAverageRT())) {
		peakRows.add(peakRow);
	    }
	}

	Collections.sort(peakRows, new PeakListRowSorter(
		SortingProperty.Intensity, SortingDirection.Descending));

	if (threshold > peakRows.size())
	    threshold = peakRows.size();
	for (int i = 0; i < threshold; i++) {
	    newList.addRow(peakRows.elementAt(i));
	}
	return newList;
    }

    /**
     * Returns selected peak list
     */
    PeakList getSelectedPeakList() {
	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	return selectedPeakList;
    }

    /**
     * Reloads peak lists from the project to the selector combo box
     */
    void rebuildPeakListSelector() {

	logger.finest("Rebuilding the peak list selector");

	PeakList selectedPeakList = (PeakList) peakListSelector
		.getSelectedItem();
	PeakList currentPeakLists[] = MZmineCore.getCurrentProject()
		.getPeakLists(dataFile);
	peakListSelector.removeAllItems();
	for (int i = currentPeakLists.length - 1; i >= 0; i--) {
	    peakListSelector.addItem(currentPeakLists[i]);
	}
	if (selectedPeakList != null)
	    peakListSelector.setSelectedItem(selectedPeakList);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

	Object src = e.getSource();

	if (src == thresholdCombo) {

	    PeakThresholdMode mode = (PeakThresholdMode) this.thresholdCombo
		    .getSelectedItem();

	    switch (mode) {
	    case ABOVE_INTENSITY_PEAKS:
		peakTextField.setText(String.valueOf(thresholdSettings
			.getIntensityThreshold()));
		peakTextField.setEnabled(true);
		break;
	    case ALL_PEAKS:
		peakTextField.setEnabled(false);
		break;
	    case TOP_PEAKS:
	    case TOP_PEAKS_AREA:
		peakTextField.setText(String.valueOf(thresholdSettings
			.getTopPeaksThreshold()));
		peakTextField.setEnabled(true);
		break;
	    }

	    thresholdSettings.setMode(mode);

	}

	if (src == peakTextField) {
	    PeakThresholdMode mode = (PeakThresholdMode) this.thresholdCombo
		    .getSelectedItem();
	    String value = peakTextField.getText();
	    switch (mode) {
	    case ABOVE_INTENSITY_PEAKS:
		double topInt = Double.parseDouble(value);
		thresholdSettings.setIntensityThreshold(topInt);
		break;
	    case TOP_PEAKS:
	    case TOP_PEAKS_AREA:
		int topPeaks = Integer.parseInt(value);
		thresholdSettings.setTopPeaksThreshold(topPeaks);
		break;
	    }
	}

	PeakList selectedPeakList = getPeaksInThreshold();
	if (selectedPeakList != null)
	    masterFrame.getPlot().loadPeakList(selectedPeakList);

    }

    @Override
    public void treeNodesChanged(TreeModelEvent event) {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
		.getTreePath().getLastPathComponent();
	if (node.getUserObject() instanceof PeakList)
	    rebuildPeakListSelector();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent event) {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
		.getTreePath().getLastPathComponent();
	if (node.getUserObject() instanceof PeakList)
	    rebuildPeakListSelector();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent event) {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
		.getTreePath().getLastPathComponent();
	if (node.getUserObject() instanceof PeakList)
	    rebuildPeakListSelector();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent event) {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) event
		.getTreePath().getLastPathComponent();
	if (node.getUserObject() instanceof PeakList)
	    rebuildPeakListSelector();
    }

}
