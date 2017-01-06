package edu.ucar.nidas.apps.cockpit.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QRect;
import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.QDir;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QToolTip;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMenuBar;
import com.trolltech.qt.gui.QPalette;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QFileDialog;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QMessageBox.StandardButton;
import com.trolltech.qt.gui.QDesktopWidget;

import edu.ucar.nidas.apps.cockpit.model.CockpitConfig;
import edu.ucar.nidas.apps.cockpit.model.MinMaxer;
import edu.ucar.nidas.core.UdpConnection;
import edu.ucar.nidas.core.UdpConnInfo;
import edu.ucar.nidas.core.UdpDataReaderThread;
import edu.ucar.nidas.model.Site;
import edu.ucar.nidas.model.Dsm;
import edu.ucar.nidas.model.Sample;
import edu.ucar.nidas.model.Var;
import edu.ucar.nidas.model.DataProcessor;
import edu.ucar.nidas.model.DataSource;
import edu.ucar.nidas.model.DataClient;
import edu.ucar.nidas.model.QProxyDataClient;
import edu.ucar.nidas.model.NotifyClient;
import edu.ucar.nidas.ui.StatusBar;
import edu.ucar.nidas.ui.LogDisplay;
import edu.ucar.nidas.model.StatusDisplay;
import edu.ucar.nidas.model.Log;
import edu.ucar.nidas.util.DOMUtils;

import org.w3c.dom.Document;
import javax.xml.parsers.ParserConfigurationException;

/**
 * this is the main program of cockpit.
 * It contains a mainWindow ui instance,
 * an array of gauges.
 * and a timer
 *
 */

public class Cockpit extends QMainWindow {

    /**
     * This class is the cockpit main class. 
     * It controls the UI, 
     *    data connection, 
     *    and cent-tab-wdiget
     */

    /**
     * global defaults
     */
    static QImage gnodataImg = null;  //nodataImg-rip

    public static QColor defTraceColor = new QColor(255,255,0);
    public static QColor defHistoryColor = new QColor(170,170,255);

    public static ArrayList<QColor> defBGColors = new ArrayList<QColor>();

    public static final int gwdef = 120;

    public static final int ghdef = 80;

    private StatusBar _statusbar;

    private QAction _connectAction;

    private QAction _freezeAllPlotAction;

    private QAction _unfreezeAllPlotAction;

    private QAction _freezeAllGridAction;

    private QAction _unfreezeAllGridAction;

    public QAction autoCycleTabsAction;

    private LogDialog _logDialog;

    private Log _log;

    /**
     * Config file, specifed in runstring, or in dialog.
     */
    private String _configFileName = null;

    private String defaultConfigName = QDir.current().filePath("cockpit.xml");
    
    /**
     * UDP data server address.
     */
    private String _connAddress = "localhost"; 
    private int _connPort = 30005;

    private ConnectionDialog _connDialog = null;

    /**
     * Parameters associated with the current data connection.
     */
    private UdpConnInfo _udpConnInfo = null;

    /**
     * UdpConnection
     */
    private UdpConnection _udpConnection = null;

    /*
     * Variables by their name.
     */
    private HashMap<String, Var> _varsByName = new HashMap<String, Var>();

    /**
     * list of data clients by variable and by
     * index of data value in the variable's data array.
     */
    private HashMap<String, DataProcessor> _dataProcessorByVarName =
        new HashMap<String, DataProcessor>();

    /**
     * cent widget for all tabs
     */
    private CentTabWidget _centWidget = null;

    private UdpDataReaderThread _dataThread = null;

    private final Object _dataThreadLock = new Object();

    private Reconnector _reconnector = null;

    /**
     * Data reduction period, in milliseconds. Points will
     * be plotted at this interval, typically 1000 msec for
     * 1 second data points.
     */
    private int _statisticsPeriod = 1000;

    /**
     * Cockpit constructor.
     */
    public Cockpit(String[] args)
    {
        if (args != null && args.length > 0) parseArg(args);

        // Just in case we have more than one cockpits :-)
        if (defBGColors.isEmpty()) {
            defBGColors.add(new QColor(0,85,127));
            defBGColors.add(new QColor(82,85,79));
            defBGColors.add(new QColor(85,85,127));
            defBGColors.add(new QColor(65,94,84));
        }

        connectSlotsByName();
        setHidden(true);

        _statusbar = new StatusBar(this);
        _logDialog = new LogDialog(this);
        _log = new LogDisplay(_logDialog.getTextWidget());
        setGeometry(PageGeometry.x,  PageGeometry.y,
                PageGeometry.w, PageGeometry.h);  
        _centWidget = new CentTabWidget(this);
        setCentralWidget(_centWidget); 
        createUIs();
        openImage();

	_udpConnection = new UdpConnection();

	_reconnector = this.new Reconnector();

        connect(true);

        String cname = getConfigFileName();
        if (cname != null) {
            try {
                Document document = DOMUtils.parseXML(
                        new FileInputStream(cname), true);
                CockpitConfig config = new CockpitConfig(document);
                _centWidget.apply(config);
            }
            catch(Exception e) {
                status(e.getMessage());
                logError(e.getMessage());
            }
        }
    }

