package edu.ucar.nidas.apps.cockpit.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.Alignment;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QFrame;
import com.trolltech.qt.gui.QGroupBox;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QRadioButton;
import com.trolltech.qt.gui.QTextEdit;
import com.trolltech.qt.gui.QVBoxLayout;

import edu.ucar.nidas.core.UdpConnection;
import edu.ucar.nidas.core.UdpConnInfo;

/**
 * This class provides a UI interface for users to 
 * select an address and port for a data connection.
 */
public class ConnectionDialog extends QDialog
{
    /**
     * User preferred data connection, entered as program arguments
     */
    private String _unicastAddr = "localhost";
    /**
     * Default connection port.
     */
    private int _connPort = 30000;
    /**
     * Default multicast IP
     */
    private String _multicastAddr = "239.0.0.10";;	

    /**
     * Selected address.
     */
    private String _connAddr = null;

    private UdpConnection _udpConnection = null;

    private int _ttl;
    /**
     * Udp data connection information
     */
    private UdpConnInfo _selectedConnection = null;
    private List<UdpConnInfo> _connections = null;

    //diag-ui
    private QRadioButton _mcserv, _ucserv, _rb;

    private QPushButton _bcancel, _bok;

    private QTextEdit _jtPort, _jtmName, _jtsName;

    private QComboBox _jc, _cbData;

    private QLabel _jlServData, _jlPort;

    private QCheckBox _connDebug;

    private Cockpit _cockpit;

    private boolean _debug = false;

    /**
     * The dialog that provides users with UI interface to perform selection and search of the data connection.
     * 
     * @param cockpit
     * @param modal
     * @param cp
     * @param inputS
     */
    public ConnectionDialog(Cockpit cockpit,
            UdpConnection conn, String addr, int port)
    {
        super(cockpit);
        _cockpit = cockpit;
        _udpConnection = conn;
        _connAddr = addr;
        _unicastAddr = addr;
        _connPort = port;

        setModal(true);

        //create the UI-components
        createUI();
    }

    public boolean getDebug()
    {
        return _debug;
    }

    /**
     * Return the Connection.
     * @return
     */
    public UdpConnInfo getSelectedConnection()
    {
        return _selectedConnection;
    }

    private void toggleUI () {
        _bok.setText(" Ok   ");
        _mcserv.setEnabled(false);
        _ucserv.setEnabled(false);
        _jtPort.setEnabled(false);
        _jtmName.setEnabled(false);
        _jtsName.setEnabled(false);
        _jc.setEnabled(false);
        _jlPort.setEnabled(false);
        _jlServData.setVisible(true);
    }

    /**
     * Get the connection address from UI
     * @return
     */
    private String getAddressInput() {
        if (_ucserv.isChecked()) {
            _unicastAddr = _jtsName.toPlainText().trim();
            _connAddr = _unicastAddr;
        } else if (_mcserv.isChecked()) {
            _multicastAddr = _jtmName.toPlainText().trim();
            _connAddr = _multicastAddr;
        } 
        return _connAddr;
    }

    public String getAddress()
    {
        return _connAddr;
    }

    /**
     *  Get the user preferred port
     * @return
     */
    private int getPortInput()
    {
        _connPort = Integer.valueOf(_jtPort.toPlainText().trim());
        return _connPort;
    }

    public int getPort()
    {
        return _connPort;
    }

    private int getTTLInput()
    {
        _ttl = _jc.currentIndex()+1;
        return _ttl;
    }

    public int getTTL()
    {
        return _ttl;
    }

