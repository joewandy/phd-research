/* Copyright (C) 2008, Groningen Bioinformatics Centre (http://gbic.biol.rug.nl/)
 * This file is part of mzmatch.
 * 
 * PeakML is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * mzmatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with PeakML; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */



package peakmlviewer.dialog;


// java

// eclipse
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

// peakml

// peakmlviewer
import peakmlviewer.*;
import peakmlviewer.action.*;





/**
 * 
 */
public class SortIPeakDialog extends ActionDialog implements Listener
{
	// constructor(s)
	public SortIPeakDialog(MainWnd mainwnd, Shell parent)
	{
		super(mainwnd, parent, "Sort");
	}
	
	@Override
	public void init()
	{
		super.init();
		
		// initialize the action to null
		action = null;
		
		// create the layout
		shell.setLayout(new GridLayout(2, false));
		
		// create the widgets for the sorting algorithm
		lbl_sort = new Label(shell, SWT.None);
		lbl_sort.setText("sort: ");
		
		cmb_sort = new Combo(shell, SWT.None);
		for (int i=1; i<SortIPeak.SORT_SIZE; ++i)
			cmb_sort.add(SortIPeak.sort_names[i]);
		
		// create the widgets for the dialog
		btn_ok = new Button(shell, SWT.Selection);
		btn_ok.setText("Ok");
		btn_ok.addListener(SWT.Selection, this);
		
		btn_cancel = new Button(shell, SWT.Selection);
		btn_cancel.setText("Cancel");
		btn_cancel.addListener(SWT.Selection, this);
		
		//
		shell.pack();
	}
	
	
	// Listeners overrides
	public void handleEvent(Event event)
	{
		if (event.widget == btn_ok)
		{
			action = null;
			
			String sort = cmb_sort.getText();
			for (int i=1; i<SortIPeak.SORT_SIZE; ++i)
				if (sort.equals(SortIPeak.sort_names[i]))
				{
					action = new SortIPeak(i);
					break;
				}
			shell.dispose();
		}
		else if (event.widget == btn_cancel)
		{
			shell.dispose();
		}
	}
	
	
	// ActionDialog overrides
	@Override
	protected Action getAction()
	{
		return action;
	}

	@Override
	public String getName()
	{
		return "Sort";
	}
	
	
	// data
	protected SortIPeak action;
	
	protected Label lbl_sort;
	protected Combo cmb_sort;
	
	protected Button btn_ok;
	protected Button btn_cancel;
}