    public Log getLog()
    {
        return _log;
    }

    public Var getVar(String name) 
    {
        return _varsByName.get(name);
    }

    public Set<String> getVarNames() 
    {
        return _varsByName.keySet();
    }

    public Collection<Var> getVars() 
    {
        return _varsByName.values();
    }

    public DataSource getDataSource(Var var) 
    {
        return _dataProcessorByVarName.get(var.getNameWithStn());
    }

    @Override
    public void closeEvent(QCloseEvent event)
    {
        // System.out.println("close event");
        shutdown();
        event.accept();
    }

    public void setConnAddress(String val)
    {
        _connAddress = val;
    }

    public String getConnAddress() { return _connAddress; }

    public void setConnPort(int val) {_connPort = val;}

    public int getConnPort() { return _connPort; }
    
    public CentTabWidget getCentWidget() { return _centWidget; }

    public StatusDisplay getStatusDisplay() { return _statusbar; }

    public void status(String msg, int ms)
    {
        _statusbar.show(msg,ms);
    }
      
    public void status(String msg)
    {
        _statusbar.show(msg);
    }
      
    public void logError(String msg)
    {
        _log.error(msg);
    }
      
    public void logInfo(String msg)
    {
        _log.info(msg);
    }
      
    public void logDebug(String msg)
    {
        _log.debug(msg);
    }
      
    private void createUIs()
    {
        // status
        status("Start ...", 10000);

        //create menu items
        createMenuItems(); 
        show();
    }

    /**
     * Tooltips on menu items are not supported directly in Qt 4.
     * This was cobbled together from web postings.
     */
    public static class QMenuActionWithToolTip extends QAction
    {
        private QMenu _menu;

        public QMenuActionWithToolTip(String text, String tooltip, QMenu parent)
        {
            super(text, parent);
            _menu = parent;
            setToolTip(tooltip);
            hovered.connect(this, "showToolTip()");
        }
        public void showToolTip()
        {
            // System.err.println("showToolTip: " + toolTip());
            // int val = (Integer)data();
            // System.err.println("showToolTip, data=: " + val);
            QRect rect = _menu.actionGeometry(this);
            // System.err.println(rect.toString());
            int pos_x = _menu.pos().x() + rect.x();
            int pos_y = _menu.pos().y() + rect.y() - rect.height() / 2;
            QPoint pos = new QPoint(pos_x, pos_y);
            /*
            System.err.printf("pos= %d, %d\n",pos.x(), pos.y());
            QPoint gpos = _menu.mapToGlobal(pos);
            System.err.printf("gpos= %d, %d\n",gpos.x(), gpos.y());
            */
            QRect screen = QApplication.desktop().availableGeometry(_menu);
            /*
            System.err.printf("screen= %d x %d\n",screen.width(), screen.height());
            */
            if (pos.x() > screen.width() / 2) {
                /* If LHS of menu is more than half way across screen, put
                 * tooltip below menu item. */
                pos.setY(pos_y + rect.height());
            }
            else {
                /* If LHS of menu is less than half way across screen, put
                 * tooltip to right of menu item. */
                pos.setX(pos_x + rect.width());
            }
            QToolTip.showText(pos, toolTip());
        }
    }

