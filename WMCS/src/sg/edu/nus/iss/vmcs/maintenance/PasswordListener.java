package sg.edu.nus.iss.vmcs.maintenance;

/*
 * Copyright 2003 ISS.
 * The contents contained in this document may not be reproduced in any
 * form or by any means, without the written permission of ISS, other
 * than for the purpose for which it has been supplied.
 *
 */

import java.awt.event.*;
import java.awt.*;

/**
 *
 *
 * @version 3.0 5/07/2003
 * @author Olivo Miotto, Pang Ping Li
 */

public class PasswordListener implements ActionListener {

	AccessManager actrl;

	public PasswordListener(AccessManager ac) {
		actrl = ac;
	}
	public void actionPerformed(ActionEvent e) {
		TextField text;

		String pswd;

		text = (TextField) e.getSource();
		pswd = text.getText();
		actrl.processPassword(pswd);
	}
}