    /**
     * diag-ui creation
     */
    public void createUI() {

        setWindowTitle("Data connection");
        QVBoxLayout mlayout = new QVBoxLayout();

        //for row1 and row2
        QGroupBox qf = new QGroupBox();
        //qf.
        QVBoxLayout qv = new QVBoxLayout();
        //row1 multicast
        QHBoxLayout hlayout = new QHBoxLayout();
        _mcserv = new QRadioButton("Multicast");
        _mcserv.clicked.connect(this, "multicastServerRadio()");
        hlayout.addWidget(_mcserv);
        _jtmName = new QTextEdit(_multicastAddr);
        _jtmName.setMaximumSize(200, 30);
        _jtmName.adjustSize();
        hlayout.addWidget(_jtmName);
        hlayout.addWidget(new QLabel("TTL"));
        _jc = new QComboBox();
        for (int k = 1; k <= 3; k++) {
            _jc.addItem(String.valueOf(k));
        }
        hlayout.addWidget(_jc);
        hlayout.addStretch();
        Alignment qal = new Alignment();
        qal.set(Qt.AlignmentFlag.AlignLeft);
        qv.addLayout(hlayout);

        //row 2 : radio= server textfield = servername
        hlayout = new QHBoxLayout();
        _ucserv = new QRadioButton("Unicast  "); 
        _ucserv.setChecked(true);
        _ucserv.clicked.connect(this, "unicastServerRadio()");
        hlayout.addWidget(_ucserv);
        _jtsName = new QTextEdit(_unicastAddr);
        _jtsName.setMaximumSize(200, 30);
        _jtsName.adjustSize();
        //_jtsName.setAlignment(Qt.AlignmentFlag.AlignRight);
        hlayout.addWidget(_jtsName);
        hlayout.addWidget(new QLabel());
        hlayout.addStretch();
        qv.addLayout(hlayout);
        qf.setLayout(qv);
        // qf.setTitle("Data Connection");

        mlayout.addWidget(qf);

        //row-3  == port 
        hlayout = new QHBoxLayout();
        _jlPort = new QLabel("         Port#     ");
        hlayout.addWidget(_jlPort);
        _jtPort = new QTextEdit("" + _connPort);
        _jtPort.setMaximumSize(200, 30);
        _jtPort.adjustSize();
        //_jtPort.setAlignment(Qt.AlignmentFlag.AlignRight);
        hlayout.addWidget(_jtPort);
        hlayout.addWidget(new QLabel());
        hlayout.addStretch();
        //hlayout.setAlignment(qal);
        mlayout.addLayout(hlayout);


        //row 4  -servInf selection combobox
        hlayout = new QHBoxLayout();
        hlayout.addWidget(new QLabel());
        mlayout.addLayout(hlayout);

        hlayout = new QHBoxLayout();
        _jlServData = new QLabel("Server Option:");
        _jlServData.setVisible(false);
        hlayout.addWidget(_jlServData);
        _cbData = new QComboBox();
        _cbData.setVisible(false);
        hlayout.addWidget(_cbData);
        mlayout.addItem(hlayout);

        //row-5 text-edit
        _connDebug = new QCheckBox("Log connection debug messages");
        _connDebug.setChecked(false);
        _connDebug.clicked.connect(this, "connDebug()");
        mlayout.addWidget(_connDebug);

        //row-last   --ok-cancel buttons
        hlayout = new QHBoxLayout();
        hlayout.addWidget(new QLabel());
        _bok = new QPushButton("Search", this);
        _bok.clicked.connect(this, "pressOk()");
        hlayout.addWidget(_bok);
        _bcancel = new QPushButton("Cancel", this);
        _bcancel.clicked.connect(this, "pressCancel()");
        hlayout.addWidget(_bcancel);
        hlayout.addWidget(new QLabel());
        mlayout.addItem(hlayout);

        setLayout(mlayout);
        setGeometry(400, 300, 400, 300);
        setVisible(true);
        exec();
    }

    void connDebug()
    {
        _debug = _connDebug.isChecked();
        if (_debug) _cockpit.showLog();
    }

    void multicastServerRadio()
    {
        if (_mcserv.isChecked()) _ucserv.setChecked(false);
        if (!_mcserv.isChecked() && !_ucserv.isChecked()) _mcserv.setChecked(true);
    }

    void unicastServerRadio()
    {
        if (_ucserv.isChecked()) _mcserv.setChecked(false);
        if (!_mcserv.isChecked() && !_ucserv.isChecked()) _ucserv.setChecked(true);
    }

    /**
     * This is a toggled button to search and finalize the data connection.
     * if it shows "Search", it looks up the potential servers
     * else, it join the multicast group if needed, and close the dialog
     */
    void pressOk()
    {
        setCursor(new QCursor(Qt.CursorShape.WaitCursor));
        if (_bok.text().trim().equals("Search")) {

            search();
            if (_connections.size() == 1 ) {  //ONLY ONE- find it
                _selectedConnection = _connections.get(0);
                close();
            }
        } else {
            _selectedConnection = _connections.get(_cbData.currentIndex());
            close();
        }
        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
    }

    void pressCancel()
    {
        close();
    }

    /**
     * This method searches potential data connections, and let users select
     * if there are multiple choices
     */
    private void search()
    {

        String addr = getAddressInput();
        int port = getPortInput();
        int ttl = getTTLInput();

        try {
            _connections = _udpConnection.search(addr, port, ttl,
                    _cockpit.getLog(), _debug);
        }
        catch (IOException e) {
            _cockpit.getLog().error("search: " + e.toString());
            return;
        }

        if (_connections == null || _connections.size()==0 ) {
            _cockpit.getLog().error("search: No server found");
            _cockpit.status("search: No server found");
            return;
        }

        if (_connections.size()==1) {
            _selectedConnection = _connections.get(0);
            return;
        }

        // display connections
        //
        for (UdpConnInfo conn : _connections) {
            _cbData.addItem(conn.toString());
        }
        _cbData.setVisible(true);
        toggleUI();
    }
}