    private void createMenuItems()
    {
        //menuBar
        QMenuBar menuBar = new QMenuBar(this);
        QPalette pl = new QPalette(Qt.GlobalColor.lightGray);
        menuBar.setPalette(pl);

        menuBar.setObjectName("menuBar");
        menuBar.setGeometry(new QRect(0, 0, 1290, 25));
        setMenuBar(menuBar);

        //file and items
        QMenu file = menuBar.addMenu("&File");
        menuBar.setFont(new QFont("Ariel", 12));

        QAction action = new QMenuActionWithToolTip("&Connect",
                "Connect to data server", file);
        action.triggered.connect(this, "connect()");
        file.addAction(action);
        _connectAction = action;

        action = new QMenuActionWithToolTip("&Show Log",
                "Show log message window", file);
        action.triggered.connect(this, "showLog()");
        file.addAction(action);

        file.addAction("&Save Config", this, "saveConfig()");
        file.addAction("&Open Config", this, "openConfig()");
        file.addAction("&Exit", this, "close()");

        QMenu add = menuBar.addMenu("Add");
        add.addAction("NewPageByVar", this, "addPageByVar()");
        add.addAction("NewPageByHt", this, "addPageByHt()");
        //add.addAction("SortPageByVariable", _centWidget, "addVariablePage()");
        //add.addAction("SortPageByHeight", _centWidget, "addHeightPage()");
        add.setEnabled(false);

        // Top menu of global options
        QMenu topMenu = menuBar.addMenu("&Global Options");

        action = new QMenuActionWithToolTip("&Clear All History",
                "Clear history shadows on all plots", topMenu);
        action.triggered.connect(_centWidget, "globalClearHistory()");
        topMenu.addAction(action);

        action = new QMenuActionWithToolTip("Freeze Plot Sizes",
                "Fix plot sizes on all plot pages", topMenu);
        action.triggered.connect(_centWidget, "freezePlotSizes()");
        topMenu.addAction(action);
        _freezeAllPlotAction = action;

        action = new QMenuActionWithToolTip("Unfreeze Plot Sizes", 
            "Allow plot sizes to vary, losing history shadow",
            topMenu);
        action.triggered.connect(_centWidget, "unfreezePlotSizes()");
        topMenu.addAction(action);
        _unfreezeAllPlotAction = action;

        action = new QMenuActionWithToolTip("Freeze Grids", 
            "Fix grid layout of plots",
            topMenu);
        action.triggered.connect(_centWidget, "freezeGrids()");
        topMenu.addAction(action);
        _freezeAllGridAction = action;

        action = new QMenuActionWithToolTip("Unfreeze Grids", 
            "Allow grid layout to change",
            topMenu);
        action.triggered.connect(_centWidget, "unfreezeGrids()");
        topMenu.addAction(action);
        _unfreezeAllGridAction = action;

        action = new QMenuActionWithToolTip("Auto Cycle &Tabs", 
            "Cycle through plot pages",
            topMenu);
        action.triggered.connect(_centWidget, "autoCycleTabs()");
        topMenu.addAction(action);
        autoCycleTabsAction = action;

        action = new QMenuActionWithToolTip("Change Plot Time &Width", 
            "Change time scale on all plots, losing history",
            topMenu);
        action.triggered.connect(_centWidget, "changeAllPlotTimeWidth()");
        topMenu.addAction(action);

        action = new QMenuActionWithToolTip("Set Data &Timeout", 
            "Set data timeout value for all plots, in seconds",
            topMenu);
        action.triggered.connect(_centWidget, "setDataTimeout()");
        topMenu.addAction(action);

        topMenu.addAction("AutoScalePlots", _centWidget,
                "globalAutoScalePlots()");

        // Top menu of page options
        topMenu = menuBar.addMenu("&Page Options");
        action = new QMenuActionWithToolTip("&Clear History",
                "Clear history shadow on plots in current page", topMenu);
        action.triggered.connect(_centWidget, "pageClearHistory()");
        topMenu.addAction(action);

        QMenu subMenu = topMenu.addMenu("Color");
        action = new QMenuActionWithToolTip("Change &Trace Color",
                "Change trace color on plots in current page", subMenu);
        action.triggered.connect(_centWidget, "pageTraceColor()");
        subMenu.addAction(action);

        action = new QMenuActionWithToolTip("Change &History Color",
                "Change color of history shadows in current page", subMenu);
        action.triggered.connect(_centWidget, "pageHistoryColor()");
        subMenu.addAction(action);

        action = new QMenuActionWithToolTip("Change &Background Color",
                "Change background color in current page, losing history", subMenu);
        action.triggered.connect(_centWidget, "pageBackgroundColor()");
        subMenu.addAction(action);

        /*
        QMenu srt = globalMenu.addMenu("SortBy");
        // srt.addAction("Variable", _centWidget, "gsortVariable()");
        // srt.addAction("Height", _centWidget, "gsortHeight()");
        */

        action = new QMenuActionWithToolTip("Change Plot Time &Width", 
            "Change time scale of plots on current page, losing history",
            topMenu);
        action.triggered.connect(_centWidget, "changePagePlotTimeWidth()");
        topMenu.addAction(action);

    }

    public void showToolTip()
    {
        System.err.println("showToolTip");
    }

    public void disableFreezePlotSizeMenu()
    {
        _freezeAllPlotAction.setEnabled(false);
    }

    public void enableFreezePlotSizeMenu()
    {
        _freezeAllPlotAction.setEnabled(true);
    }

