package sg.edu.nus.iss.vmcs.maintenance;

/*
 * Copyright 2003 ISS.
 * The contents contained in this document may not be reproduced in any
 * form or by any means, without the written permission of ISS, other
 * than for the purpose for which it has been supplied.
 *
 */

import java.awt.Frame;
import java.util.Observable;
import java.util.Observer;

import sg.edu.nus.iss.vmcs.customer.controller.TransactionController;
import sg.edu.nus.iss.vmcs.machinery.MachineryController;
import sg.edu.nus.iss.vmcs.store.CashStoreItem;
import sg.edu.nus.iss.vmcs.store.DrinksBrand;
import sg.edu.nus.iss.vmcs.store.DrinksStoreItem;
import sg.edu.nus.iss.vmcs.store.Store;
import sg.edu.nus.iss.vmcs.store.StoreController;
import sg.edu.nus.iss.vmcs.store.StoreItem;
import sg.edu.nus.iss.vmcs.system.MainController;
import sg.edu.nus.iss.vmcs.system.SimulatorControlPanel;
import sg.edu.nus.iss.vmcs.util.MessageDialog;
import sg.edu.nus.iss.vmcs.util.VMCSException;

/**
 *
 *
 * @version 3.0 5/07/2003
 * @author Olivo Miotto, Pang Ping Li
 */

public class MaintenanceController implements Observer {

	private MainController mCtrl;
	private MaintenancePanel mpanel;
	private AccessManager am;

	public MaintenanceController(MainController mctrl) {
		mCtrl = mctrl;
		am = new AccessManager(this);
		
		StoreController storeController = mCtrl.getStoreController();
		storeController.getStore(Store.CASH).addObserver(this);
		storeController.getStore(Store.DRINK).addObserver(this);
	}

	public MainController getMainController() {
		return mCtrl;
	}

	/**
	 * setup the maintenance panel and display it.
	 */
	public void displayMaintenancePanel() {
		SimulatorControlPanel scp = mCtrl.getSimulatorControlPanel();
		if (mpanel == null)
			mpanel = new MaintenancePanel((Frame) scp, this);
		mpanel.display();
		mpanel.setActive(MaintenancePanel.DIALOG, true);
		// setActive of password, invalid and valid display.
	}

	public AccessManager getAccessManager() {
		return am;
	}

	public void loginMaintainer(boolean st) {
		mpanel.displayPasswordState(st);
		mpanel.clearPassword();
		if (st == true) {
			// login successful
			mpanel.setActive(MaintenancePanel.WORKING, true);
			mpanel.setActive(MaintenancePanel.PSWD, false);
			MachineryController machctrl = mCtrl.getMachineryController();
			machctrl.setDoorState(false);
			
			// transition to suspend transaction state
			TransactionController tCtrl = mCtrl.getTransactionController();
			//tCtrl.getCoinInputBox().setActive(false);
			//tCtrl.getDrinkSelectionBox().setActive(false);
			tCtrl.setTransactionState(tCtrl.getSuspendTxnState());
		}
	}

	// invoked in CoinDisplayListener
	public void displayCoin(int idx) {
		StoreController sctrl = mCtrl.getStoreController();
		CashStoreItem item;
		try {
			item = (CashStoreItem) sctrl.getStoreItem(Store.CASH, idx);
			if (mpanel != null)
				mpanel.getCoinDisplay().displayQty(idx, item.getQuantity());
		} catch (VMCSException e) {
			System.out.println("MaintenanceController.displayCoin:" + e);
		}

	}

	// invoked in DrinkDisplayListener;
	public void displayDrinks(int idx) {
		StoreController sctrl = mCtrl.getStoreController();
		DrinksStoreItem item;
		try {
			item = (DrinksStoreItem) sctrl.getStoreItem(Store.DRINK, idx);
			DrinksBrand db = (DrinksBrand) item.getContent();
			if (mpanel != null) {
				mpanel.getDrinksDisplay().displayQty(idx, item.getQuantity());
				mpanel.displayPrice(db.getPrice());
			}
		} catch (VMCSException e) {
			System.out.println("MaintenanceController.displayDrink:" + e);
		}

	}

	// invoked by PriceDisplayListener
	public void setPrice(int pr) {
		StoreController sctrl = mCtrl.getStoreController();
		int curIdx = mpanel.getCurIdx();
		sctrl.setPrice(curIdx, pr);
		mpanel.getDrinksDisplay().getPriceDisplay().setValue(pr + "C");
	}

	// TotalCashButtonListener
	public void getTotalCash() {
		StoreController sctrl = mCtrl.getStoreController();
		int tc = sctrl.getTotalCash();
		mpanel.displayTotalCash(tc);

	}

	// TransferCashButtonListener
	// get all the cash from store and set store cash 0;
	public void transferAll() {
		StoreController sctrl = mCtrl.getStoreController();
		MachineryController machctrl = mCtrl.getMachineryController();

		int cc; // coin quantity;

		try {

			cc = sctrl.transferAll();
			mpanel.displayCoins(cc);
			machctrl.displayCoinStock();
			// the cash qty current is displayed in the Maintenance panel needs to be update to be 0;
			// not required.
			mpanel.updateCurrentQtyDisplay(Store.CASH, 0);
		} catch (VMCSException e) {
			System.out.println("MaintenanceController.transferAll:" + e);
		}
	}

	// StoreViewerListener
	public void changeStoreQty(char type, int idx, int qty) {
		//StoreController sctrl = mCtrl.getStoreController();

		try {
			mpanel.updateQtyDisplay(type, idx, qty);
			mpanel.initCollectCash();
			mpanel.initTotalCash();
		} catch (VMCSException e) {
			System.out.println("MaintenanceController.changeStoreQty:" + e);
		}
	}

	// exit button listener;
	public void logoutMaintainer() {

		MachineryController machctrl = mCtrl.getMachineryController();

		boolean ds = machctrl.isDoorClosed();

		if (ds == false) {
			MessageDialog msg =
				new MessageDialog(
					mpanel,
					"Please Lock the Door before You Leave");
			msg.setLocation(500, 500);
			return;
		}
		
		// transition to drink selection state after maintainer logout (problem fixed)
		TransactionController tCtrl = mCtrl.getTransactionController();
		tCtrl.getCoinInputBox().setActive(false);
		tCtrl.getCoinInputBox().getTotalDisplay().setValue("0C");
		tCtrl.getDrinkSelectionBox().setActive(true);
		tCtrl.getRefundBox().setValue("0C");
		tCtrl.setTransactionState(tCtrl.getSelectDrinkState());
		
		mpanel.setActive(MaintenancePanel.DIALOG, true);

	}
	
	public void closeMaintenancePanel() {
		mpanel.closeDown();
		SimulatorControlPanel scp = mCtrl.getSimulatorControlPanel();
		scp.setActive(SimulatorControlPanel.ACT_MAINTAINER, true);
	}

	public void closeDown() {
		if (mpanel != null)
			mpanel.dispose();
	}

	@Override
	public void update(Observable o, Object arg) {
		System.out.println("Updating maintenance panel...");
		
		if (o instanceof CashStoreItem) {
			StoreItem[] items = mCtrl.getStoreController().getStoreItems(Store.CASH);
			for (int i = 0; i < items.length; i++)
				if (items[i] == o)
					displayCoin(i);
		} else {
			StoreItem[] items = mCtrl.getStoreController().getStoreItems(Store.DRINK);
			for (int i = 0; i < items.length; i++)
				if (items[i] == o)
					displayDrinks(i);
		}
			
	}

}