    public void disableUnfreezePlotSizeMenu()
    {
        _unfreezeAllPlotAction.setEnabled(false);
    }

    public void enableUnfreezePlotSizeMenu()
    {
        _unfreezeAllPlotAction.setEnabled(true);
    }

    public void disableFreezeGridMenu()
    {
        _freezeAllGridAction.setEnabled(false);
    }

    public void enableFreezeGridMenu()
    {
        _freezeAllGridAction.setEnabled(true);
    }

    public void disableUnfreezeGridMenu()
    {
        _unfreezeAllGridAction.setEnabled(false);
    }

    public void enableUnfreezeGridMenu()
    {
        _unfreezeAllGridAction.setEnabled(true);
    }

    public void showLog()
    {
        _logDialog.raise();
        _logDialog.show();
    }

    public void shutdown()
    {
        synchronized(_dataThreadLock) {
            try {
                if (_dataThread != null) {
                    _dataThread.interrupt();
                    _dataThread.join();
                }
            }
            catch (InterruptedException e) {}
        }

        _statusbar.close();
        try {
            _udpConnection.close();
        }
        catch(IOException e) {}
        // System.exit(0);
    }

    public void setConfigFileName(String val)
    {
        _configFileName = val;
    }
        
    public String getConfigFileName()
    {
        return _configFileName;
    }
    
    private void addPageByVar()
    {
        new VarLookup(this);
    }
    
    private void addPageByHt()
    {
        new HtLookup(this);
    }
    
    private void saveConfig()
    {
        String cname = getConfigFileName();
        if (cname == null)
            cname = defaultConfigName;

	cname = QFileDialog.getSaveFileName(this, "Save File", cname);
        if (cname == null || cname.isEmpty()){
            statusBar().showMessage("configuration file is NOT selected.", 10000); //10 sec
        }
        setConfigFileName(cname);

        // read current configuration from display
        CockpitConfig config = new CockpitConfig(_centWidget);
        try {
            Document document = DOMUtils.newDocument();
            config.toDOM(document);
            DOMUtils.writeXML(document, cname);
        }
        catch(Exception e) {
            status(e.getMessage());
            logError(e.getMessage());
        }
    }

    private void openConfig()
    {
        String cname = getConfigFileName();
        if (cname == null)
            cname = defaultConfigName;
	cname = QFileDialog.getOpenFileName(this, "Open File", cname);
        setConfigFileName(cname);
        try {
            Document document = DOMUtils.parseXML(new FileInputStream(cname), true);
            CockpitConfig config = new CockpitConfig(document);
            _centWidget.apply(config);
        }
        catch(Exception e) {
            status(e.getMessage());
            logError(e.getMessage());
        }
    }

    /** 
     * Establish a data connection.
     * @return
     */
    public boolean connect(boolean dialog)
    {

        setCursor(new QCursor(Qt.CursorShape.WaitCursor));

        if (dialog) {
            // create modal dialog to establish connection
            _connDialog = new ConnectionDialog(this,
                _udpConnection, getConnAddress(), getConnPort());

            _udpConnInfo = _connDialog.getSelectedConnection();
            if (_udpConnInfo == null) return false;
        }
        else {
            String addr = _connDialog.getAddress();
            int port = _connDialog.getPort();
            int ttl = _connDialog.getTTL();
            ArrayList<UdpConnInfo> connections = null;
            try {
                connections = _udpConnection.search(addr, port, ttl,
                        getLog(), _connDialog.getDebug());
            }
            catch (IOException e) {
                status(e.getMessage());
                logError(e.getMessage());
                return false;
            }
            UdpConnInfo matchConn = null;
            for (UdpConnInfo conn : connections) {
                if (_udpConnInfo.getServer().equals(conn.getServer()) &&
                    _udpConnInfo.getProjectName().equals(conn.getProjectName())) {
                    matchConn = conn;
                    break;
                }
            }
            if (matchConn == null) {
                String msg = "Data connection for server " +
                    _udpConnInfo.getServer() +
                    " and project " +
                    _udpConnInfo.getProjectName() + " not found";
                status(msg);
                logError(msg);
                return false;
            }
            _udpConnInfo = matchConn;
        }

        String projectname = _udpConnInfo.getProjectName();

        setWindowTitle(projectname + " COCKPIT");

        try {
            _udpConnection.connect(_udpConnInfo, _log, _connDialog.getDebug());
        }
        catch (IOException e) {
            status(e.getMessage());
            logError(e.getMessage());
            return false;
        }
        ArrayList<Site> sites = null;
        try {
            Document doc = _udpConnection.readDOM();
            sites = Site.parse(doc);
        }
        catch (Exception e) {
            status("Parsing XML: " + e.getMessage());
            logError("Parsing XML: " + e.getMessage());
            return false;
        }

        synchronized(_dataThreadLock) {
            if (_dataThread != null) _dataThread.interrupt();
            _dataThread = new UdpDataReaderThread(
                    _udpConnection.getUdpSocket(), _statusbar, _log, _reconnector);
        }
        _dataProcessorByVarName.clear();
        _varsByName.clear();

        for (Site site : sites) {
            ArrayList<Dsm> dsms = site.getDsms();

            for (Dsm dsm : dsms) {
                ArrayList<Sample> samps = dsm.getSamples();

                for (Sample samp : samps) {
                    ArrayList<Var> vars = samp.getVars();
                    
                    for (Var var : vars) {
                        if (var.getLength() != 1) continue;
                        _varsByName.put(var.getNameWithStn(), var);
                        DataProcessor dc = _dataProcessorByVarName.get(var.getNameWithStn());
                        if (dc == null) {
                            dc = new MinMaxer(_statisticsPeriod);
                            _dataProcessorByVarName.put(var.getNameWithStn(),dc);
                        }
                        _dataThread.addClient(samp,var,dc);

                    }
                }
            }
        }

        _centWidget.setName(projectname);
        _centWidget.addGaugePages(sites);

        Collection<GaugePage> pages = _centWidget.getGaugePages();

        for (GaugePage page: pages) {
            List<Gauge> gauges = page.getGauges();
            for (Gauge gauge: gauges) {
                // System.out.println("gauge name=" + gauge.getName());
                Var var = _varsByName.get(gauge.getName());
                DataProcessor dc = _dataProcessorByVarName.get(var.getNameWithStn());
                DataClient proxy = QProxyDataClient.getProxy(gauge);
                dc.addClient(proxy);
            }
        }

        status("   No sensor data yet...", 10000);
        
        _connectAction.setEnabled(false);

        _dataThread.start();

        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));

        return true;
    }

    public boolean connect()
    {
        return connect(true);
    }

    private  void openImage() {
        if (gnodataImg !=null) return;
        String nodatapath = "classpath:image/nodata.png";
        gnodataImg = new QImage(nodatapath);
        gnodataImg.setColor(1,new QColor(Qt.GlobalColor.red).rgb());
    }

    static class PageGeometry {
        static public int x = 350, y = 250, w = 1000, h = 700;
    }
    
    /**
     * parse the parameters received from user's input
     * [-s server:port] [-c config.xml] 
     * Silently ignores unrecognized arguments.
     * @param args  -s serv:port -c config.xml
     * @return      void
     */
    private void parseArg(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-s") && i + 1 < args.length){
                String opt = args[i+1].trim();
                String[] ss = opt.split(":");
                if (ss.length >= 1)
                    _connAddress = ss[0];
                if (ss.length > 1)
                    _connPort = Integer.valueOf(ss[1]);
            }
            else if (args[i].equals("-c") && i + 1 < args.length ) {
                String cname = args[++i];
                if (cname.length() > 0) {
                    if (QDir.isRelativePath(cname))
                        cname = QDir.current().filePath(cname);
                    setConfigFileName(cname);
                }
            }
            else if (args[i].equals("-arg") || args[i].equals("-open") &&
                //args from jnlp
                i + 1 < args.length) {
                String op = args[++i];
                String[] trs = op.split(" ");
                if (trs.length != 2) {
                    status("Invalid argument: " + op);
                    logError("Invalid argument: " + op);
                }
                else parseArg(trs);
            }
        }
    }

    /**
     * Construct a cockpit mainframe, and only one mainframe
     * options: -s to pass address:port example:
     *  -s   porter.eol.ucar.edu:30000
     */
    public static void main(String[] args)
    {
        QApplication.initialize(args);
        //setNativeLookAndFeel();
        Cockpit cockpit = new Cockpit(args);
        QApplication.execStatic();
        QApplication.shutdown();
    }

    private class Reconnector implements NotifyClient, Runnable
    {
        @Override
	public void wake()
	{
            QApplication.invokeLater(this);
	}

	/**
	 * Do the reconnection.
	 */
        @Override
	public void run()
	{
            status("Attempting reconnect...");
            while (!connect(false)) {
                status("Attempting reconnect...");
                QApplication.processEvents();
            }
            status("Reconnected",5*1000);
	}
    }

    public int confirmMessageBox(String s, String title) {
        int ret =
            QMessageBox.warning(this, title, s,
                    StandardButton.Ok, StandardButton.Abort);
        return ret;
    }
}
